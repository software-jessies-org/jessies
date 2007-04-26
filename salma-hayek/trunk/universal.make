# Use "VARIABLE ?= default" to assign a values iff the variable hasn't already
# been set.

# ----------------------------------------------------------------------------
# Ensure we're running a suitable version of make(1).
# ----------------------------------------------------------------------------

REQUIRED_MAKE_VERSION = 3.81
REAL_MAKE_VERSION = $(firstword $(MAKE_VERSION))
EARLIER_MAKE_VERSION = $(firstword $(sort $(REAL_MAKE_VERSION) $(REQUIRED_MAKE_VERSION)))
ifneq "$(REQUIRED_MAKE_VERSION)" "$(EARLIER_MAKE_VERSION)"
    $(warning This makefile assumes at least GNU make $(REQUIRED_MAKE_VERSION), but you're using $(REAL_MAKE_VERSION))
    $(warning )
    $(warning If you don't have build errors, you can ignore these warnings.)
    $(warning If you do have build errors, they are probably not make-related.)
    $(warning Exceptions include errors like:)
    $(warning make: *** virtual memory exhausted.  Stop.)
    $(warning ../salma-hayek/universal.make:494: *** makefile bug: local variable FIND_FALSE from scope setsid (with value "! -prune") was referred to in scope setsid.  Stop.)
endif

# ----------------------------------------------------------------------------
# Disable legacy make behavior.
# ----------------------------------------------------------------------------

# We used to disable suffix rules, but the default compilation rules are suffix
# rules, and we want to use them in per-directory.make.
#.SUFFIXES:

.DEFAULT:
.DELETE_ON_ERROR:

# ----------------------------------------------------------------------------
# Locate salma-hayek.
# ----------------------------------------------------------------------------

MOST_RECENT_MAKEFILE_DIRECTORY = $(patsubst %/,%,$(dir $(word $(words $(MAKEFILE_LIST)),$(MAKEFILE_LIST))))
SALMA_HAYEK := $(patsubst ../%,$(dir $(CURDIR))%,$(MOST_RECENT_MAKEFILE_DIRECTORY))

# ----------------------------------------------------------------------------
# Work out what we're going to generate.
# ----------------------------------------------------------------------------

SCRIPT_PATH = $(SALMA_HAYEK)/bin

TARGET_OS_SCRIPT = $(SCRIPT_PATH)/target-os.rb
SCRIPTS_WHICH_AFFECT_COMPILER_FLAGS += $(TARGET_OS_SCRIPT)
TARGET_OS_SCRIPT_OUTPUT := $(shell ruby $(TARGET_OS_SCRIPT))
TARGET_OS = $(word 1,$(TARGET_OS_SCRIPT_OUTPUT))
# (TARGET_ARCH has a special meaning to the built-in compilation rules.)
TARGET_ARCHITECTURE = $(word 2,$(TARGET_OS_SCRIPT_OUTPUT))
TARGET_DIRECTORY = $(word 3,$(TARGET_OS_SCRIPT_OUTPUT))

ifneq "$(REQUIRED_MAKE_VERSION)" "$(EARLIER_MAKE_VERSION)"
    ifeq "$(TARGET_OS)" "Cygwin"
        $(warning The make which comes with Cygwin 1.5.18-1 isn't good enough.)
        $(warning Try http://software.jessies.org/3rdParty/make-3.81-cygwin instead.)
    endif
    ifeq "$(TARGET_OS)" "Darwin"
        $(warning Try our pre-built http://software.jessies.org/3rdParty/make-3.81-darwin-universal instead.)
    endif
    ifeq "$(TARGET_OS)" "Linux"
        $(warning Debian testing/unstable has a new enough make if you do sudo apt-get install make.)
        $(warning Ubunutu "Dapper Drake" has a new enough make.)
        $(warning Or try http://software.jessies.org/3rdParty/make-3.81-linux.)
    endif
    ifeq "$(TARGET_OS)" "SunOS"
        $(warning Try installing GNU make from Blastwave, with pkg-get -i gmake.)
    endif
# The blank line separates any duplicate warning, which 3.80 seems fond of generating.
    $(warning )
endif

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

searchPath = $(shell which $(1) 2> /dev/null)
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

SYMLINK.$(TARGET_OS) = ln -s
# Use cp for the benefit of Windows native compilers which don't
# understand "symlinks".
SYMLINK.Cygwin = cp

define COPY_RULE
	mkdir -p $(@D) && \
	$(RM) $@ && \
	$(SYMLINK.$(TARGET_OS)) $< $@
endef

export MAKE

# ----------------------------------------------------------------------------
# Locate Java.
# ----------------------------------------------------------------------------

findMakeFriendlyEquivalentName.$(TARGET_OS) = $(1)
# make-friendly means forward slashes and no spaces.
# Quoted because the original may contain backslashes.
findMakeFriendlyEquivalentName.Cygwin = $(shell cygpath --mixed --short-name '$(1)')
findMakeFriendlyEquivalentName = $(findMakeFriendlyEquivalentName.$(TARGET_OS))

JDK_ROOT_SCRIPT = $(SCRIPT_PATH)/find-jdk-root.rb
SCRIPTS_WHICH_AFFECT_COMPILER_FLAGS += $(JDK_ROOT_SCRIPT)
JDK_ROOT := $(call findMakeFriendlyEquivalentName,$(shell ruby $(JDK_ROOT_SCRIPT)))

# ----------------------------------------------------------------------------
# We use our own replacement for javah(1).
# ----------------------------------------------------------------------------

JAVAHPP = $(SCRIPT_PATH)/javahpp.rb

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

# A rather unconvincing and error-laden post from a guy who works on the JVM
# suggests that the JVM may have trouble generating stack traces through native
# code without these flags:
# http://weblogs.java.net/blog/kellyohair/archive/2006/01/compilation_of.html
C_AND_CXX_FLAGS += -fno-omit-frame-pointer -fno-strict-aliasing

# Insist on position-independent code.
# We do nothing on Cygwin because its g++ fails with:
# warning: -fPIC ignored for target (all code is position independent)
PIC_FLAG.$(TARGET_OS) = -fPIC
PIC_FLAG.Cygwin =
C_AND_CXX_FLAGS += $(PIC_FLAG.$(TARGET_OS))

# Include debugging information.
C_AND_CXX_FLAGS += -g

# Maximum warnings...
C_AND_CXX_FLAGS += -W -Wall -Wshadow
# As long as people are using GCC 3, we can't turn this on because <string> is broken.
#C_AND_CXX_FLAGS += -Wunreachable-code
OBJC_AND_OBJCXX_FLAGS += -Wno-protocol -Wundeclared-selector
# ... but assume that C++ will eventually subsume C99.
CXXFLAGS += -Wno-long-long
PURE_C_AND_CXX_FLAGS += -pedantic

# Treat warnings as errors.
C_AND_CXX_FLAGS += -Werror

CPPFLAGS += $(foreach DIRECTORY,$(EXTRA_INCLUDE_PATH),-I$(DIRECTORY))

# Exposes ptsname on libc22 per the Linux man page but causes this on Mac OS X:
# /System/Library/Frameworks/CoreServices.framework/Frameworks/CarbonCore.framework/Headers/MacMemory.h:1587: error: 'bzero' was not declared in this scope
# The 600 figure is from /usr/include/features.h.
CPPFLAGS.Linux += -D_XOPEN_SOURCE=600
# strerror_r isn't POSIX.  On gooch, it requires this.
CPPFLAGS.Linux += -D_BSD_SOURCE
CPPFLAGS += $(CPPFLAGS.$(TARGET_OS))

C_AND_CXX_FLAGS += $(C_AND_CXX_FLAGS.$(TARGET_OS))
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
  OBJECTS.$(1) = $(patsubst $(SOURCE_DIRECTORY)/%.$(1),$(COMPILATION_DIRECTORY)/%.$(OBJECT_EXTENSION.$(1)),$(filter %.$(1),$(SOURCES)))
  # The Ruby script needs help if we're going to have variable variable names.
  LOCAL_VARIABLES += OBJECTS.$(1)
endef

# It's non-obvious but utterly essential for single threaded
# applications which dynamically load multi-threaded libraries
# to use this option - or runtime crashes await.
# The default allocator is *not* thread-safe, despite g++ -v's
# apparent claims to the contrary (which, I think, are actually
# just claiming the availability of a thread-safe allocator).
LDFLAGS.Linux += -pthread

# launcher needs this on Linux.
LDFLAGS.Linux += -ldl

# Linux utilities that use Xlib need this.
# Debian (unlike Ubuntu) doesn't have the X11 libraries on its default path.
# At the moment, Ubuntu's /usr/X11R6/lib64 is a link to /usr/X11R6/lib so
# this doesn't do any harm, though it may in future.
LDFLAGS.Linux += -L/usr/X11R6/lib
LDFLAGS.Linux += -lX11

LDFLAGS += $(LDFLAGS.$(TARGET_OS))

# ----------------------------------------------------------------------------
# Extra compiler and (mainly) linker flags for building JNI.
# ----------------------------------------------------------------------------

# Tradition has it this way.
SHARED_LIBRARY_PREFIX.$(TARGET_OS) = lib
SHARED_LIBRARY_EXTENSION.$(TARGET_OS) = so

JNI_LIBRARY_EXTENSION.$(TARGET_OS) = $(SHARED_LIBRARY_EXTENSION.$(TARGET_OS))

SHARED_LIBRARY_LDFLAGS.Darwin += -dynamiclib
JNI_LIBRARY_LDFLAGS.Darwin += -framework JavaVM
JNI_LIBRARY_EXTENSION.Darwin = jnilib
# http://developer.apple.com/documentation/Porting/Conceptual/PortingUnix/compiling/chapter_4_section_9.html
# "By default, the names of dynamic libraries in Mac OS X end in .dylib instead of .so."
SHARED_LIBRARY_EXTENSION.Darwin = dylib
# The default $(LD) doesn't know about -dynamiclib on Darwin.
# This doesn't hurt on Linux, indeed it generally saves having to specify nonsense like -lstdc++.
LD = $(CXX)

# Note that our Solaris build assumes GCC rather than Sun's compiler.
# GCC's -shared option, which we use on Linux, exists, but produces link
# errors. -G, as used in Sun's tutorial examples with their own compiler works.
EXTRA_INCLUDE_PATH.SunOS += $(JDK_ROOT)/include/solaris
SHARED_LIBRARY_LDFLAGS.SunOS += -G

EXTRA_INCLUDE_PATH.Linux += $(JDK_ROOT)/include/linux
SHARED_LIBRARY_LDFLAGS.Linux += -shared

EXTRA_INCLUDE_PATH.Cygwin += $(JDK_ROOT)/include/win32
SHARED_LIBRARY_LDFLAGS.Cygwin += -shared
# Do we want stdcall aliases for even non-JNI shared libraries?
SHARED_LIBRARY_LDFLAGS.Cygwin += -Wl,--add-stdcall-alias
SHARED_LIBRARY_LDFLAGS.Cygwin += -Wl,--enable-auto-image-base
SHARED_LIBRARY_PREFIX.Cygwin =
SHARED_LIBRARY_EXTENSION.Cygwin = dll

EXTRA_INCLUDE_PATH += $(JDK_ROOT)/include
EXTRA_INCLUDE_PATH += $(EXTRA_INCLUDE_PATH.$(TARGET_OS))

EXTANT_INCLUDE_DIRECTORIES := $(wildcard $(EXTRA_INCLUDE_PATH))
NON_EXISTENT_INCLUDE_DIRECTORIES = $(filter-out $(EXTANT_INCLUDE_DIRECTORIES),$(EXTRA_INCLUDE_PATH))
ifneq "$(NON_EXISTENT_INCLUDE_DIRECTORIES)" ""
  $(warning Could not find $(NON_EXISTENT_INCLUDE_DIRECTORIES) - perhaps the first java on your PATH isn't in a JDK)
endif

SHARED_LIBRARY_LDFLAGS = $(SHARED_LIBRARY_LDFLAGS.$(TARGET_OS))
SHARED_LIBRARY_PREFIX = $(SHARED_LIBRARY_PREFIX.$(TARGET_OS))
SHARED_LIBRARY_EXTENSION = $(SHARED_LIBRARY_EXTENSION.$(TARGET_OS))

JNI_LIBRARY_LDFLAGS = $(JNI_LIBRARY_LDFLAGS.$(TARGET_OS))
JNI_LIBRARY_EXTENSION = $(JNI_LIBRARY_EXTENSION.$(TARGET_OS))

# ----------------------------------------------------------------------------
# Extra flags to always build Universal Binaries on Mac OS.
# http://developer.apple.com/documentation/Porting/Conceptual/PortingUnix/compiling/chapter_4_section_3.html
# ----------------------------------------------------------------------------

universal_binary_flags = -isysroot /Developer/SDKs/MacOSX10.4u.sdk -arch ppc -arch i386
C_AND_CXX_FLAGS.Darwin += $(universal_binary_flags)
LDFLAGS.Darwin += $(universal_binary_flags)

# ----------------------------------------------------------------------------
# Work out what native code, if any, we need to build.
# ----------------------------------------------------------------------------

NATIVE_SOURCE_PATTERN = $(CURDIR)/native/$(OS)/*/*.$(EXTENSION)
NATIVE_SOURCE = $(foreach OS,all $(TARGET_OS),$(foreach EXTENSION,$(SOURCE_EXTENSIONS),$(NATIVE_SOURCE_PATTERN)))
SUBDIRS := $(sort $(patsubst %/,%,$(dir $(wildcard $(NATIVE_SOURCE)))))

# ----------------------------------------------------------------------------

PROJECT_ROOT = $(CURDIR)

PROJECT_DIRECTORY_BASE_NAME = $(notdir $(PROJECT_ROOT))
HUMAN_PROJECT_NAME ?= $(PROJECT_DIRECTORY_BASE_NAME)
MACHINE_PROJECT_NAME := $(shell ruby -e 'puts("$(HUMAN_PROJECT_NAME)".downcase())')

BIN_DIRECTORY = .generated/$(TARGET_DIRECTORY)/bin
LIB_DIRECTORY = .generated/$(TARGET_DIRECTORY)/lib

# Distributions end up under http://software.jessies.org/
DIST_SSH_USER_AND_HOST = software@jessies.org
# Different file types end up in different directories.
# Note the use of the prerequisite's extension rather than that of the target, which is always phony.
DIST_SUBDIRECTORY_FOR_PREREQUISITE = $(DIST_SUBDIRECTORY$(suffix $<))
DIST_DIRECTORY = /home/software/downloads/$(if $(DIST_SUBDIRECTORY_FOR_PREREQUISITE),$(DIST_SUBDIRECTORY_FOR_PREREQUISITE),$(error sub-directory not specified for extension "$(suffix $<)"))
# The html files are copied, with rsync, from www/ into this directory.
DIST_SUBDIRECTORY.html = $(MACHINE_PROJECT_NAME)
# The SOURCE_DIST ends up here - FIXME: it's binary, so you might want it somewhere else.
# $(suffix)'s definition means we need .gz here not .tar.gz.
DIST_SUBDIRECTORY.gz = $(MACHINE_PROJECT_NAME)
# Debian's mirrors are in a top-level directory called debian.
# I thought there might be some tool dependency on that.
DIST_SUBDIRECTORY.deb = debian
DIST_SUBDIRECTORY.dmg = mac
DIST_SUBDIRECTORY.msi = windows
DIST_SUBDIRECTORY.pkg = sunos
DIST_SUBDIRECTORY.rpm = redhat

$(takeProfileSample)
# Can we really imagine a project without src/?  I'm wondering whether the wildcard is necessary.
WILDCARD.src := $(wildcard src)
WILDCARD.classes := $(wildcard .generated/classes)
JAVA_SOURCE_FILES := $(if $(WILDCARD.src),$(shell find src -type f -name "*.java"))
JAVA_DIRECTORY_PREREQUISITES := $(if $(strip $(WILDCARD.src) $(WILDCARD.classes)),$(shell find $(WILDCARD.src) $(WILDCARD.classes) -name .svn -prune -o -type d -print))
# If classes/ has been deleted, depending on its parent directory should get us rebuilt.
JAVA_DIRECTORY_PREREQUISITES += $(if $(WILDCARD.classes),,.generated)
$(takeProfileSample)
SOURCE_DIST_FILE = $(MACHINE_PROJECT_NAME).tar.gz

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

GENERATED_FILES += ChangeLog
GENERATED_FILES += ChangeLog.html
GENERATED_FILES += .generated

MAKE_VERSION_FILE_COMMAND = ruby $(SCRIPT_PATH)/make-version-file.rb . $(SALMA_HAYEK)
# By immediately evaluating this, we cause install-everything.sh (or other building-from-source) to warn:
# svn: '.' is not a working copy
# Now we use the version string in the name of the .rpm target, it gets evaluated even if we use = instead of :=.
VERSION_STRING := $(shell $(MAKE_VERSION_FILE_COMMAND) | tail -1)
# If you ever need a Debian equivalent of this Windows-specific script:
# sudo apt-get install uuid
makeGuid = $(shell $(SCRIPT_PATH)/uuid.rb)

# ----------------------------------------------------------------------------
# Choose a Java compiler.
# ----------------------------------------------------------------------------

JAVA_COMPILER = $(JDK_ROOT)/bin/javac
ifeq "$(wildcard $(JAVA_COMPILER)$(EXE_SUFFIX))" ""
  JAVA_COMPILER := $(error Unable to find $(JAVA_COMPILER) --- do you only have a JRE installed?)
endif

# ----------------------------------------------------------------------------
# Set up the classpath.
# TODO: Consider whether we could defer to invoke-java.rb to run the compiler
# and so lose this duplication.
# ----------------------------------------------------------------------------
CLASS_PATH += $(SALMA_HAYEK)/.generated/classes
CLASS_PATH += $(wildcard $(SALMA_HAYEK)/lib/jars/*.jar)
CLASS_PATH += $(wildcard $(PROJECT_ROOT)/lib/jars/*.jar)

# "tools.jar" doesn't exist on Mac OS (the classes are automatically available).
# Java 6 will do likewise for the other platforms, at which point this can be removed.
TOOLS_JAR := $(wildcard $(JDK_ROOT)/lib/tools.jar)
CLASS_PATH += $(TOOLS_JAR)

JAVAC_FLAGS += $(addprefix -classpath ,$(call makeNativePath,$(CLASS_PATH)))

# ----------------------------------------------------------------------------
# Set default javac flags.
# ----------------------------------------------------------------------------

JAVAC_FLAGS += -d .generated/classes/
JAVAC_FLAGS += -sourcepath src/
JAVAC_FLAGS += -g

# Turn on warnings.
JAVAC_FLAGS += -deprecation
JAVAC_FLAGS += -Xlint:all -Xlint:-serial

# We should also ensure that we build class files that can be used on
# the current Java release, regardless of where we build.
JAVAC_FLAGS += -target 1.5

# Ensure we give a clear error if the user attempts to use anything older
# than Java 5.
JAVAC_FLAGS += -source 1.5

# javac(1) warns if you build source containing characters unrepresentable
# in your locale. Although we all use UTF-8 locales, we can't guarantee that
# everyone else does, so let the compiler know that our source is in UTF-8.
JAVAC_FLAGS += -encoding UTF-8

# It's not helpful to list all the Java source files.
define BUILD_JAVA
  @echo "Compiling Java source..."
  $(RM) -r classes && \
  $(RM) -r .generated/classes && \
  mkdir -p .generated/classes
  @echo '$(JAVA_COMPILER) $(JAVAC_FLAGS) $$(JAVA_SOURCE_FILES)'
  @$(JAVA_COMPILER) $(JAVAC_FLAGS) $(call convertToNativeFilenames,$(JAVA_SOURCE_FILES)) && \
  touch $@
endef

# ----------------------------------------------------------------------------
# Installer variables
# ----------------------------------------------------------------------------

# We get the shell to find candle and light on the path but we mention
# file-list-to-wxi in a prerequisite and so must know its exact location.
FILE_LIST_TO_WXI = $(SCRIPT_PATH)/file-list-to-wxi.rb

WIX_COMPILATION_DIRECTORY = .generated/WiX

makeInstallerName.msi = $(MACHINE_PROJECT_NAME)-$(1).msi
INSTALLER_EXTENSIONS += msi
INSTALLER_EXTENSIONS.Cygwin += msi

makeInstallerName.dmg = $(MACHINE_PROJECT_NAME)-$(1).dmg
INSTALLER_EXTENSIONS += dmg
INSTALLER_EXTENSIONS.Darwin += dmg

# Create different .pkg filenames for different target architectures so they can coexist.
makeInstallerName.pkg = SJO$(MACHINE_PROJECT_NAME)_$(1)_$(TARGET_ARCHITECTURE).pkg
INSTALLER_EXTENSIONS += pkg
INSTALLER_EXTENSIONS.SunOS += pkg

# Create different .deb filenames for different target architectures so they can coexist.
makeInstallerName.deb = org.jessies.$(MACHINE_PROJECT_NAME)_$(1)_$(TARGET_ARCHITECTURE).deb
INSTALLER_EXTENSIONS += deb
INSTALLER_EXTENSIONS.Linux += deb

# alien festoons the name with suffixes (and, as always, we have to let make know what it's going to generate).
# I looked at the source and it just has a big switch to select the output architecture.
# In particular, it doesn't do anything clever with the output of rpm --showrc.
makeInstallerName.rpm = org.jessies.$(MACHINE_PROJECT_NAME)-$(1)-2.$(patsubst amd64,x86_64,$(TARGET_ARCHITECTURE)).rpm
INSTALLER_EXTENSIONS += rpm
INSTALLER_EXTENSIONS.Linux += rpm

# When $< is "org.jessies.evergreen-4.31.1934-2.x86_64.rpm", we want "org.jessies.evergreen.x86_64.rpm".
# When $< is "org.jessies.evergreen_4.31.1934_amd64.deb", we want "org.jessies.evergreen.amd64.deb".
# When $< is "evergreen-4.31.1934.msi", we want "evergreen.msi".
# I wonder if we shouldn't say "latest" somewhere in the name.
# It would be easy to do that were it not for the odd "-2" part that alien adds to the name.
# When I upload, perhaps I should get rid of that.
LATEST_INSTALLER_LINK = $(subst --2.,.,$(subst __,.,$(subst -.,.,$(call makeInstallerName$(suffix $<),))))

define defineInstallerPath
  INSTALLER.$(1) = $$(BIN_DIRECTORY)/$$(call makeInstallerName.$(1),$$(VERSION_STRING))
endef
$(foreach extension,$(INSTALLER_EXTENSIONS),$(eval $(call defineInstallerPath,$(extension))))
INSTALLERS.$(TARGET_OS) = $(foreach extension,$(INSTALLER_EXTENSIONS.$(TARGET_OS)),$(INSTALLER.$(extension)))
INSTALLERS = $(INSTALLERS.$(TARGET_OS))

PUBLISHABLE_INSTALLERS.$(MACHINE_PROJECT_NAME) = $(INSTALLERS)
# Simplicity causes us to have rules for making installers in salma-hayek but we don't want to invoke them
# as prerequisites of the user-visible targets like native-dist.
# Hence the distinction between INSTALLERS and PUBLISHABLE_INSTALLERS.
PUBLISHABLE_INSTALLERS.salma-hayek =
PUBLISHABLE_INSTALLERS = $(PUBLISHABLE_INSTALLERS.$(MACHINE_PROJECT_NAME))

# Among its many breakages, msiexec is more restrictive about slashes than Win32.
NATIVE_NAME_FOR_INSTALLERS := '$(subst /,\,$(call convertToNativeFilenames,$(PUBLISHABLE_INSTALLERS)))'

# We copy the files we want to install into a directory tree whose layout mimics where they'll be installed.
PACKAGING_DIRECTORY = .generated/native/$(TARGET_DIRECTORY)/$(MACHINE_PROJECT_NAME)

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

BUILD_TARGETS += $(if $(JAVA_SOURCE_FILES),.generated/java.sentinel)
BUILD_TARGETS += $(if $(wildcard .svn),.generated/build-revision.txt)
TIC_SOURCE := $(wildcard lib/terminfo/*.tic)
# We deliberately omit the intermediate directory.
COMPILED_TERMINFO = $(patsubst lib/terminfo/%.tic,.generated/terminfo/%,$(TIC_SOURCE))
BUILD_TARGETS += $(COMPILED_TERMINFO)

# ----------------------------------------------------------------------------
# Variables above this point, rules below.
# Variables used on either side of the colon in rules are evaluated on the first pass,
# so you can't override them after the rule has been seen, even if they don't appear
# in any overt immediate evaluations (like := assignments).
# ----------------------------------------------------------------------------

.PHONY: build
build: $(BUILD_TARGETS)

.generated/java.sentinel: $(MAKEFILE_LIST) $(JAVA_SOURCE_FILES) $(JAVA_DIRECTORY_PREREQUISITES)
	$(BUILD_JAVA)

.PHONY: clean
clean:
	$(RM) -r $(GENERATED_FILES) && \
	find . -name "*.bak" | xargs $(RM)

.PHONY: native-clean
native-clean:
	$(RM) -r .generated/native

ChangeLog.html: ChangeLog
	$(RM) $@ && \
	ruby $(SCRIPT_PATH)/svn-log-to-html.rb < $< > $@

.PHONY: ChangeLog
ChangeLog:
	$(RM) $@ && \
	$(GENERATE_CHANGE_LOG.$(REVISION_CONTROL_SYSTEM))

# This is only designed to be run on jessies.org itself.
# It's run by a custom post-commit hook to generate a new source download for each revision.
.PHONY: source-dist
source-dist: ../$(SOURCE_DIST_FILE)
	mkdir -p $(DIST_DIRECTORY) && \
	mv $< $(DIST_DIRECTORY)/

# Including a generated file in a source distribution?
# The ChangeLog is generated too!
../$(SOURCE_DIST_FILE): ChangeLog .generated/build-revision.txt
	cd .. && \
	tar -X $(SALMA_HAYEK)/dist-exclude -zcf $(SOURCE_DIST_FILE) $(PROJECT_DIRECTORY_BASE_NAME)/* $(PROJECT_DIRECTORY_BASE_NAME)/.generated/build-revision.txt

# This is only designed to be run on jessies.org itself.
.PHONY: www-dist
www-dist: ChangeLog.html
	mkdir -p $(DIST_DIRECTORY) && \
	mv ChangeLog.html $(DIST_DIRECTORY)/ && \
	if [ -d www/ ] ; then rsync -v -r www/* $(DIST_DIRECTORY)/ ; fi

.PHONY: .generated/build-revision.txt
.generated/build-revision.txt:
	mkdir -p $(@D) && \
	$(MAKE_VERSION_FILE_COMMAND) > $@

# Old versions of SunOS tic don't support the -o argument but do support redirecting
# the output to $TERMINFO.
# I'd like to use -v10 but am stymied by http://bugs.debian.org/cgi-bin/bugreport.cgi?bug=365120
# With the cp, we both avoid having to cope with Mac OS's broken ncurses library,
# which thinks that terminator's terminfo belongs in 74/ rather than t/,
# and avoid having to work out the first letter of the terminal name.
.generated/terminfo/%: lib/terminfo/%.tic
	mkdir -p $(@D) && \
	TERMINFO=$(@D) tic -v1 $< && \
	cp $(@D)/*/$(@F) $@

# ----------------------------------------------------------------------------
# How to build a .app directory and package it into an installer file.
# ----------------------------------------------------------------------------

# The output of this make invocation is captured and processed, so we mustn't echo it.
.PHONY: installer-file-list
installer-file-list:
	@$(MAKE_INSTALLER_FILE_LIST)

# Unfortunately, the start-up scripts tend to go looking for salma-hayek, so we can't just have Resources/bin etc; we have to keep the multi-directory structure, at least for now.
.PHONY: $(MACHINE_PROJECT_NAME).app
$(MACHINE_PROJECT_NAME).app: build .generated/build-revision.txt
	$(SCRIPT_PATH)/package-for-distribution.rb $(HUMAN_PROJECT_NAME) $(MACHINE_PROJECT_NAME) $(SALMA_HAYEK)

$(INSTALLER.dmg): $(MACHINE_PROJECT_NAME).app
	@echo "Creating Mac OS .dmg disk image..."
	mkdir -p $(@D) && \
	$(RM) $@ && \
	hdiutil create -fs UFS -volname $(HUMAN_PROJECT_NAME) -srcfolder $(PACKAGING_DIRECTORY) $@

$(INSTALLER.pkg): $(MACHINE_PROJECT_NAME).app
	@echo "Creating package stream..."
	mkdir -p $(@D) && \
	$(RM) $@  && \
	pkgmk -o -d $(@D) -f $(PACKAGING_DIRECTORY)/prototype -r $(PACKAGING_DIRECTORY)/root/ && \
	pkgtrans -s $(@D) $(@F) SJO$(MACHINE_PROJECT_NAME) && \
	pkginfo -l -d $@

$(INSTALLER.deb): $(MACHINE_PROJECT_NAME).app
	@echo "Creating GNU/Linux .deb package..."
	mkdir -p $(@D) && \
	$(RM) $@ && \
	fakeroot dpkg-deb --build $(PACKAGING_DIRECTORY) $@ && \
	dpkg-deb --info $@ # && dpkg-deb --contents $@

$(INSTALLER.rpm): $(INSTALLER.deb)
	@echo "Creating GNU/Linux .rpm package..."
	mkdir -p $(@D) && \
	$(RM) $@ && \
	cd $(@D) && \
	fakeroot alien --to-rpm $(CURDIR)/$<

# ----------------------------------------------------------------------------
# WiX
# ----------------------------------------------------------------------------

# Later we add more dependencies when we know $(ALL_PER_DIRECTORY_TARGETS).
%/component-definitions.wxi: $(MAKEFILE_LIST) $(FILE_LIST_TO_WXI) $(MACHINE_PROJECT_NAME).app
	mkdir -p $(@D) && \
	( cd $(PACKAGING_DIRECTORY) && find . -type f -print ) | cut -c3- | $(FILE_LIST_TO_WXI) > $@

# This silliness is probably sufficient (as well as sadly necessary).
%/component-references.wxi: %/component-definitions.wxi $(MAKEFILE_LIST)
	mkdir -p $(@D) && \
	ruby -w -ne '$$_.match(/Include/) && puts($$_); $$_.match(/<Component (Id='\''component\d+'\'')/) && puts("<ComponentRef #{$$1} />")' < $< > $@

%.wixobj: %.wxs $(patsubst %,$(WIX_COMPILATION_DIRECTORY)/component-%.wxi,references definitions)
	@echo Compiling $(notdir $<)...
	HUMAN_PROJECT_NAME=$(HUMAN_PROJECT_NAME) \
	MACHINE_PROJECT_NAME=$(MACHINE_PROJECT_NAME) \
	PATH_GUID=$(makeGuid) \
	PRODUCT_GUID=$(makeGuid) \
	SHORTCUT_HKCU_GUID=$(makeGuid) \
	SHORTCUT_HKLM_GUID=$(makeGuid) \
	STANDARD_FILES_GUID=$(makeGuid) \
	UPGRADE_GUID=$(UPGRADE_GUID) \
	VERSION_STRING=$(VERSION_STRING) \
	candle -nologo -out $(call convertToNativeFilenames,$@ $<)

$(INSTALLER.msi): $(WIX_COMPILATION_DIRECTORY)/$(MACHINE_PROJECT_NAME).wixobj $(BUILD_TARGETS)
	@echo Creating Windows installer...
	cd $(PACKAGING_DIRECTORY) && \
	light -nologo -out $(call convertToNativeFilenames,$(CURDIR)/$@ $(CURDIR)/$<)

$(WIX_COMPILATION_DIRECTORY)/$(MACHINE_PROJECT_NAME).wxs: $(SALMA_HAYEK)/lib/installer.wxs
	$(COPY_RULE)

# ----------------------------------------------------------------------------
# Rules for debugging.
# ----------------------------------------------------------------------------

.PHONY: echo.%
echo.%:
	@echo '"$($*)"'

# ----------------------------------------------------------------------------
# Rules for making makefiles.
# ----------------------------------------------------------------------------

# The $$1 here is a Ruby variable, not a make one.
.generated/local-variables.make: $(SALMA_HAYEK)/per-directory.make $(SALMA_HAYEK)/universal.make
	mkdir -p $(@D) && \
	ruby -w -ne '($$_.match(/^ *(\S+)\s*[:+]?=/) || $$_.match(/^\s*define\s*(\S+)/)) && puts("LOCAL_VARIABLES += #{$$1}")' $< | sort -u > $@

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
	mkdir -p $(@D) && \
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

# Defined here because this can only be correctly evaluated after we've included per-directory.make...
ALL_PER_DIRECTORY_TARGETS = $(foreach SUBDIR,$(SUBDIRS),$(DESIRED_TARGETS.$(notdir $(SUBDIR))))

# ... and this depends on the above variable.
FILES_TO_INSTALL += $(ALL_PER_DIRECTORY_TARGETS)
FILES_TO_INSTALL += .generated/build-revision.txt
FILES_TO_INSTALL += $(COMPILED_TERMINFO)
SUBDIRECTORIES_TO_INSTALL += bin
SUBDIRECTORIES_TO_INSTALL += doc
SUBDIRECTORIES_TO_INSTALL += lib
define MAKE_INSTALLER_FILE_LIST
  { \
    $(foreach file,$(FILES_TO_INSTALL),echo $(file) &&) \
    find $(wildcard $(SUBDIRECTORIES_TO_INSTALL)) -name .svn -prune -o -type f -print; \
  } | ruby -ne 'chomp!(); puts("Including #{$$_}...")'
endef

# The installer uses find(1) to discover what to include - so it must be built last.
$(WIX_COMPILATION_DIRECTORY)/component-definitions.wxi: $(ALL_PER_DIRECTORY_TARGETS)

# Presumably we need a similar dependency for non-WiX installers, which need the per-directory targets slightly later but still before the installer.
$(MACHINE_PROJECT_NAME).app: $(ALL_PER_DIRECTORY_TARGETS)

.PHONY: native
native: $(ALL_PER_DIRECTORY_TARGETS)
build: native

.PHONY: installer
installer: $(PUBLISHABLE_INSTALLERS)

.PHONY: native-dist
native-dist: $(addprefix symlink-latest.,$(PUBLISHABLE_INSTALLERS))

# We still need the default salma-hayek build during the nightly build.
native-dist: build

# I'm deliberately downloading the previous version of the installer and overwriting the version you've just built.
# This is so that, when we regenerate the Packages file, we don't change md5sums.
# If you want to upload a new installer, you're going to have to check-in first (or change this rule or manually delete the old one).
.PHONY: upload.%
$(addprefix upload.,$(PUBLISHABLE_INSTALLERS)): upload.%: %
	@echo Uploading $(<F)...
	ssh $(DIST_SSH_USER_AND_HOST) mkdir -p $(DIST_DIRECTORY) && \
	if scp $(DIST_SSH_USER_AND_HOST):$(DIST_DIRECTORY)/$(<F) $<; \
	then \
		echo Overwriting the local $(<F) with the copy of that version from the server - it should not be overwritten once its md5sums are in a Debian Packages file...; \
	else \
		scp $< $(DIST_SSH_USER_AND_HOST):$(DIST_DIRECTORY)/$(<F); \
	fi

# I like the idea of keeping several versions on the server but we're going to have a hard time
# linking to the one we expect people to use unless we create a symlink.
$(addprefix symlink-latest.,$(PUBLISHABLE_INSTALLERS)): symlink-latest.%: upload.%
.PHONY: symlink-latest.%
$(addprefix symlink-latest.,$(PUBLISHABLE_INSTALLERS)): symlink-latest.%: %
	@echo Symlinking the latest $(LATEST_INSTALLER_LINK)...
	ssh $(DIST_SSH_USER_AND_HOST) $(RM) $(DIST_DIRECTORY)/$(LATEST_INSTALLER_LINK) '&&' \
	ln -s $(<F) $(DIST_DIRECTORY)/$(LATEST_INSTALLER_LINK) '&&' \
	find $(DIST_DIRECTORY) -name '"$(call makeInstallerName$(suffix $<),*)"' -mtime +7 '|' xargs $(RM)

.PHONY: install
install: $(addprefix run-installer,$(suffix $(PUBLISHABLE_INSTALLERS)))

$(addprefix run-installer,$(suffix $(PUBLISHABLE_INSTALLERS))): $(PUBLISHABLE_INSTALLERS)

.PHONY: remove
remove: $(addprefix run-remover,$(suffix $(PUBLISHABLE_INSTALLERS)))

.PHONY: run-installer.pkg
run-installer.pkg:
	yes | sudo /usr/sbin/pkgadd -G -d $(INSTALLER.pkg) all

.PHONY: run-installer.deb
run-installer.deb:
	sudo dpkg -i $(INSTALLER.deb)

.PHONY: run-installer.rpm
run-installer.rpm:
	@echo Installing the .rpm might be possible with sudo rpm -i $(INSTALLER.rpm) but that would be a bad idea on a Debian box...

.PHONY: run-installer.dmg
run-installer.dmg:
	open -a DiskImageMounter $(INSTALLER.dmg)

.PHONY: run-installer.msi
run-installer.msi:
	msiexec /i $(NATIVE_NAME_FOR_INSTALLERS)

# This only works if this is precisely the same version that was installed.
# We can't uninstall by GUID because the GUID you give to the uninstaller is
# one of the GUIDs that we have to change each time we build in order for Windows
# to realize that we're building a replacement version.
# We could search the Registry for the installed GUID but it's indexed by GUID
# rather than name.
.PHONY: run-remover.msi
run-remover.msi:
	msiexec /x $(NATIVE_NAME_FOR_INSTALLERS)
