# Where are we?
ifeq ($(MAKE_VERSION), 3.79)
    # Old versions of make (like the one Apple ships) don't
    # have $(MAKEFILE_LIST), so we need to have a fall-back
    # for that case.
    SALMA_HAYEK=~/Projects/salma-hayek/
else
    SALMA_HAYEK=$(dir $(word $(words $(MAKEFILE_LIST)),$(MAKEFILE_LIST)))
endif

# By default, distributions end up under http://www.jessies.org/~enh/
DIST_SCP_USER_AND_HOST=enh@jessies.org
DIST_SCP_DIRECTORY="~/public_html/software/$(PROJECT_NAME)/nightly-builds"

# A list of likely JDK rt.jar locations.
# Traditional Java setup:
KNOWN_RT_JAR_LOCATIONS+=$(JAVA_HOME)/jre/lib/rt.jar
# Apple:
KNOWN_RT_JAR_LOCATIONS+=/System/Library/Frameworks/JavaVM.framework/Classes/classes.jar
# Fall back to searching:
KNOWN_RT_JAR_LOCATIONS:=$(KNOWN_RT_JAR_LOCATIONS) $(shell locate /rt.jar)

SOURCE_FILES=$(shell find `pwd`/src -type f -name "*.java")
TAR_FILE_OF_THE_DAY=`date +$(PROJECT_NAME)-%Y-%m-%d.tar`

# Pick a JDK and append the MRJ141Stubs, in case they're there.
RT_JAR=$(firstword $(wildcard $(KNOWN_RT_JAR_LOCATIONS)))

BOOT_CLASS_PATH=$(RT_JAR)
ifneq ($(wildcard MRJ141Stubs.jar),)
    BOOT_CLASS_PATH:=$(BOOT_CLASS_PATH):MRJ141Stubs.jar
endif

REVISION_CONTROL_SYSTEM := $(if $(wildcard .svn),svn,cvs)

define GENERATE_CHANGE_LOG.svn
  svn log > ChangeLog
endef

define GENERATE_CHANGE_LOG.cvs
  $(if $(shell which cvs2cl),cvs2cl,cvs2cl.pl) --hide-filenames
endef

.PHONY: build
build: $(SOURCE_FILES)
	@echo Recompiling the world... && \
	 make clean && \
	 mkdir -p classes && \
	 jikes -bootclasspath $(BOOT_CLASS_PATH) -classpath $(SALMA_HAYEK)/classes -d classes/ -sourcepath src/ +D +P +Pall +Pno-serial +Pno-redundant-modifiers $(SOURCE_FILES)
	 @#javac -d classes/ -sourcepath src/ $(SOURCE_FILES)

GENERATED_FILES += classes

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
	tar --exclude=".#*" --exclude=CVS --exclude=.svn -cvf $(TAR_FILE_OF_THE_DAY) $(PROJECT_NAME)/ && \
	gzip $(TAR_FILE_OF_THE_DAY) && \
	scp $(TAR_FILE_OF_THE_DAY).gz $(DIST_SCP_USER_AND_HOST):$(DIST_SCP_DIRECTORY)/ && \
	ssh $(DIST_SCP_USER_AND_HOST) ln -s -f $(DIST_SCP_DIRECTORY)/$(TAR_FILE_OF_THE_DAY).gz $(DIST_SCP_DIRECTORY)/../$(PROJECT_NAME).tgz

JAR=$(if $(JAVA_HOME),$(JAVA_HOME)/bin/)jar
CREATE_OR_UPDATE_JAR=cd $(2) && $(JAR) $(1)f $(CURDIR)/$@ -C classes $(notdir $(wildcard $(2)/classes/*))

$(PROJECT_NAME).jar: build
	@$(call CREATE_OR_UPDATE_JAR,c,$(CURDIR)) && \
	$(call CREATE_OR_UPDATE_JAR,u,$(SALMA_HAYEK))

GENERATED_FILES += $(PROJECT_NAME).jar

grep-v = $(filter-out @@%,$(filter-out %@@,$(subst $(1),@@ @@,$(2))))
DIRECTORY_NAME := $(notdir $(CURDIR))

BINDIST_FILES += README COPYING $(PROJECT_NAME).jar
FILTERED_BINDIST_FILES = $(shell find $(BINDIST_FILES) -type f | grep -v CVS)

.PHONY: bindist
bindist: $(PROJECT_NAME)-bindist.tgz

$(PROJECT_NAME)-bindist.tgz: build $(BINDIST_FILES)
	@cd .. && tar -zcf $(addprefix $(DIRECTORY_NAME)/,$@ $(FILTERED_BINDIST_FILES))

GENERATED_FILES += $(PROJECT_NAME)-bindist.tgz
