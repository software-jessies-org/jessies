# You may use:
#   make
#   make clean
#   make native
#   make native-clean
#   make native-dist
#   make source-dist
#   make www-dist

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
# Locate salma-hayek.
# ----------------------------------------------------------------------------

getAbsolutePath = $(patsubst @%,$(CURDIR)/%,$(patsubst @/%,/%,$(patsubst %,@%,$(1))))

MOST_RECENT_MAKEFILE_DIRECTORY = $(dir $(word $(words $(MAKEFILE_LIST)),$(MAKEFILE_LIST)))
SALMA_HAYEK := $(call getAbsolutePath,$(MOST_RECENT_MAKEFILE_DIRECTORY))

# ----------------------------------------------------------------------------
# Work out what we're going to generate.
# ----------------------------------------------------------------------------

TARGET_OS_SCRIPT = $(SALMA_HAYEK)/bin/target-os.rb
SCRIPTS_WHICH_AFFECT_COMPILER_FLAGS += $(TARGET_OS_SCRIPT)
TARGET_OS := $(shell $(TARGET_OS_SCRIPT))

# ----------------------------------------------------------------------------
# Define useful stuff not provided by GNU make.
# ----------------------------------------------------------------------------

EXE_SUFFIX.Cygwin = .exe
EXE_SUFFIX = $(EXE_SUFFIX.$(TARGET_OS))

NATIVE_PATH_SEPARATOR.$(TARGET_OS) = :
# Quoted to get through bash without being treated as a statement terminator.
NATIVE_PATH_SEPARATOR.Cygwin = ";"
NATIVE_PATH_SEPARATOR = $(NATIVE_PATH_SEPARATOR.$(TARGET_OS))

convertToNativeFilenames.$(TARGET_OS) = $(1)
# javac is happy with forward slashes (as is the underlying Win32 API).
convertToNativeFilenames.Cygwin = $(if $(1),$(shell cygpath --mixed $(1)))
convertToNativeFilenames = $(convertToNativeFilenames.$(TARGET_OS))

searchPath = $(shell which $(1))
makeNativePath = $(subst $(SPACE),$(NATIVE_PATH_SEPARATOR),$(call convertToNativeFilenames,$(1)))

SPACE = $(subst :, ,:)

# I sprinkled the code with calls to dump the wall-clock time and counted the
# lines of output to work out between which calls the time was disappearing.
# I moved the calls around to isolate the particularly expensive lines.
# When I'd isolated things which I couldn't easily improve,
# I left one copy either side of each line - so you can get some idea of the time
# taken by the line.
# Yes, it's crude but, when faced with a build time of ~30s, it was adequate.
#takeProfileSample = $(eval $(shell date --iso=s 1>&2))
takeProfileSample =

# ----------------------------------------------------------------------------
# Locate Java.
# ----------------------------------------------------------------------------

findMakeFriendlyEquivalentName.$(TARGET_OS) = $(1)
# make-friendly means forward slashes and no spaces.
# Quoted because the original may contain backslashes.
findMakeFriendlyEquivalentName.Cygwin = $(shell cygpath --mixed --short-name '$(1)')
findMakeFriendlyEquivalentName = $(findMakeFriendlyEquivalentName.$(TARGET_OS))

JDK_ROOT_SCRIPT = $(SALMA_HAYEK)/bin/find-jdk-root.rb
SCRIPTS_WHICH_AFFECT_COMPILER_FLAGS += $(JDK_ROOT_SCRIPT)
JDK_ROOT := $(call findMakeFriendlyEquivalentName,$(shell $(JDK_ROOT_SCRIPT)))

# ----------------------------------------------------------------------------
# We use our own replacement for javah(1).
# ----------------------------------------------------------------------------

JAVAHPP = $(SALMA_HAYEK)bin/javahpp.rb

# ----------------------------------------------------------------------------
# Find the source.
# ----------------------------------------------------------------------------

SOURCE_EXTENSIONS += c
OBJECT_EXTENSION.c = o
SOURCE_EXTENSIONS += cpp
OBJECT_EXTENSION.cpp = o
SOURCE_EXTENSIONS += m
OBJECT_EXTENSION.m = o
SOURCE_EXTENSIONS += mm
OBJECT_EXTENSION.mm = o

SOURCE_EXTENSIONS += wxs
OBJECT_EXTENSION.wxs = wixobj

HEADER_EXTENSIONS += h

HEADER_EXTENSIONS += wxi

# ----------------------------------------------------------------------------
# Sensible C family compiler flags.
# ----------------------------------------------------------------------------

