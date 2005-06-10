# You may use:
#   make
#   make clean
#   make dist
#   make bindist

# You can set:
#   JAVA_COMPILER to "gcjx", "javac", or a binary of your choice.

# Your calling Makefile:
#   must include ../salma-hayek/universal.make

#   must have any extra rules after the include
#   must set any variables before the include
#     (Variables used on either side of the colon in rules are evaluated on
#      the first pass. By the time you get to the other side of the include,
#      you're too late to override them.)

# Use "VARIABLE ?= default" to assign a values iff the variable hasn't already
# been set.

# ----------------------------------------------------------------------------
# Ensure we're running a suitable version of make(1).
# ----------------------------------------------------------------------------

# It would be nice if this could be included in the boilerplate but $(MAKEFILE_LIST)
# is only available in make-3.80 and, without that, we can't include the boilerplate.
REQUIRED_MAKE_VERSION = 3.80
REAL_MAKE_VERSION = $(firstword $(MAKE_VERSION))
EARLIER_MAKE_VERSION = $(firstword $(sort $(REAL_MAKE_VERSION) $(REQUIRED_MAKE_VERSION)))
ifneq "$(REQUIRED_MAKE_VERSION)" "$(EARLIER_MAKE_VERSION)"
    $(error This makefile requires at least version $(REQUIRED_MAKE_VERSION) of GNU make, but you're using $(REAL_MAKE_VERSION))
endif

# ----------------------------------------------------------------------------
# Disable legacy make behavior.
# ----------------------------------------------------------------------------

# We used to disable suffix rules, but the default compilation rules are suffix
# rules, and we want to use them in per-directory.make.
#.SUFFIXES:

.DEFAULT:
.DELETE_ON_ERROR:
.SECONDARY:

# ----------------------------------------------------------------------------
# Define useful stuff not provided by GNU make.
# ----------------------------------------------------------------------------

EXE_SUFFIX.CYGWIN_NT-5.0 = .exe
EXE_SUFFIX = $(EXE_SUFFIX.$(TARGET_OS))

PATH_SEPARATOR.CYGWIN_NT-5.0 = '";"'
PATH_SEPARATOR = $(if $(PATH_SEPARATOR.$(TARGET_OS)),$(PATH_SEPARATOR.$(TARGET_OS)),:)

convertCygwinToWin32Path = $(shell echo $(1) | perl -pe 's@/cygdrive/([a-z])@$$1:@g')

pathsearch = $(firstword $(wildcard $(addsuffix /$(1)$(EXE_SUFFIX),$(subst :, ,$(PATH)))))
makepath = $(call convertCygwinToWin32Path,$(subst $(SPACE),$(PATH_SEPARATOR),$(strip $(1))))
getAbsolutePath = $(patsubst @%,$(CURDIR)/%,$(patsubst @/%,/%,$(patsubst %,@%,$(1))))

SPACE := $(subst :, ,:)

# ----------------------------------------------------------------------------
# Locate salma-hayek.
# ----------------------------------------------------------------------------

MOST_RECENT_MAKEFILE_DIRECTORY = $(dir $(word $(words $(MAKEFILE_LIST)),$(MAKEFILE_LIST)))
SALMA_HAYEK := $(call getAbsolutePath,$(MOST_RECENT_MAKEFILE_DIRECTORY))

# ----------------------------------------------------------------------------
# Locate Java.
# ----------------------------------------------------------------------------

# The current version of Java has a well-known home on Darwin.
DEFAULT_JAVA_HOME.Darwin = /System/Library/Frameworks/JavaVM.framework/Versions/CurrentJDK/Home

# Assume the tools are on the path if $(JAVA_HOME) isn't specified.
# Note the := to evaluate $(JAVA_HOME) before we potentially default it to a $(error).
JAVA_PATH := $(if $(JAVA_HOME),$(JAVA_HOME)/bin/)

DEFAULT_JAVA_HOME = $(DEFAULT_JAVA_HOME.$(TARGET_OS))
JAVA_HOME ?= $(if $(DEFAULT_JAVA_HOME),$(DEFAULT_JAVA_HOME),$(error Please set $$(JAVA_HOME)))

JAR = $(JAVA_PATH)jar
JAVAH := $(JAVA_PATH)javah

# ----------------------------------------------------------------------------
# Find the source.
# ----------------------------------------------------------------------------

SOURCE_EXTENSIONS += c
SOURCE_EXTENSIONS += cpp
SOURCE_EXTENSIONS += m
SOURCE_EXTENSIONS += mm

HEADER_EXTENSIONS += h

# ----------------------------------------------------------------------------
# Work out what we're going to generate.
# ----------------------------------------------------------------------------

TARGET_OS := $(shell uname)

# ----------------------------------------------------------------------------
# Sensible C family compiler flags.
# ----------------------------------------------------------------------------

EXTRA_INCLUDE_PATH += "$(SALMA_HAYEK)/native/Headers"

CFLAGS += -std=c99
C_AND_CXXFLAGS += -fPIC
C_AND_CXXFLAGS += -g
# Maximum warnings...
C_AND_CXX_FLAGS += -W -Wall -Werror
OBJC_AND_OBJCXX_FLAGS += -Wno-protocol -Wundeclared-selector
# ... but assume that C++ will eventually subsume C99.
CXXFLAGS += -Wno-long-long
PURE_C_AND_CXX_FLAGS += -pedantic
# The use of whitespace here is carefully crafted to work with directories
# whose names contain spaces, something common on Win32.
CPPFLAGS += $(subst $(SPACE)", -I", $(EXTRA_INCLUDE_PATH))

CFLAGS += $(C_AND_CXX_FLAGS)
CXXFLAGS += $(C_AND_CXX_FLAGS)

OBJECTIVE_SOURCE_PATTERNS += %.m
OBJECTIVE_SOURCE_PATTERNS += %.mm

C_AND_CXX_FLAGS += $(if $(filter $(OBJECTIVE_SOURCE_PATTERNS),$<),,$(PURE_C_AND_CXX_FLAGS))

# ----------------------------------------------------------------------------
# Extra compiler and (mainly) linker flags for building JNI.
# ----------------------------------------------------------------------------

JNI_LIBRARY_LDFLAGS.Darwin += -dynamiclib -framework JavaVM
JNI_LIBRARY_PREFIX.Darwin = lib
JNI_LIBRARY_EXTENSION.Darwin = jnilib
# The default $(LD) doesn't know about -dynamiclib on Darwin.
# This doesn't hurt on Linux, indeed it generally saves having to specify nonsense like -lstdc++.
LD = $(CXX)
# The default $(CC) used by $(LINK.o) doesn't know about the Darwin equivalent of -lstdc++.
CC = $(CXX)

EXTRA_INCLUDE_PATH.Linux += "$(JAVA_HOME)/include/linux"
JNI_LIBRARY_LDFLAGS.Linux += -shared
JNI_LIBRARY_PREFIX.Linux = lib
JNI_LIBRARY_EXTENSION.Linux = so

EXTRA_INCLUDE_PATH.CYGWIN_NT-5.0 += "$(JAVA_HOME)/include/win32"
JNI_LIBRARY_LDFLAGS.CYGWIN_NT-5.0 += -shared -Wl,--add-stdcall-alias
JNI_LIBRARY_PREFIX.CYGWIN_NT-5.0 =
JNI_LIBRARY_EXTENSION.CYGWIN_NT-5.0 = dll

EXTRA_INCLUDE_PATH += "$(JAVA_HOME)/include"
EXTRA_INCLUDE_PATH += $(EXTRA_INCLUDE_PATH.$(TARGET_OS))
JNI_LIBRARY_LDFLAGS += $(JNI_LIBRARY_LDFLAGS.$(TARGET_OS))
JNI_LIBRARY_PREFIX = $(JNI_LIBRARY_PREFIX.$(TARGET_OS))
JNI_LIBRARY_EXTENSION = $(JNI_LIBRARY_EXTENSION.$(TARGET_OS))

# ----------------------------------------------------------------------------
# Work out what native code, if any, we need to build.
# ----------------------------------------------------------------------------

NATIVE_SOURCE_PATTERN = $(CURDIR)/native/$(OS)/*/*.$(EXTENSION)
NATIVE_SOURCE = $(foreach OS,all $(TARGET_OS),$(foreach EXTENSION,$(SOURCE_EXTENSIONS),$(NATIVE_SOURCE_PATTERN)))
SUBDIRS := $(sort $(patsubst %/,%,$(dir $(wildcard $(NATIVE_SOURCE)))))

# ----------------------------------------------------------------------------

PROJECT_ROOT = $(CURDIR)

SVN := $(call pathsearch,svn)
PROJECT_NAME_FROM_SVN = $(shell svn info | perl -w -ne 'm{URL: .*?/([^/]*)(/trunk)?$$} && print($$1);')
PROJECT_NAME := $(if $(SVN),$(PROJECT_NAME_FROM_SVN),$(notdir $(PROJECT_ROOT)))

SCRIPT_PATH=$(SALMA_HAYEK)/bin

# By default, distributions end up under http://www.jessies.org/~enh/
DIST_SCP_USER_AND_HOST=enh@jessies.org
DIST_SCP_DIRECTORY="~/public_html/software/$(PROJECT_NAME)/nightly-builds"

SOURCE_FILES=$(shell find $(PROJECT_ROOT)/src -type f -name "*.java")
TAR_FILE_OF_THE_DAY := $(shell date +$(PROJECT_NAME)-%Y-%m-%d.tar)
DIRECTORY_FOR_TAR_FILE_OF_THE_DAY := $(if $(wildcard ../trunk),../..,..)

REVISION_CONTROL_SYSTEM := $(if $(wildcard .svn),svn,cvs)

define GENERATE_CHANGE_LOG.svn
  svn log > ChangeLog
endef

define GENERATE_CHANGE_LOG.cvs
  $(if $(shell which cvs2cl),cvs2cl,cvs2cl.pl) --hide-filenames
endef

CREATE_OR_UPDATE_JAR=cd $(2)/classes && $(JAR) $(1)f $(CURDIR)/$@ $(notdir $(wildcard $(2)/classes/*))

GENERATED_FILES += classes
GENERATED_FILES += generated
GENERATED_FILES += $(PROJECT_NAME).jar
GENERATED_FILES += $(PROJECT_NAME)-bindist.tgz

grep-v = $(filter-out @@%,$(filter-out %@@,$(subst $(1),@@ @@,$(2))))
DIRECTORY_NAME := $(notdir $(CURDIR))

BINDIST_FILES += $(PROJECT_NAME).jar
BINDIST_FILES += COPYING
BINDIST_FILES += README
BINDIST_FILES += $(foreach BASE_NAME,$(notdir $(SUBDIRS)),$(patsubst $(CURDIR)/%,%,$(DEFAULT_TARGET.$(BASE_NAME))))
BINDIST_DIRS += bin
BINDIST_DIRS += $(wildcard doc)
BINDIST_FILES += $(shell find $(BINDIST_DIRS) -type f | perl -ne 'm@(^|/)(CVS|\.svn)($|/)@ || print')

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
FILE_LIST_WITH_DIRECTORIES += generated
FILE_LIST_WITH_DIRECTORIES += ChangeLog # The ChangeLog should never be checked in, but should be in distributions.
FILE_LIST = $(subst /./,/,$(addprefix $(PROJECT_NAME)/,$(filter-out $(dir $(FILE_LIST_WITH_DIRECTORIES)),$(FILE_LIST_WITH_DIRECTORIES))))

# ----------------------------------------------------------------------------
# Choose a Java compiler.
# ----------------------------------------------------------------------------

JAVA_COMPILER ?= javac

ifeq "$(wildcard $(JAVA_COMPILER)$(EXE_SUFFIX))$(call pathsearch,$(JAVA_COMPILER))" ""
  REQUESTED_JAVA_COMPILER := $(JAVA_COMPILER)
  JAVA_COMPILER = $(JAVA_HOME)/bin/$(REQUESTED_JAVA_COMPILER)
  ifeq "$(wildcard $(JAVA_COMPILER)$(EXE_SUFFIX))" ""
    JAVA_COMPILER = $(error Unable to find $(REQUESTED_JAVA_COMPILER))
  endif
endif

# Make the compiler's leafname available for simple javac/gcjx tests.
COMPILER_TYPE = $(notdir $(JAVA_COMPILER))

# ----------------------------------------------------------------------------
# Find the boot classes.
# Note: we only look for rt.jar; someday we might need to find the other jars.
# ----------------------------------------------------------------------------

# Traditional Java location:
RT_JAR=$(JAVA_HOME)/jre/lib/rt.jar
ifeq "$(wildcard $(RT_JAR))" ""
  # Apple:
  RT_JAR=/System/Library/Frameworks/JavaVM.framework/Classes/classes.jar
  ifeq "$(wildcard $(RT_JAR))" ""
    # Fall back to searching:
    RT_JAR=$(firstword $(shell locate /rt.jar))
  endif
endif

# ----------------------------------------------------------------------------
# Set up the bootclasspath.
# ----------------------------------------------------------------------------
BOOT_CLASS_PATH.gcjx += $(RT_JAR)
BOOT_CLASS_PATH += $(BOOT_CLASS_PATH.$(COMPILER_TYPE))

# ----------------------------------------------------------------------------
# Set up the classpath.
# ----------------------------------------------------------------------------
CLASS_PATH += $(SALMA_HAYEK)/classes
CLASS_PATH += $(SALMA_HAYEK)/MRJ141Stubs.jar
CLASS_PATH += $(CLASS_PATH.$(COMPILER_TYPE))

# ----------------------------------------------------------------------------
# Sort out the flags our chosen compiler needs.
# ----------------------------------------------------------------------------

JAVA_FLAGS += $(JAVA_FLAGS.$(COMPILER_TYPE))
JAVA_FLAGS += $(addprefix -bootclasspath ,$(call makepath,$(BOOT_CLASS_PATH)))
JAVA_FLAGS += $(addprefix -classpath ,$(call makepath,$(CLASS_PATH)))
JAVA_FLAGS += -d classes/
JAVA_FLAGS += -sourcepath src/
JAVA_FLAGS += -deprecation

JAVA_FLAGS.gcjx += -pedantic -verbose -fverify # -error -- reinstate later!

JAVA_FLAGS.javac += -Xlint -Xlint:-serial

# We should also ensure that we build class files that can be used on
# Mac OS, regardless of where we build.
JAVA_FLAGS += -target 1.5

# While while we're at it, it's probably worth refusing to compile source
# using 1.5 features as long as we have one platform that's not ready.
# This should also weed out any attempt to use a Java older than 1.4.
JAVA_FLAGS += -source 1.5

define BUILD_JAVA
  @echo Recompiling the world... && \
  rm -rf classes && \
  mkdir -p classes && \
  $(JAVA_COMPILER) $(JAVA_FLAGS) $(call convertCygwinToWin32Path,$(SOURCE_FILES))
endef

# ----------------------------------------------------------------------------
# Prevent us from using per-directory.make's local variables in universal.make
# make doesn't support variable scoping, so this requires some cunning.
# ----------------------------------------------------------------------------

# -include because, on the first run, it doesn't exist and, even it has seen
# the rule for building it, make doesn't run the rule (it doesn't run any
# rules in that phase of makefile processing).
# You still seem to get protection of a new local variable the first time you
# run make after adding it.
# So that's OK then.
-include generated/local-variables.make

# We want to use the $(BASE_NAME) of the preceding scope in error messages.
LOCAL_VARIABLES := $(filter-out BASE_NAME,$(LOCAL_VARIABLES))

define copyLocalVariable
  $(1).$(BASE_NAME) := $$($(1))
endef
define unsetLocalVariable
  $(1) = $$(shell $(RM) generated/local-variables.make)$$(error makefile bug: local variable $(1) from scope $(BASE_NAME) (with value "$($(1).$(BASE_NAME))") was referred to in scope $$(BASE_NAME))
endef

# We need to $(eval) each assignment individually before they're concatenated
# by $(foreach) and hence turned into a syntax error.
forEachLocalVariable = $(foreach LOCAL_VARIABLE,$(LOCAL_VARIABLES),$(eval $(call $(1),$(LOCAL_VARIABLE))))

define closeLocalVariableScope
  $(call forEachLocalVariable,copyLocalVariable)
  $(call forEachLocalVariable,unsetLocalVariable)
endef

# ----------------------------------------------------------------------------
# Variables above this point,
# rules below...
# ----------------------------------------------------------------------------

.PHONY: build
build: build.java

.PHONY: build.java
build.java: $(SOURCE_FILES)
	$(BUILD_JAVA)

# We don't usually build individual classes but rule for generating the JNI
# header correctly specifies a dependency on a .class file which may be
# absent.
# If we were to make this class file depend on PHONY build.java, it would
# always be rebuilt.
%.class: $(SOURCE_FILES)
	$(BUILD_JAVA)

.PHONY: clean
clean:
	@$(RM) -r $(GENERATED_FILES)

$(DIRECTORY_FOR_TAR_FILE_OF_THE_DAY)/$(TAR_FILE_OF_THE_DAY).gz: build
	$(GENERATE_CHANGE_LOG.$(REVISION_CONTROL_SYSTEM)); \
	find . -name "*.bak" | xargs --no-run-if-empty rm; \
	$(SCRIPT_PATH)/svn-log-to-html.rb < ChangeLog > ChangeLog.html && \
	cd $(DIRECTORY_FOR_TAR_FILE_OF_THE_DAY) && \
	tar -cf $(TAR_FILE_OF_THE_DAY) $(FILE_LIST) && \
	rm -f $(TAR_FILE_OF_THE_DAY).gz && \
	gzip $(TAR_FILE_OF_THE_DAY)

.PHONY: dist
dist: $(DIRECTORY_FOR_TAR_FILE_OF_THE_DAY)/$(TAR_FILE_OF_THE_DAY).gz
	ssh $(DIST_SCP_USER_AND_HOST) mkdir -p $(DIST_SCP_DIRECTORY) && \
	scp ChangeLog.html $(DIST_SCP_USER_AND_HOST):$(DIST_SCP_DIRECTORY)/.. && \
	scp -r www/* $(DIST_SCP_USER_AND_HOST):$(DIST_SCP_DIRECTORY)/.. && \
	scp $< $(DIST_SCP_USER_AND_HOST):$(DIST_SCP_DIRECTORY)/ && \
	ssh $(DIST_SCP_USER_AND_HOST) ln -s -f $(DIST_SCP_DIRECTORY)/$(TAR_FILE_OF_THE_DAY).gz $(DIST_SCP_DIRECTORY)/../$(PROJECT_NAME).tgz

$(PROJECT_NAME).jar: build
	@$(call CREATE_OR_UPDATE_JAR,c,$(CURDIR)) && \
	$(call CREATE_OR_UPDATE_JAR,u,$(SALMA_HAYEK))

.PHONY: bindist
bindist: $(PROJECT_NAME)-bindist.tgz

$(PROJECT_NAME)-bindist.tgz: build $(BINDIST_FILES)
	@cd .. && tar -zcf $(addprefix $(DIRECTORY_NAME)/,$@ $(BINDIST_FILES))

# ----------------------------------------------------------------------------
# How to build a .app directory for Mac OS
# ----------------------------------------------------------------------------

.PHONY: app
app: build
	APP_DIR=../$(PROJECT_NAME).app/Contents && \
	rm -rf $$APP_DIR && \
	mkdir -p $$APP_DIR/MacOS && \
	mkdir -p $$APP_DIR/Resources && \
	cp -r . $$APP_DIR/Resources/$(PROJECT_NAME) && \
	cp -r $(SALMA_HAYEK) $$APP_DIR/Resources/salma-hayek && \
	cd $$APP_DIR/MacOS && \
	echo -e '#!/bin/bash\ncd\nexec `dirname $$0`/../Resources/$(PROJECT_NAME)/bin/$(PROJECT_NAME)\n' > $(PROJECT_NAME) && \
	chmod a+x $(PROJECT_NAME)

# ----------------------------------------------------------------------------
# Implicit rules for compiling Objective C and Objective C++ source.
# ----------------------------------------------------------------------------

COMPILE.m = $(COMPILE.c) $(OBJC_AND_OBJCXX_FLAGS)
%.o: %.m
	$(COMPILE.m) $(OUTPUT_OPTION) $<

COMPILE.mm = $(COMPILE.cpp) $(OBJC_AND_OBJCXX_FLAGS)
%.o: %.mm
	$(COMPILE.mm) $(OUTPUT_OPTION) $<

# ----------------------------------------------------------------------------
# Rules for debugging.
# ----------------------------------------------------------------------------

.PHONY: echo.%
echo.%:
	@echo '"$($*)"'

# ----------------------------------------------------------------------------
# Rules for making makefiles.
# ----------------------------------------------------------------------------

generated/local-variables.make: $(SALMA_HAYEK)/per-directory.make $(SALMA_HAYEK)/universal.make
	@mkdir -p $(@D) && \
	perl -w -ne '(m/^\s*(\S+)\s*[:+]?=/ || m/^\s*define\s*(\S+)/) && print("LOCAL_VARIABLES += $$1\n")' $< | sort -u > $@

# ----------------------------------------------------------------------------
# The magic incantation to build and clean all the native subdirectories.
# Including per-directory.make more than once is bound to violate the
# variables-before-rules dictum.
# per-directory.make needs to cope with that but it'd be best if it doesn't impose
# that constraint on the rest of universal.make - so let's keep this last.
# ----------------------------------------------------------------------------

define buildNativeDirectory
  SOURCE_DIRECTORY = $(1)
  include $(SALMA_HAYEK)/per-directory.make
endef

DUMMY := $(foreach SUBDIR,$(SUBDIRS),$(eval $(call buildNativeDirectory,$(SUBDIR)))$(closeLocalVariableScope))
BASE_NAME = (rules)
