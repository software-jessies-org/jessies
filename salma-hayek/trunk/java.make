# You may use:
#   make
#   make clean
#   make dist
#   make bindist

# You can set:
#   JAVA_COMPILER to "jikes", "javac", or a binary of your choice.

# Your calling Makefile:
#   must define PROJECT_NAME
#   may append to BINDIST_FILES
#   may append to SUBDIRS
#   must include ../salma-hayek/java.make

# ----------------------------------------------------------------------------
# Ensure we're running a suitable version of make(1).
# ----------------------------------------------------------------------------

REQUIRED_MAKE_VERSION = 3.80
REAL_MAKE_VERSION = $(firstword $(MAKE_VERSION))
EARLIER_MAKE_VERSION = $(firstword $(sort $(REAL_MAKE_VERSION) $(REQUIRED_MAKE_VERSION)))
ifneq "$(REQUIRED_MAKE_VERSION)" "$(EARLIER_MAKE_VERSION)"
    $(error This makefile requires at least version $(REQUIRED_MAKE_VERSION) of GNU make, but you're using $(REAL_MAKE_VERSION))
endif

# ----------------------------------------------------------------------------
# Define useful stuff not provided by GNU make.
# ----------------------------------------------------------------------------

pathsearch = $(firstword $(wildcard $(addsuffix /$(1),$(subst :, ,$(PATH)))))
makepath = $(subst $(SPACE),:,$(strip $(1)))

SPACE := $(subst :, ,:)

# ----------------------------------------------------------------------------

SUBDIRS += $(wildcard native/all/*) $(wildcard native/Darwin/*)

SALMA_HAYEK=$(dir $(word $(words $(MAKEFILE_LIST)),$(MAKEFILE_LIST)))

# By default, distributions end up under http://www.jessies.org/~enh/
DIST_SCP_USER_AND_HOST=enh@jessies.org
DIST_SCP_DIRECTORY="~/public_html/software/$(PROJECT_NAME)/nightly-builds"

SOURCE_FILES=$(shell find `pwd`/src -type f -name "*.java")
TAR_FILE_OF_THE_DAY=`date +$(PROJECT_NAME)-%Y-%m-%d.tar`

REVISION_CONTROL_SYSTEM := $(if $(wildcard .svn),svn,cvs)

define GENERATE_CHANGE_LOG.svn
  svn log > ChangeLog
endef

define GENERATE_CHANGE_LOG.cvs
  $(if $(shell which cvs2cl),cvs2cl,cvs2cl.pl) --hide-filenames
endef

JAR=$(if $(JAVA_HOME),$(JAVA_HOME)/bin/)jar
CREATE_OR_UPDATE_JAR=cd $(2) && $(JAR) $(1)f $(CURDIR)/$@ -C classes $(notdir $(wildcard $(2)/classes/*))

GENERATED_FILES += classes
GENERATED_FILES += $(PROJECT_NAME).jar
GENERATED_FILES += $(PROJECT_NAME)-bindist.tgz

grep-v = $(filter-out @@%,$(filter-out %@@,$(subst $(1),@@ @@,$(2))))
DIRECTORY_NAME := $(notdir $(CURDIR))

BINDIST_FILES += README COPYING $(PROJECT_NAME).jar
FILTERED_BINDIST_FILES = $(shell find $(BINDIST_FILES) -type f | grep -v /CVS/)

define GENERATE_FILE_LIST.bk
  bk sfiles -g
endef
define GENERATE_FILE_LIST.cvs
  cvs ls -R -P -e | perl -ne 'm/(.*):$$/ && ($$dir = "$$1"); m@^/([^/]*)/@ && print ("$$dir/$$1\n")'
endef
define GENERATE_FILE_LIST.svn
  svn list -R
endef

FILE_LIST_WITH_DIRECTORIES = $(shell $(GENERATE_FILE_LIST.$(REVISION_CONTROL_SYSTEM)))
FILE_LIST_WITH_DIRECTORIES += classes
FILE_LIST_WITH_DIRECTORIES += ChangeLog # The ChangeLog should never be checked in, but should be in distributions.
FILE_LIST = $(subst /./,/,$(addprefix $(PROJECT_NAME)/,$(filter-out $(dir $(FILE_LIST_WITH_DIRECTORIES)),$(FILE_LIST_WITH_DIRECTORIES))))

# ----------------------------------------------------------------------------
# Choose a Java compiler.
# ----------------------------------------------------------------------------

ifneq "$(JAVA_COMPILER)" ""

  # The user asked for a specific compiler, so check that we can find it.
  ifeq "$(wildcard $(JAVA_COMPILER))$(call pathsearch,$(JAVA_COMPILER))" ""
    $(error Unable to find $(JAVA_COMPILER))
  endif

else

  # The user left the choice of compiler up to us.
  # Favor Jikes, but fall back to javac.
  JAVA_COMPILER = jikes
  ifeq "$(wildcard $(JAVA_COMPILER))$(call pathsearch,$(JAVA_COMPILER))" ""
    JAVA_COMPILER = javac
    ifeq "$(wildcard $(JAVA_COMPILER))$(call pathsearch,$(JAVA_COMPILER))" ""
      $(error Unable to find Jikes or javac)
    endif
  endif

endif

# Make the compiler's leafname available for simple javac/jikes tests.
COMPILER_TYPE = $(notdir $(JAVA_COMPILER))

# ----------------------------------------------------------------------------
# Find any necessary JAR files.
# ----------------------------------------------------------------------------

# A list of likely JDK rt.jar locations.
# Traditional Java setup:
KNOWN_RT_JAR_LOCATIONS+=$(JAVA_HOME)/jre/lib/rt.jar
# Apple:
KNOWN_RT_JAR_LOCATIONS+=/System/Library/Frameworks/JavaVM.framework/Classes/classes.jar
# install-everything.sh:
KNOWN_RT_JAR_LOCATIONS+=/usr/local/jdk1.5.0/jre/lib/rt.jar
# Fall back to searching:
KNOWN_RT_JAR_LOCATIONS:=$(KNOWN_RT_JAR_LOCATIONS) $(shell locate /rt.jar)

# Pick a JDK and append the MRJ141Stubs, in case they're there.
RT_JAR=$(firstword $(wildcard $(KNOWN_RT_JAR_LOCATIONS)))

BOOT_CLASS_PATH += $(BOOT_CLASS_PATH.$(COMPILER_TYPE))
CLASS_PATH += $(CLASS_PATH.$(COMPILER_TYPE))

BOOT_CLASS_PATH.jikes += $(RT_JAR)

ifneq ($(wildcard MRJ141Stubs.jar),)
    CLASS_PATH += MRJ141Stubs.jar
endif
CLASS_PATH += $(SALMA_HAYEK)/classes

# ----------------------------------------------------------------------------
# Sort out the flags our chosen compiler needs.
# ----------------------------------------------------------------------------

JAVA_FLAGS += $(JAVA_FLAGS.$(COMPILER_TYPE))
JAVA_FLAGS += $(addprefix -bootclasspath ,$(call makepath,$(BOOT_CLASS_PATH)))
JAVA_FLAGS += $(addprefix -classpath ,$(call makepath,$(CLASS_PATH)))
JAVA_FLAGS += -d classes/
JAVA_FLAGS += -sourcepath src/
JAVA_FLAGS += -deprecation

JAVA_FLAGS.jikes += +D +P +Pall +Pno-serial +Pno-redundant-modifiers
JAVA_FLAGS.javac += -Xlint -Xlint:-serial
JAVA_FLAGS.javac += -Xlint:-unchecked # until Jikes supports generics

# ----------------------------------------------------------------------------
# Variables above this point,
# rules below...
# ----------------------------------------------------------------------------

.PHONY: build
build: $(SOURCE_FILES) build.subdirs
	@echo Recompiling the world... && \
	 make clean && \
	 mkdir -p classes && \
	 $(JAVA_COMPILER) $(JAVA_FLAGS) $(SOURCE_FILES)

.PHONY: clean
clean:
	@$(RM) -rf $(GENERATED_FILES)

.PHONY: dist
dist: build
	$(GENERATE_CHANGE_LOG.$(REVISION_CONTROL_SYSTEM)); \
	find . -name "*.bak" | xargs --no-run-if-empty rm; \
	ssh $(DIST_SCP_USER_AND_HOST) mkdir -p $(DIST_SCP_DIRECTORY) && \
	scp -r www/* $(DIST_SCP_USER_AND_HOST):$(DIST_SCP_DIRECTORY)/.. && \
	cd $(if $(wildcard ../trunk),../..,..) && \
	tar -cf $(TAR_FILE_OF_THE_DAY) $(FILE_LIST) && \
	rm -f $(TAR_FILE_OF_THE_DAY).gz && \
	gzip $(TAR_FILE_OF_THE_DAY) && \
	scp $(TAR_FILE_OF_THE_DAY).gz $(DIST_SCP_USER_AND_HOST):$(DIST_SCP_DIRECTORY)/ && \
	ssh $(DIST_SCP_USER_AND_HOST) ln -s -f $(DIST_SCP_DIRECTORY)/$(TAR_FILE_OF_THE_DAY).gz $(DIST_SCP_DIRECTORY)/../$(PROJECT_NAME).tgz

$(PROJECT_NAME).jar: build
	@$(call CREATE_OR_UPDATE_JAR,c,$(CURDIR)) && \
	$(call CREATE_OR_UPDATE_JAR,u,$(SALMA_HAYEK))

.PHONY: bindist
bindist: $(PROJECT_NAME)-bindist.tgz

$(PROJECT_NAME)-bindist.tgz: build $(BINDIST_FILES)
	@cd .. && tar -zcf $(addprefix $(DIRECTORY_NAME)/,$@ $(FILTERED_BINDIST_FILES))

.PHONY: build.subdirs
build.subdirs:
	@$(foreach SUBDIR,$(SUBDIRS),$(MAKE) -C $(SUBDIR);)

.PHONY: echo.%
echo.%:
	@echo '$($*)'