EXTRA_INCLUDE_PATH += $(SALMA_HAYEK)/native/Headers

CFLAGS += -std=c99
PIC_FLAG.$(TARGET_OS) = -fPIC
# Cygwin's g++ says:
# warning: -fPIC ignored for target (all code is position independent)
# And warnings are errors.
PIC_FLAG.Cygwin =
C_AND_CXX_FLAGS += $(PIC_FLAG.$(TARGET_OS))
C_AND_CXX_FLAGS += -g
# Maximum warnings...
C_AND_CXX_FLAGS += -W -Wall -Werror
OBJC_AND_OBJCXX_FLAGS += -Wno-protocol -Wundeclared-selector
# ... but assume that C++ will eventually subsume C99.
CXXFLAGS += -Wno-long-long
PURE_C_AND_CXX_FLAGS += -pedantic
CPPFLAGS += $(foreach DIRECTORY,$(EXTRA_INCLUDE_PATH),-I$(DIRECTORY))

# Exposes ptsname on libc22 per the Linux man page but causes this on Mac OS X:
# /System/Library/Frameworks/CoreServices.framework/Frameworks/CarbonCore.framework/Headers/MacMemory.h:1587: error: 'bzero' was not declared in this scope
# The 600 figure is from /usr/include/features.h.
CPPFLAGS.Linux += -D_XOPEN_SOURCE=600
CPPFLAGS += $(CPPFLAGS.$(TARGET_OS))

CFLAGS += $(C_AND_CXX_FLAGS)
CXXFLAGS += $(C_AND_CXX_FLAGS)

OBJECTIVE_SOURCE_PATTERNS += %.m
OBJECTIVE_SOURCE_PATTERNS += %.mm

C_AND_CXX_FLAGS += $(if $(filter $(OBJECTIVE_SOURCE_PATTERNS),$<),,$(PURE_C_AND_CXX_FLAGS))

COMPILE.m = $(COMPILE.c) $(OBJC_AND_OBJCXX_FLAGS)
COMPILE.mm = $(COMPILE.cpp) $(OBJC_AND_OBJCXX_FLAGS)

# The local variable support doesn't like this (and perhaps code macros in general)
# being defined in per-directory.make.
# You end up with a variable called OBJECTS.copyLocalVariable, containing a $(error),
# being evaluated by the call to copyLocalVariable at the end of the next scope.
define defineObjectsPerLanguage
  OBJECTS.$(1) = $(patsubst $(SOURCE_DIRECTORY)/%.$(1),$(GENERATED_DIRECTORY)/%.$(OBJECT_EXTENSION.$(1)),$(filter %.$(1),$(SOURCES)))
  # The Perl script needs help if we're going to have variable variable names.
  LOCAL_VARIABLES += OBJECTS.$(1)
endef

# It's non-obvious but utterly essential for single threaded
# applications which dynamically load multi-threaded libraries
# to use this option - or runtime crashes await.
# The default allocator is *not* thread-safe, despite g++ -v's
# apparent claims to the contrary (which, I think, are actually
# just claiming the availability of a thread-safe allocator).
LDFLAGS.Linux += -pthread

# launcher needs this on Linux
LDFLAGS.Linux += -ldl

LDFLAGS += $(LDFLAGS.$(TARGET_OS))

# ----------------------------------------------------------------------------
# Extra compiler and (mainly) linker flags for building JNI.
# ----------------------------------------------------------------------------

JNI_LIBRARY_LDFLAGS += $(LDFLAGS)

JNI_LIBRARY_LDFLAGS.Darwin += -dynamiclib -framework JavaVM
JNI_LIBRARY_PREFIX.Darwin = lib
JNI_LIBRARY_EXTENSION.Darwin = jnilib
# The default $(LD) doesn't know about -dynamiclib on Darwin.
# This doesn't hurt on Linux, indeed it generally saves having to specify nonsense like -lstdc++.
LD = $(CXX)
# The default $(CC) used by $(LINK.o) doesn't know about the Darwin equivalent of -lstdc++.
CC = $(CXX)

# Note that our Solaris build assumes GCC rather than Sun's compiler.
# GCC's -shared option, which we use on Linux, exists, but produces link
# errors. -G, as used in Sun's tutorial examples with their own compiler works.
EXTRA_INCLUDE_PATH.SunOS += $(JDK_ROOT)/include/solaris
JNI_LIBRARY_LDFLAGS.SunOS += -G
JNI_LIBRARY_PREFIX.SunOS = lib
JNI_LIBRARY_EXTENSION.SunOS = so

EXTRA_INCLUDE_PATH.Linux += $(JDK_ROOT)/include/linux
JNI_LIBRARY_LDFLAGS.Linux += -shared
JNI_LIBRARY_PREFIX.Linux = lib
JNI_LIBRARY_EXTENSION.Linux = so

EXTRA_INCLUDE_PATH.Cygwin += $(JDK_ROOT)/include/win32
JNI_LIBRARY_LDFLAGS.Cygwin += -shared -Wl,--add-stdcall-alias -Wl,--image-base,0x68000000
JNI_LIBRARY_PREFIX.Cygwin =
JNI_LIBRARY_EXTENSION.Cygwin = dll

EXTRA_INCLUDE_PATH += $(JDK_ROOT)/include
EXTRA_INCLUDE_PATH += $(EXTRA_INCLUDE_PATH.$(TARGET_OS))

NON_EXISTENT_INCLUDE_DIRECTORIES := $(filter-out $(wildcard $(EXTRA_INCLUDE_PATH)),$(EXTRA_INCLUDE_PATH))
ifneq "$(NON_EXISTENT_INCLUDE_DIRECTORIES)" ""
  $(error Could not find $(NON_EXISTENT_INCLUDE_DIRECTORIES) - perhaps the first java on your PATH isn't in a JDK)
endif

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

PROJECT_NAME = $(notdir $(PROJECT_ROOT))

SCRIPT_PATH=$(SALMA_HAYEK)/bin

# By default, distributions end up under http://software.jessies.org/
DIST_SSH_USER_AND_HOST=software@jessies.org
# The html files are copied into the parent directory.
DIST_DIRECTORY=/home/software/downloads/$(PROJECT_NAME)

$(takeProfileSample)
SOURCE_FILES := $(shell find $(PROJECT_ROOT)/src -type f -name "*.java")
$(takeProfileSample)
SOURCE_DIST_FILE = $(PROJECT_NAME).tgz

REVISION_CONTROL_SYSTEM += $(if $(wildcard .svn),svn)
REVISION_CONTROL_SYSTEM += $(if $(wildcard CVS),cvs)
REVISION_CONTROL_SYSTEM += $(if $(wildcard SCCS),bk)
REVISION_CONTROL_SYSTEM := $(strip $(REVISION_CONTROL_SYSTEM))
REVISION_CONTROL_SYSTEM := $(if $(REVISION_CONTROL_SYSTEM),$(REVISION_CONTROL_SYSTEM),unknown)

define GENERATE_CHANGE_LOG.svn
  svn log > ChangeLog
endef

define GENERATE_CHANGE_LOG.cvs
  $(if $(shell which cvs2cl),cvs2cl,cvs2cl.pl) --hide-filenames
endef

CREATE_OR_UPDATE_JAR=cd $(2)/classes && jar $(1)f $(CURDIR)/$@ $(notdir $(wildcard $(2)/classes/*))

GENERATED_FILES += ChangeLog
GENERATED_FILES += ChangeLog.html
GENERATED_FILES += classes
GENERATED_FILES += .generated
GENERATED_FILES += $(PROJECT_NAME).jar

export REVISION := $(shell svn info | perl -ne 'm/^Revision: (\d+)/ && print("$$1\n")')

# ----------------------------------------------------------------------------
# Choose a Java compiler.
# ----------------------------------------------------------------------------

JAVA_COMPILER ?= javac

ifeq "$(wildcard $(JAVA_COMPILER)$(EXE_SUFFIX))$(call searchPath,$(JAVA_COMPILER))" ""
  REQUESTED_JAVA_COMPILER := $(JAVA_COMPILER)
  JAVA_COMPILER = $(REQUESTED_JAVA_COMPILER)
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
RT_JAR=$(JDK_ROOT)/jre/lib/rt.jar
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
CLASS_PATH += $(SALMA_HAYEK)/AppleJavaExtensions.jar
CLASS_PATH += $(SALMA_HAYEK)/swing-worker.jar
TOOLS_JAR := $(wildcard $(JDK_ROOT)/lib/tools.jar)
CLASS_PATH += $(TOOLS_JAR)
CLASS_PATH += $(CLASS_PATH.$(COMPILER_TYPE))

# ----------------------------------------------------------------------------
# Sort out the flags our chosen compiler needs.
# ----------------------------------------------------------------------------

JAVA_FLAGS += $(JAVA_FLAGS.$(COMPILER_TYPE))
JAVA_FLAGS += $(addprefix -bootclasspath ,$(call makeNativePath,$(BOOT_CLASS_PATH)))
JAVA_FLAGS += $(addprefix -classpath ,$(call makeNativePath,$(CLASS_PATH)))
JAVA_FLAGS += -d classes/
JAVA_FLAGS += -sourcepath src/
JAVA_FLAGS += -deprecation
JAVA_FLAGS += -g

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
  $(RM) -r classes && \
  mkdir -p classes && \
  $(JAVA_COMPILER) $(JAVA_FLAGS) $(call convertToNativeFilenames,$(SOURCE_FILES))
endef

# ----------------------------------------------------------------------------
# Find WiX
# ----------------------------------------------------------------------------

# There doesn't seem to be a standard installation home for this tool.
# Have the shell search for it on the PATH.
WIX_PATH =
CANDLE = $(WIX_PATH)candle
LIGHT = $(WIX_PATH)light
FILE_LIST_TO_WXI = $(SCRIPT_PATH)/file-list-to-wxi.rb

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
-include .generated/local-variables.make

# We want to use the $(BASE_NAME) of the preceding scope in error messages.
LOCAL_VARIABLES := $(filter-out BASE_NAME,$(LOCAL_VARIABLES))

define copyLocalVariable
  ERROR.$(1) =
  $(1).$(BASE_NAME) := $$($(1))
endef
define unsetLocalVariable
  ERROR.$(1) = $$(shell $(RM) .generated/local-variables.make)$$(error makefile bug: local variable $(1) from scope $(BASE_NAME) (with value "$($(1).$(BASE_NAME))") was referred to in scope $$(BASE_NAME))
  $(1) = $$(ERROR.$(1))
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

.PHONY: clean
clean:
	@find . -name "*.bak" | xargs $(RM) -r $(GENERATED_FILES)

.PHONY: native-clean
native-clean:
	@$(RM) -r .generated/native

ChangeLog.html: ChangeLog
	$(RM) $@ && \
	$(SCRIPT_PATH)/svn-log-to-html.rb < $< > $@

.PHONY: ChangeLog
ChangeLog:
	$(RM) $@ && \
	$(GENERATE_CHANGE_LOG.$(REVISION_CONTROL_SYSTEM))

# This is only designed to be run on jessies.org itself.
.PHONY: source-dist
source-dist: ../$(SOURCE_DIST_FILE)
	mkdir -p $(DIST_DIRECTORY) && \
	$(RM) $(DIST_DIRECTORY)/$(SOURCE_DIST_FILE) && \
	cp $< $(DIST_DIRECTORY)/

$(PROJECT_NAME).jar: build.java
	@$(call CREATE_OR_UPDATE_JAR,c,$(CURDIR)) && \
	$(call CREATE_OR_UPDATE_JAR,u,$(SALMA_HAYEK))

../$(SOURCE_DIST_FILE): ChangeLog
	cd .. && \
	tar -X $(SALMA_HAYEK)/dist-exclude -zcf $(SOURCE_DIST_FILE) $(PROJECT_NAME)/*

# This is only designed to be run on jessies.org itself.
.PHONY: www-dist
www-dist: ChangeLog.html
	mkdir -p $(DIST_DIRECTORY) && \
	cp -f ChangeLog.html $(DIST_DIRECTORY)/ && \
	if [ -d www/ ] ; then rsync -v -r www/* $(DIST_DIRECTORY)/ ; fi

# ----------------------------------------------------------------------------
# How to build a .app directory for Mac OS, package it as a ".dmg", and copy
# it to the web server.
# ----------------------------------------------------------------------------

# FIXME: native should depend on this on Mac OS X.
$(PROJECT_NAME).dmg: build
	@$(SCRIPT_PATH)/make-mac-os-app.rb $(PROJECT_NAME) $(SALMA_HAYEK)

# FIXME: This should be the Mac OS X native-dist target.
.PHONY: app-dist
app-dist: $(PROJECT_NAME).dmg
	ssh $(DIST_SSH_USER_AND_HOST) mkdir -p $(DIST_DIRECTORY) && \
	scp $< $(DIST_SSH_USER_AND_HOST):$(DIST_DIRECTORY)/

# ----------------------------------------------------------------------------
# Rules for debugging.
# ----------------------------------------------------------------------------

.PHONY: echo.%
echo.%:
	@echo '"$($*)"'

# ----------------------------------------------------------------------------
# Rules for making makefiles.
# ----------------------------------------------------------------------------

# The $$1 here is a Perl variable, not a make one.
.generated/local-variables.make: $(SALMA_HAYEK)/per-directory.make $(SALMA_HAYEK)/universal.make
	@mkdir -p $(@D) && \
	perl -w -ne '(m/^\s*(\S+)\s*[:+]?=/ || m/^\s*define\s*(\S+)/) && print("LOCAL_VARIABLES += $$1\n")' $< | sort -u > $@

# Several of the rules include $(MAKEFILE_LIST) in their prerequisites.
# All of the .o files, for example, depend on the makefiles.
# The makefiles delegate some of their work to scripts.
# These scripts help to determine, for example, compiler flags,
# so the .o files should depend on the scripts too.
# MAKEFILE_LIST is set by make itself.
# I'm not sure we can alter it.
# This empty makefile fragment will save us having to remember to use a
# different variable in place of MAKEFILE_LIST.
.generated/recompilation-trigger.make: $(SCRIPTS_WHICH_AFFECT_COMPILER_FLAGS)
	@mkdir -p $(@D) && \
	touch $@

-include .generated/recompilation-trigger.make

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

$(takeProfileSample)
DUMMY := $(foreach SUBDIR,$(SUBDIRS),$(eval $(call buildNativeDirectory,$(SUBDIR)))$(closeLocalVariableScope))
BASE_NAME = (rules)
$(takeProfileSample)

# Needs to be after we've included per-directory.make.
ALL_NATIVE_TARGETS = $(foreach SUBDIR,$(SUBDIRS),$(DESIRED_TARGETS.$(notdir $(SUBDIR))))

INSTALLER_PATTERN = %.msi %.msm
ALL_NATIVE_TARGETS_EXCEPT_INSTALLERS = $(filter-out $(INSTALLER_PATTERN),$(ALL_NATIVE_TARGETS))
MAKE_INSTALLER_FILE_LIST = find $(wildcard classes doc bin) $(patsubst $(PROJECT_ROOT)/%,%,$(ALL_NATIVE_TARGETS_EXCEPT_INSTALLERS)) -name .svn -prune -o -type f -print

# %.msm files aren't stand-alone installers
INSTALLER_BINARY = $(filter %.msi,$(ALL_NATIVE_TARGETS))

# TODO: The installer needs to be sure the Java is built but building Java
# on Windows into a Samba directory isn't working for me at the moment.
# Building on Cygwin is slow enough that it will be distressing, for the
# makefile maintainer, to keep rebuilding this unnecessarily.
# It shouldn't be impractical to notice that no .java file has changed and none
# have been added or removed.
$(PROJECT_ROOT)/.generated/native/Cygwin/WiX/Cygwin/component-definitions.wxi: $(ALL_NATIVE_TARGETS_EXCEPT_INSTALLERS) build.java

.PHONY: native
# Needs to be after ALL_NATIVE_TARGETS is defined.
native: $(ALL_NATIVE_TARGETS)

# make native-dist a silent no-op where there's nothing for it to do for the
# benefit of a simple, uniform nightly build script.
ifeq "$(INSTALLER_BINARY)" ""

native-dist:;

else

.PHONY: native-dist
native-dist: $(INSTALLER_BINARY)
	ssh $(DIST_SSH_USER_AND_HOST) mkdir -p $(DIST_DIRECTORY) && \
	scp $< $(DIST_SSH_USER_AND_HOST):$(DIST_DIRECTORY)/$(PROJECT_NAME)-$(TARGET_OS)$(suffix $<)

endif

ifeq "$(TARGET_OS)" "Cygwin"

# Among its many breakages, msiexec is more restrictive about slashes than Win32.
NATIVE_NAME_FOR_INSTALLER := '$(subst /,\,$(call convertToNativeFilenames,$(INSTALLER_BINARY)))'

# Use make -n install-commands to tell you what to copy and paste.
# Doing "make install" is too slow for experimentation.
.PHONY: install-commands
install-commands:
	# This (and double clicking) doesn't usually work if we're already installed.
	msiexec /i $(NATIVE_NAME_FOR_INSTALLER) /l'*'v .generated/install.log
	# Notepad because the log is in UCS-2.
	notepad .generated/install.log
	# This doesn't work if we're not already installed and doesn't fix deleted shortcuts if we are.
	msiexec /i $(NATIVE_NAME_FOR_INSTALLER) /l'*'v .generated/install.log REINSTALL=ALL REINSTALLMODE=vomus
	# This doesn't work unless it's exactly the same installer as you used to install it.
	msiexec /x $(NATIVE_NAME_FOR_INSTALLER)

endif
