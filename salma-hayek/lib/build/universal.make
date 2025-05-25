# ----------------------------------------------------------------------------
# Disable legacy make behavior.
# ----------------------------------------------------------------------------

# We used to disable suffix rules, but the default compilation rules are suffix
# rules, and we want to use them in per-directory.make.
#.SUFFIXES:

.DEFAULT:
.DELETE_ON_ERROR:

# ----------------------------------------------------------------------------
# Define useful stuff not provided by GNU make.
# ----------------------------------------------------------------------------

COMMA = ,
SPACE = $(subst :, ,:)
define NEWLINE_WITH_DELIMITERS
:
:
endef
NEWLINE = $(subst :,,$(NEWLINE_WITH_DELIMITERS))

# I sprinkled the code with calls to dump the wall-clock time and counted the
# lines of output to work out between which calls the time was disappearing.
# I moved the calls around to isolate the particularly expensive lines.
# When I'd isolated things which I couldn't easily improve,
# I left one copy either side of each line - so you can get some idea of the time
# taken by the line.
# Yes, it's crude but, when faced with a build time of ~30s, it was adequate.
#takeProfileSample = $(eval $(shell date --iso=s 1>&2))
takeProfileSample =

export MAKE

# ----------------------------------------------------------------------------
# Locate salma-hayek.
# ----------------------------------------------------------------------------

# $(lastword) isn't available in 3.80.
MOST_RECENT_MAKEFILE = $(word $(words $(MAKEFILE_LIST)),$(MAKEFILE_LIST))
# The location of this makefile shouldn't change with later includes.
UNIVERSAL_MAKEFILE := $(MOST_RECENT_MAKEFILE)
# $(dir $(dir)) doesn't do what you want.
dirWithoutSlash = $(patsubst %/,%,$(dir $(1)))
MAKEFILE_DIRECTORY = $(call dirWithoutSlash,$(UNIVERSAL_MAKEFILE))
ABSOLUTE_MAKEFILE_DIRECTORY = $(patsubst ../%,$(dir $(CURDIR))%,$(MAKEFILE_DIRECTORY))
SALMA_HAYEK = $(call dirWithoutSlash,$(call dirWithoutSlash,$(ABSOLUTE_MAKEFILE_DIRECTORY)))

# ----------------------------------------------------------------------------
# Work out what we're going to generate.
# ----------------------------------------------------------------------------

SCRIPT_PATH = $(SALMA_HAYEK)/bin
BUILD_SCRIPT_PATH = $(SALMA_HAYEK)/lib/build

TARGET_OS_SCRIPT = $(SCRIPT_PATH)/target-os.rb
SCRIPTS_WHICH_AFFECT_COMPILER_FLAGS += $(TARGET_OS_SCRIPT)
TARGET_OS_SCRIPT_OUTPUT := $(shell ruby $(TARGET_OS_SCRIPT))
TARGET_OS = $(word 1,$(TARGET_OS_SCRIPT_OUTPUT))
# (TARGET_ARCH has a special meaning to the built-in compilation rules.)
TARGET_ARCHITECTURE = $(word 2,$(TARGET_OS_SCRIPT_OUTPUT))
TARGET_DIRECTORY = $(word 3,$(TARGET_OS_SCRIPT_OUTPUT))
HOST_OS_SCRIPT_OUTPUT := $(shell unset TARGET_ARCHITECTURE; ruby $(TARGET_OS_SCRIPT))
HOST_ARCHITECTURE = $(word 2,$(HOST_OS_SCRIPT_OUTPUT))

ifneq "$(REQUIRED_MAKE_VERSION)" "$(EARLIER_MAKE_VERSION)"
    ifeq "$(TARGET_OS)" "Cygwin"
        $(warning The make which comes with Cygwin 1.5.18-1 isn't good enough.)
        $(warning Try http://software.jessies.org/3rdParty/make-3.81-cygwin instead.)
    endif
    ifeq "$(TARGET_OS)" "Darwin"
        $(warning Try our pre-built http://software.jessies.org/3rdParty/make-3.81-darwin-universal instead.)
    endif
    ifeq "$(TARGET_OS)" "Linux"
        $(warning Debian Etch has a new enough make if you do sudo apt-get install make.)
        $(warning Or try http://software.jessies.org/3rdParty/make-3.81-linux.)
    endif
    ifeq "$(TARGET_OS)" "SunOS"
        $(warning Try installing GNU make from Blastwave, with pkg-get -i gmake.)
    endif
# The blank line separates any duplicate warning, which 3.80 seems fond of generating.
    $(warning )
endif

# ----------------------------------------------------------------------------
# Abstractions to help with platform-specific file system differences.
# ----------------------------------------------------------------------------

EXE_SUFFIX.Cygwin = .exe
EXE_SUFFIX = $(EXE_SUFFIX.$(TARGET_OS))

NATIVE_PATH_SEPARATOR.$(TARGET_OS) = :
# Quoted to get through bash without being treated as a statement terminator.
NATIVE_PATH_SEPARATOR.Cygwin = ";"
NATIVE_PATH_SEPARATOR = $(NATIVE_PATH_SEPARATOR.$(TARGET_OS))

convertToNativeFilename.$(TARGET_OS) = $(1)
# javac is happy with forward slashes (as is the underlying Windows API).
convertToNativeFilename.Cygwin = $(shell cygpath --mixed $(1))
convertToNativeFilenames = $(foreach file,$(1),'$(call convertToNativeFilename.$(TARGET_OS),$(file))')

searchPath = $(shell which $(1) 2> /dev/null)
makeNativePath = $(subst ' ','$(NATIVE_PATH_SEPARATOR)',$(call convertToNativeFilenames,$(1)))

define SYMLINK_RULE
	mkdir -p $(@D) && \
	$(RM) $@ && \
	ln -s $< $@
endef

define MOVE_GENERATED_TARGET_INTO_PLACE
	chmod a-w $@.tmp && \
	$(RM) $@ && \
	mv $@.tmp $@
endef

# Use cp for the benefit of Windows native compilers which don't
# understand "symlinks".
define COPY_RULE
	mkdir -p $(@D) && \
	cp $< $@.tmp && \
	$(MOVE_GENERATED_TARGET_INTO_PLACE)
endef

# 2017-01-09 gcc-5.4.0's preprocessor can't find the JNI headers with Cygwin 2.6.1.
# Something's mishandling a symlink from a directory some eight levels deep.
COALESCE_RULE.$(TARGET_OS) = $(SYMLINK_RULE)
COALESCE_RULE.Cygwin = $(COPY_RULE)
COALESCE_RULE = $(COALESCE_RULE.$(TARGET_OS))

# ----------------------------------------------------------------------------
# Locate Java.
# ----------------------------------------------------------------------------

findMakeFriendlyEquivalentName.$(TARGET_OS) = $(1)
# make-friendly means forward slashes and no spaces.
# Quoted because the original may contain backslashes.
findMakeFriendlyEquivalentName.Cygwin = $(if $(1),$(shell cygpath --mixed --short-name '$(1)'))
findMakeFriendlyEquivalentName = $(findMakeFriendlyEquivalentName.$(TARGET_OS))

JDK_ROOT_SCRIPT = $(SCRIPT_PATH)/find-jdk-root.rb
SCRIPTS_WHICH_AFFECT_COMPILER_FLAGS += $(JDK_ROOT_SCRIPT)
JDK_ROOT := $(call findMakeFriendlyEquivalentName,$(shell ruby $(JDK_ROOT_SCRIPT)))

JDK_INCLUDE = $(JDK_ROOT)/include
JDK_BIN = $(JDK_ROOT)/bin

# ----------------------------------------------------------------------------
# We use our own replacement for javah(1).
# ----------------------------------------------------------------------------

JAVAHPP = $(SCRIPT_PATH)/javahpp

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

NATIVE_OS_DIRECTORIES += all
NATIVE_OS_DIRECTORIES += $(TARGET_OS)
NATIVE_OS_DIRECTORIES += $(NATIVE_OS_DIRECTORIES.$(TARGET_OS))

# ----------------------------------------------------------------------------
# Sensible C family compiler flags.
# ----------------------------------------------------------------------------

EXTRA_INCLUDE_PATH += $(SALMA_HAYEK)/native/Headers

CFLAGS += -std=c99
CXXFLAGS += -std=c++11

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
DEBUG_FLAG.$(TARGET_OS) = -g
DEBUG_FLAG.Cygwin = -gdwarf-2
C_AND_CXX_FLAGS += $(DEBUG_FLAG.$(TARGET_OS))

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
CPPFLAGS.Cygwin += -D_XOPEN_SOURCE=600
# strerror_r isn't POSIX until _POSIX_C_SOURCE >= 200112L.  On gooch, it requires this...
CPPFLAGS.Linux += -D_BSD_SOURCE
# ... but on libc6-dev 2.22, that warns unless we also define this:
CPPFLAGS.Linux += -D_DEFAULT_SOURCE
CPPFLAGS += $(CPPFLAGS.$(TARGET_OS))

# stdint.h tells us that:
# The ISO C99 standard specifies that in C++ implementations these
# macros should only be defined if explicitly requested.
CXXFLAGS += -D__STDC_LIMIT_MACROS

C_AND_CXX_FLAGS += $(C_AND_CXX_FLAGS.$(TARGET_OS))
CFLAGS += $(C_AND_CXX_FLAGS)
CXXFLAGS += $(C_AND_CXX_FLAGS)

C_AND_CXX_FLAGS.i386-on-amd64 += -m32

C_AND_CXX_FLAGS += $(C_AND_CXX_FLAGS.$(TARGET_ARCHITECTURE)-on-$(HOST_ARCHITECTURE))

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

# java-launcher needs this on Linux.
LDFLAGS.Linux += -ldl

# Linux utilities that use Xlib need this.
# Debian (unlike Ubuntu) doesn't have the X11 libraries on its default path.
# At the moment, Ubuntu's /usr/X11R6/lib64 is a link to /usr/X11R6/lib so
# this doesn't do any harm, though it may in future.
LDFLAGS.Linux += -L/usr/X11R6/lib
LDFLAGS.Linux += -lX11

# Debian sid's prerelease g++ -V4.3 isn't linking this by default.
# This seems unlikely to be harmful elsewhere.
LDFLAGS.Linux += -lstdc++

# Prevent desktop shortcuts from spewing forth console windows.
LDFLAGS.Cygwin += -Wl,--subsystem,windows

# Cygwin 1.7's g++-4 generates a java-launcher and ruby-launcher which crash
# on startup unless we give it this option.
LDFLAGS.Cygwin += -Wl,--enable-auto-import

LDFLAGS += $(LDFLAGS.$(TARGET_OS))

LDFLAGS.i386-on-amd64 += -m32

LDFLAGS += $(LDFLAGS.$(TARGET_ARCHITECTURE)-on-$(HOST_ARCHITECTURE))

# ----------------------------------------------------------------------------
# Extra compiler and (mainly) linker flags for building JNI.
# ----------------------------------------------------------------------------

# Tradition has it this way.
SHARED_LIBRARY_PREFIX.$(TARGET_OS) = lib

SHARED_LIBRARY_EXTENSION.$(TARGET_OS) = so
# http://developer.apple.com/documentation/Porting/Conceptual/PortingUnix/compiling/chapter_4_section_9.html
# "By default, the names of dynamic libraries in Mac OS X end in .dylib instead of .so."
SHARED_LIBRARY_EXTENSION.Darwin = dylib

LEGACY_JNI_LIBRARY_EXTENSION.$(TARGET_OS) = $(SHARED_LIBRARY_EXTENSION.$(TARGET_OS))
LEGACY_JNI_LIBRARY_EXTENSION.Darwin = jnilib
LEGACY_JNI_LIBRARY_EXTENSION = $(LEGACY_JNI_LIBRARY_EXTENSION.$(TARGET_OS))

SHARED_LIBRARY_LDFLAGS.Darwin += -dynamiclib
# The default $(LD) doesn't know about -dynamiclib on Darwin.
# This doesn't hurt on Linux, indeed it generally saves having to specify nonsense like -lstdc++.
LD = $(CXX)

EXTRA_INCLUDE_PATH.Darwin += $(JDK_INCLUDE)/darwin

# Note that our Solaris build assumes GCC rather than Sun's compiler.
# GCC's -shared option, which we use on Linux, exists, but produces link
# errors. -G, as used in Sun's tutorial examples with their own compiler works.
EXTRA_INCLUDE_PATH.SunOS += $(JDK_INCLUDE)/solaris
SHARED_LIBRARY_LDFLAGS.SunOS += -G

EXTRA_INCLUDE_PATH.FreeBSD += $(JDK_INCLUDE)/freebsd
SHARED_LIBRARY_LDFLAGS.FreeBSD += -shared

EXTRA_INCLUDE_PATH.Linux += $(JDK_INCLUDE)/linux
SHARED_LIBRARY_LDFLAGS.Linux += -shared

EXTRA_INCLUDE_PATH.Cygwin += $(JDK_INCLUDE)/win32
SHARED_LIBRARY_LDFLAGS.Cygwin += -shared
# Do we want stdcall aliases for even non-JNI shared libraries?
SHARED_LIBRARY_LDFLAGS.Cygwin += -Wl,--add-stdcall-alias
SHARED_LIBRARY_LDFLAGS.Cygwin += -Wl,--enable-auto-image-base
SHARED_LIBRARY_PREFIX.Cygwin =
SHARED_LIBRARY_EXTENSION.Cygwin = dll

EXTRA_INCLUDE_PATH += $(JDK_INCLUDE)
EXTRA_INCLUDE_PATH += $(EXTRA_INCLUDE_PATH.$(TARGET_OS))

EXTANT_INCLUDE_DIRECTORIES := $(wildcard $(EXTRA_INCLUDE_PATH))
NON_EXISTENT_INCLUDE_DIRECTORIES = $(filter-out $(EXTANT_INCLUDE_DIRECTORIES),$(EXTRA_INCLUDE_PATH))
ifneq "$(NON_EXISTENT_INCLUDE_DIRECTORIES)" ""
  $(warning Could not find $(NON_EXISTENT_INCLUDE_DIRECTORIES) - perhaps the first java on your PATH isn't in a JDK)
endif

SHARED_LIBRARY_LDFLAGS = $(SHARED_LIBRARY_LDFLAGS.$(TARGET_OS))
SHARED_LIBRARY_PREFIX = $(SHARED_LIBRARY_PREFIX.$(TARGET_OS))
SHARED_LIBRARY_EXTENSION = $(SHARED_LIBRARY_EXTENSION.$(TARGET_OS))

# ----------------------------------------------------------------------------
# Extra flags for macOS.
# ----------------------------------------------------------------------------

# The only reason for the choice of 10.9 is that on macOS 10.14 I was told that
# I shouldn't be using libc++ without setting that as my minimum, and that not
# using libc++ is deprecated. In 2018, no one is still using macOS 10.14.
C_AND_CXX_FLAGS.Darwin += -mmacosx-version-min=10.9
C_AND_CXX_FLAGS.Darwin += -stdlib=libc++
# We should probably rewrite the code to use newer APIs, but I'm just here to
# get macOS compiling again, so I'm punting this until someone tells us that
# the deprecated stuff has actually been removed.
C_AND_CXX_FLAGS.Darwin += -Wno-error=deprecated-declarations
LDFLAGS.Darwin += -lobjc
LDFLAGS.Darwin += -framework Cocoa

# ----------------------------------------------------------------------------
# Extra compiler flags for building for Windows without Cygwin.
# ----------------------------------------------------------------------------

# The orthography at mingw.org is MinGW but here I follow, well, mainly the other directories
# and my pronunciation but also http://www.delorie.com/howto/cygwin/mno-cygwin-howto.html.
NATIVE_OS_DIRECTORIES.Cygwin += Mingw
COMPILING_MINGW = $(filter $(CURDIR)/.generated/native/Mingw/%,$<)
# This page was invaluable when I wanted to downgrade from i686-pc-mingw32-g++ from Cygwin-1.7.9 to g++-3 in Cygwin-1.7.7:
# http://cygwin.com/ml/cygwin-announce/2011-04/msg00015.html
MINGW_FLAGS.g++-3 += -mno-cygwin
# With the newly packaged mingw compiler for Cygwin, cygwin-launcher failed to start
# due to the absence of either libgcc_s_dw2-1.dll or libstdc++-6.dll.
# http://stackoverflow.com/questions/4702732/the-program-cant-start-because-libgcc-s-dw2-1-dll-is-missing
MODERN_MINGW_FLAGS += -static-libgcc -static-libstdc++
MINGW_FLAGS.i686-pc-mingw32-g++ += $(MODERN_MINGW_FLAGS)
MINGW_FLAGS.i686-w64-mingw32-g++ += $(MODERN_MINGW_FLAGS)
MINGW_FLAGS.x86_64-w64-mingw32-g++ += $(MODERN_MINGW_FLAGS)
C_AND_CXX_FLAGS.Cygwin += $(if $(COMPILING_MINGW),$(MINGW_FLAGS.$(MINGW_COMPILER)))

# Facilitate overriding for CXX that's conditional on a per-target, per-directory basis.
DEFAULT_CXX := $(CXX)
CXX.$(TARGET_OS) = $(DEFAULT_CXX)
CXX = $(CXX.$(TARGET_OS))

# Cygwin 1.7 has a g++-4 which can be installed as the default compiler.
# Its compiler driver has no -mno-cygwin option.
MINGW_COMPILER.i386 = g++-3
# The newly packaged cross-compiler isn't available in 1.7.7.
# We had trouble forking after loading the JVM in versions between there and 1.7.18.
MINGW_COMPILER.i386 = i686-pc-mingw32-g++
# The above compiler is gcc-4.7.3 in Cygwin at the time of writing.
# Whereas the below one is gcc-5.4.0.
# So MinGW-w64 seems to be the future, even for 32 bit builds.
MINGW_COMPILER.i386 = i686-w64-mingw32-g++
MINGW_COMPILER.amd64 = x86_64-w64-mingw32-g++
MINGW_COMPILER = $(MINGW_COMPILER.$(TARGET_ARCHITECTURE))
CXX.Cygwin = $(if $(COMPILING_MINGW),$(MINGW_COMPILER),$(DEFAULT_CXX))

CXX.Darwin = $(DEFAULT_CXX)

HAVE_MINGW_SOURCE := $(wildcard $(CURDIR)/native/Mingw)
CRT_SHARED_LIBRARIES.Cygwin += $(if $(HAVE_MINGW_SOURCE),.generated/$(TARGET_DIRECTORY)/bin/libwinpthread-1.dll)
CRT_SHARED_LIBRARIES.Cygwin += $(if $(HAVE_MINGW_SOURCE),.generated/$(TARGET_DIRECTORY)/bin/winpthreads.COPYING.txt)
CRT_SHARED_LIBRARIES += $(CRT_SHARED_LIBRARIES.$(TARGET_DIRECTORY))
CRT_SHARED_LIBRARIES += $(CRT_SHARED_LIBRARIES.$(TARGET_OS))

# ----------------------------------------------------------------------------
# Work out what native code, if any, we need to build.
# ----------------------------------------------------------------------------

NATIVE_SOURCE_PATTERN = $(CURDIR)/native/$(OS)/*/*.$(EXTENSION)
NATIVE_SOURCE = $(foreach OS,$(NATIVE_OS_DIRECTORIES),$(foreach EXTENSION,$(SOURCE_EXTENSIONS),$(NATIVE_SOURCE_PATTERN)))
SUBDIRS := $(sort $(patsubst %/,%,$(dir $(wildcard $(NATIVE_SOURCE)))))

# ----------------------------------------------------------------------------

PROJECT_ROOT = $(CURDIR)

export PROJECT_ROOT

DISTINCT_PROJECT_ROOTS = $(sort $(PROJECT_ROOT) $(SALMA_HAYEK))

PROJECT_DIRECTORY_BASE_NAME = $(notdir $(PROJECT_ROOT))
HUMAN_PROJECT_NAME ?= $(PROJECT_DIRECTORY_BASE_NAME)
MACHINE_PROJECT_NAME := $(shell ruby -e 'puts("$(HUMAN_PROJECT_NAME)".downcase())')

# I want to be able to use $(SALMA_HAYEK)/.generated/$(TARGET_DIRECTORY)/bin/... in dependencies.
BIN_DIRECTORY = $(PROJECT_ROOT)/.generated/$(TARGET_DIRECTORY)/bin
LIB_DIRECTORY = $(PROJECT_ROOT)/.generated/$(TARGET_DIRECTORY)/lib

REVISION_CONTROL_SYSTEM_DIRECTORIES += ../.git
REVISION_CONTROL_SYSTEM_DIRECTORIES += .git
REVISION_CONTROL_SYSTEM_DIRECTORIES += .hg
REVISION_CONTROL_SYSTEM_DIRECTORIES += .svn
REVISION_CONTROL_SYSTEM_DIRECTORIES += CVS
REVISION_CONTROL_SYSTEM_DIRECTORIES += SCCS

REVISION_CONTROL_SYSTEM_../.git = git
REVISION_CONTROL_SYSTEM_.git = git
REVISION_CONTROL_SYSTEM_.hg = hg
REVISION_CONTROL_SYSTEM_.svn = svn
REVISION_CONTROL_SYSTEM_CVS = cvs
REVISION_CONTROL_SYSTEM_SCCS = bk

REVISION_CONTROL_SYSTEM_DIRECTORY := $(firstword $(wildcard $(REVISION_CONTROL_SYSTEM_DIRECTORIES)))
REVISION_CONTROL_SYSTEM = $(if $(REVISION_CONTROL_SYSTEM_DIRECTORY),$(REVISION_CONTROL_SYSTEM_$(REVISION_CONTROL_SYSTEM_DIRECTORY)),unknown)

FIND_EXPRESSION_TO_IGNORE_REVISION_CONTROL_SYSTEM_DIRECTORY = $(if $(filter-out ../%,$(REVISION_CONTROL_SYSTEM_DIRECTORY)),-name $(REVISION_CONTROL_SYSTEM_DIRECTORY) -prune -o)

$(takeProfileSample)
# Can we really imagine a project without src/?  I'm wondering whether the wildcard is necessary.
WILDCARD.src := $(wildcard src)
WILDCARD.classes := $(wildcard .generated/classes $(SALMA_HAYEK)/.generated/classes)
JAVA_SOURCE_FILES := $(if $(WILDCARD.src),$(shell find src -type f -name "*.java"))
JAVA_SOURCE_DIRECTORY_PREREQUISITES := $(if $(WILDCARD.src),$(shell find $(WILDCARD.src) $(FIND_EXPRESSION_TO_IGNORE_REVISION_CONTROL_SYSTEM_DIRECTORY) -type d -print))
JAVA_CLASSES_PREREQUISITES := $(if $(WILDCARD.classes),$(shell find $(WILDCARD.classes) -print))
# If classes/ has been deleted, depending on its parent directory should get us rebuilt.
JAVA_CLASSES_PREREQUISITES += $(if $(WILDCARD.classes),,.generated)
$(takeProfileSample)

define GENERATE_CHANGE_LOG.svn
  svn log
endef

define GENERATE_CHANGE_LOG.hg
  hg log
endef

define GENERATE_CHANGE_LOG.cvs
  $(if $(shell which cvs2cl),cvs2cl,cvs2cl.pl) --hide-filenames --stdout
endef

GENERATED_FILES += ChangeLog
GENERATED_FILES += ChangeLog.html
GENERATED_FILES += .generated

MAKE_VERSION_FILE_COMMAND = ruby $(BUILD_SCRIPT_PATH)/make-version-file.rb . $(SALMA_HAYEK)
# By immediately evaluating this, we cause install-everything.sh (or other building-from-source) to warn:
# svn: '.' is not a working copy
# Now we use the version string in the name of the .rpm target, it gets evaluated even if we use = instead of :=.
VERSION_STRING := $(shell $(MAKE_VERSION_FILE_COMMAND) | tail -1)
# If you ever need a Debian equivalent of this Windows-specific script:
# sudo apt-get install uuid
makeGuid = $(shell $(BUILD_SCRIPT_PATH)/uuid.rb)

# ----------------------------------------------------------------------------
# Choose the javac that's in the JDK we found if the user didn't manually
# set JAVA_COMPILER. Currently both javac and ecj are supported.
# ----------------------------------------------------------------------------

JAVA_COMPILER ?= $(JDK_BIN)/javac

# ----------------------------------------------------------------------------
# Set up the classpath.
# TODO: Consider whether we could defer to invoke-java.rb to run the compiler
# and so lose this duplication.
# ----------------------------------------------------------------------------
EXTRA_JARS := $(wildcard $(foreach PROJECT_ROOT,$(DISTINCT_PROJECT_ROOTS),$(PROJECT_ROOT)/lib/jars/*.jar))
CLASS_PATH += $(SALMA_HAYEK)/.generated/classes
CLASS_PATH += $(EXTRA_JARS)

JAVAC_FLAGS += -classpath $(call makeNativePath,$(CLASS_PATH))

# ----------------------------------------------------------------------------
# Set Sun javac flags.
# ----------------------------------------------------------------------------

JAVAC_FLAGS.javac += -d .generated/classes/
JAVAC_FLAGS.javac += -sourcepath src/
JAVAC_FLAGS.javac += -g

# Turn on warnings.
JAVAC_FLAGS.javac += -deprecation
JAVAC_FLAGS.javac += -Xlint:all -Xlint:-serial -Xlint:-this-escape

JAVA_MAJOR_VERSION := $(shell ruby -e 'require "$(JDK_ROOT_SCRIPT)"; puts(JAVA_MAJOR_VERSION)')

# We should also ensure that we build class files that can be used on the current Java release, regardless of where we build.
JAVAC_FLAGS.javac += -target $(JAVA_MAJOR_VERSION)

# Ensure we give a clear error if the user attempts to use anything older.
JAVAC_FLAGS.javac += -source $(JAVA_MAJOR_VERSION)

# Multi-arch from Wheezy and up
BOOT_JDK_ALTERNATIVES += /usr/lib/jvm/java-$(JAVA_MAJOR_VERSION)-openjdk-amd64
# Squeeze and before
BOOT_JDK_ALTERNATIVES += /usr/lib/jvm/java-$(JAVA_MAJOR_VERSION)-openjdk
BOOT_JDK_ALTERNATIVES += /var/chroot/ia32/usr/lib/jvm/java-$(JAVA_MAJOR_VERSION)-openjdk
# := deferred to ALTERNATE_BOOTCLASSPATH
BOOT_JDK.Linux ?= $(firstword $(wildcard $(BOOT_JDK_ALTERNATIVES)))

# FreeBSD
BOOT_JDK.FreeBSD = /usr/local/openjdk${JAVA_MAJOR_VERSION}

# := deferred to ALTERNATE_BOOTCLASSPATH
BOOT_JDK.Cygwin = $(call findMakeFriendlyEquivalentName,$(shell ruby -e 'require "$(JDK_ROOT_SCRIPT)"; puts(findBootJdkFromRegistry())'))

BOOT_JDK = $(BOOT_JDK.$(TARGET_OS))
ALTERNATE_BOOTCLASSPATH ?= $(BOOT_JDK)/jre/lib/rt.jar
ALTERNATE_BOOTCLASSPATH := $(wildcard $(ALTERNATE_BOOTCLASSPATH))
BOOT_JDK_MESSAGE += $(NEWLINE) $(NEWLINE)
BOOT_JDK_MESSAGE += No JDK $(JAVA_MAJOR_VERSION) rt.jar found!
BOOT_JDK_MESSAGE += $(NEWLINE) $(NEWLINE)
BOOT_JDK_MESSAGE += We'll build with -source to ensure language compatibility,
BOOT_JDK_MESSAGE += $(NEWLINE)
BOOT_JDK_MESSAGE += but without rt.jar this build can't guarantee API compatibility.
BOOT_JDK_MESSAGE += $(NEWLINE) $(NEWLINE)
BOOT_JDK_MESSAGE += Be careful!
BOOT_JDK_MESSAGE += $(NEWLINE)
# The *** does battle with filter-build-output.rb.
BOOT_JDK_WARNING = $(warning *** $(BOOT_JDK_MESSAGE))
BOOT_JDK_DIAGNOSTIC = $(if $(filter 8,$(JAVA_MAJOR_VERSION)),$(BOOT_JDK_WARNING))
JAVAC_FLAGS.javac += $(if $(ALTERNATE_BOOTCLASSPATH),-bootclasspath $(ALTERNATE_BOOTCLASSPATH),$(BOOT_JDK_DIAGNOSTIC))

# ----------------------------------------------------------------------------
# Set ecj flags.
# ----------------------------------------------------------------------------

JAVAC_FLAGS.ecj += -d .generated/classes/
JAVAC_FLAGS.ecj += -sourcepath src/
JAVAC_FLAGS.ecj += -g
JAVAC_FLAGS.ecj += -Xemacs

# Turn on warnings.
JAVAC_FLAGS.ecj += -deprecation
JAVAC_FLAGS.ecj += -warn:-serial

# We should also ensure that we build class files that can be used on the current Java release, regardless of where we build.
JAVAC_FLAGS.ecj += -target $(JAVA_MAJOR_VERSION)

# Ensure we give a clear error if the user attempts to use anything older.
JAVAC_FLAGS.ecj += -source $(JAVA_MAJOR_VERSION)

# ----------------------------------------------------------------------------

# javac(1) warns if you build source containing characters unrepresentable
# in your locale. Although we all use UTF-8 locales, we can't guarantee that
# everyone else does, so let the compiler know that our source is in UTF-8.
JAVAC_FLAGS += -encoding UTF-8

# Combine the compiler-specific flags with the portable flags.
JAVAC_FLAGS += $(JAVAC_FLAGS.$(notdir $(JAVA_COMPILER)))

# It's not helpful to list all the Java source files.
define BUILD_JAVA
  @echo "-- Compiling Java source..."
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
FILE_LIST_TO_WXI = $(BUILD_SCRIPT_PATH)/file-list-to-wxi.rb

WIX_COMPILATION_DIRECTORY = .generated/WiX

WIX_ARCH.i386 = x86
WIX_ARCH.amd64 = x64
WIX_ARCH = $(WIX_ARCH.$(TARGET_ARCHITECTURE))

ProgramFilesFolder.i386 = ProgramFilesFolder
ProgramFilesFolder.amd64 = ProgramFiles64Folder
ProgramFilesFolder = $(ProgramFilesFolder.$(TARGET_ARCHITECTURE))

makeInstallerName.msi = $(MACHINE_PROJECT_NAME)-$(1).$(TARGET_ARCHITECTURE).msi
INSTALLER_EXTENSIONS += msi
INSTALLER_EXTENSIONS.Cygwin += msi

# Some people can't use an installer that installs to "C:\Program Files".
# The .msi file's contents don't seem conducive to manual extraction (7-zip says it just contains file1, file2 etc).
makeInstallerName.gz = $(MACHINE_PROJECT_NAME)-$(1).$(TARGET_ARCHITECTURE).tar.gz
INSTALLER_EXTENSIONS += gz
# The contents of a .deb or .rpm are easily extracted with the provided tools.
# So this line is just here for testing.
#INSTALLER_EXTENSIONS.Linux += gz
# The contents of a .msi are not so easily extracted.
INSTALLER_EXTENSIONS.Cygwin += gz

makeInstallerName.zip = $(MACHINE_PROJECT_NAME)-$(1).zip
INSTALLER_EXTENSIONS += zip
INSTALLER_EXTENSIONS.Darwin += zip

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

# Among its many breakages, msiexec is more restrictive about slashes than Windows.
NATIVE_NAME_FOR_MSI_INSTALLER := $(subst /,\,$(call convertToNativeFilenames,$(INSTALLER.msi)))

# We copy the files we want to install into a directory tree whose layout mimics where they'll be installed.
PACKAGING_DIRECTORY = .generated/native/$(TARGET_DIRECTORY)/$(MACHINE_PROJECT_NAME)

# ----------------------------------------------------------------------------
# Distribution variables - where we upload to.
# ----------------------------------------------------------------------------

DIST_MAKEFILE ?= $(SALMA_HAYEK)/lib/build/dist.make

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

define copyLocalVariable
  ERROR.$(1) =
  $(1).$(PREVIOUS_BASE_NAME) := $$($(1))
endef
define unsetLocalVariable
  ERROR.$(1) = $$(shell $(RM) .generated/local-variables.make)$$(error makefile bug: local variable $(1) from scope "$(PREVIOUS_BASE_NAME)" (with value "$($(1).$(PREVIOUS_BASE_NAME))") was referred to in scope "$$(BASE_NAME)")
  $(1) = $$(ERROR.$(1))
endef

# We need to $(eval) each assignment individually before they're concatenated
# by $(foreach) and hence turned into a syntax error.
forEachLocalVariable = $(foreach LOCAL_VARIABLE,$(LOCAL_VARIABLES),$(eval $(call $(1),$(LOCAL_VARIABLE))))

define closeLocalVariableScope
  $(eval PREVIOUS_BASE_NAME := $$(BASE_NAME))
  $(call forEachLocalVariable,copyLocalVariable)
  $(call forEachLocalVariable,unsetLocalVariable)
endef

# This variable can only be correctly evaluated after we've included per-directory.make.
# This should remain the first reference to it, to keep us out of trouble.
ALL_PER_DIRECTORY_TARGETS = $(error ALL_PER_DIRECTORY_TARGETS evaluated too early)

BUILD_TARGETS += $(if $(JAVA_SOURCE_FILES),$(PROJECT_ROOT)/.generated/java.build-finished)
BUILD_TARGETS += $(if $(REVISION_CONTROL_SYSTEM_DIRECTORY),.generated/build-revision.txt)
BUILD_TARGETS += $(ALL_PER_DIRECTORY_TARGETS)
BUILD_TARGETS += $(CRT_SHARED_LIBRARIES)

FILES_TO_INSTALL += $(ALL_PER_DIRECTORY_TARGETS)
FILES_TO_INSTALL += .generated/build-revision.txt
FILES_TO_INSTALL += $(CRT_SHARED_LIBRARIES)
IS_SALMA_HAYEK = $(filter $(SALMA_HAYEK),$(PROJECT_ROOT))
FILES_TO_INSTALL += $(if $(IS_SALMA_HAYEK),native/Headers/JAVA_MAJOR_VERSION.h)

TIC_SOURCE := $(wildcard lib/terminfo/*.tic)
# FreeBSD seems to prefer compiling tic in such a way that it outputs the hashed
# database form of terminfo, while Linux and others generate the plain text
# versions.
# TODO: Instead of relying on some arbitrary assumptions about how operating
# systems are configured, automatically figure out if 'terminfo.db' or the
# text form has been created, and pick the right one. I'm going to leave that
# as an exercise to someone who knows Makefile better than I.
# We deliberately omit the intermediate directory.
COMPILED_TERMINFO.$(TARGET_OS) = $(patsubst lib/terminfo/%.tic,.generated/terminfo/%,$(TIC_SOURCE))
COMPILED_TERMINFO.FreeBSD = $(if $(TIC_SOURCE),.generated/terminfo.db)
COMPILED_TERMINFO = $(COMPILED_TERMINFO.$(TARGET_OS))

# We've had some issues in the past with cross-platform compatibility of
# terminfo. The following two lines can be commented out (or, better, disabled
# on a per-platform basis) if some incompatibility is found, at least until
# such a time as the terminfo is fixed.
BUILD_TARGETS += $(COMPILED_TERMINFO)
FILES_TO_INSTALL += $(COMPILED_TERMINFO)

SUBDIRECTORIES_TO_INSTALL += bin
SUBDIRECTORIES_TO_INSTALL += doc
SUBDIRECTORIES_TO_INSTALL += lib

define MAKE_INSTALLER_FILE_LIST
  { \
    $(foreach file,$(patsubst $(PROJECT_ROOT)/%,%,$(FILES_TO_INSTALL)),echo $(file) &&) \
    find $(wildcard $(SUBDIRECTORIES_TO_INSTALL)) $(FIND_EXPRESSION_TO_IGNORE_REVISION_CONTROL_SYSTEM_DIRECTORY) -type f -print; \
  } | ruby -ne 'puts("Including #{$$_.chomp()}...")'
endef

# ----------------------------------------------------------------------------
# Variables above this point, rules below.
# Variables used on either side of the colon in rules are evaluated on the first pass,
# so you can't override them after the rule has been seen, even if they don't appear
# in any overt immediate evaluations (like := assignments).
# ----------------------------------------------------------------------------

# build is the default target and so must appear first, though we can't evaluate BUILD_TARGETS until later.
.PHONY: build
build:

# ----------------------------------------------------------------------------
# The magic incantation to build and clean all the native subdirectories.
# Including per-directory.make more than once is bound to violate the
# variables-before-rules dictum.
# per-directory.make needs to cope with that but it'd be best if it doesn't impose
# that constraint on the rest of universal.make - so let's keep this after the universal.make variables.
# ----------------------------------------------------------------------------

# $(SOURCE_DIRECTORY) is effectively a local variable, though it's assigned in buildNativeDirectory.
LOCAL_VARIABLES += SOURCE_DIRECTORY

# We want to use the $(BASE_NAME) of the preceding scope in error messages.
define buildNativeDirectory
  SOURCE_DIRECTORY = $(1)
  include $(SALMA_HAYEK)/lib/build/per-directory.make
endef

$(takeProfileSample)
DUMMY := $(foreach SUBDIR,$(SUBDIRS),$(eval $(call buildNativeDirectory,$(SUBDIR)))$(closeLocalVariableScope))
BASE_NAME = rules
$(takeProfileSample)

# Redefined here because this can only be correctly evaluated after we've included per-directory.make.
ALL_PER_DIRECTORY_TARGETS = $(foreach SUBDIR,$(SUBDIRS),$(DESIRED_TARGETS.$(notdir $(SUBDIR))))

# Now we can evaluate BUILD_TARGETS.
build: $(BUILD_TARGETS)

# This sentinel tells us that we need to rebuild if the source changes during the compilation:
# ie before the output files have been generated but (potentially) after the source files have been read.
.generated/java.build-started: $(MAKEFILE_LIST) $(JAVA_SOURCE_FILES) $(JAVA_SOURCE_DIRECTORY_PREREQUISITES)
	mkdir -p $(@D) && \
	touch $@

# With JAVA_CLASSES_PREREQUISITES we're really only interested in .class files that are newer than the sentinel, especially those from salma-hayek when we're building other projects.
$(PROJECT_ROOT)/.generated/java.build-finished: .generated/java.build-started $(JAVA_CLASSES_PREREQUISITES)
	$(BUILD_JAVA)

.PHONY: clean
clean:
	$(RM) -r $(GENERATED_FILES) && \
	find . -name "*.bak" | xargs $(RM)

.PHONY: native-clean
native-clean:
	$(RM) -r .generated/native

ChangeLog.html: ChangeLog
	ruby $(BUILD_SCRIPT_PATH)/svn-log-to-html.rb < $< > $@.tmp && \
	$(MOVE_GENERATED_TARGET_INTO_PLACE)

.PHONY: ChangeLog
ChangeLog:
	$(GENERATE_CHANGE_LOG.$(REVISION_CONTROL_SYSTEM)) > $@.tmp && \
	$(MOVE_GENERATED_TARGET_INTO_PLACE)

.PHONY: .generated/build-revision.txt
.generated/build-revision.txt:
	mkdir -p $(@D) && \
	$(MAKE_VERSION_FILE_COMMAND) > $@.tmp && \
	$(MOVE_GENERATED_TARGET_INTO_PLACE)

# Old versions of SunOS tic don't support the -o argument but do support redirecting
# the output to $TERMINFO.
# I'd like to use -v10 but am stymied by http://bugs.debian.org/cgi-bin/bugreport.cgi?bug=365120
# With the mv, we both avoid having to cope with Mac OS's broken ncurses library,
# which thinks that terminator's terminfo belongs in 74/ rather than t/,
# and avoid having to work out the first letter of the terminal name.
ifneq "$(TARGET_OS)" "FreeBSD"
.generated/terminfo/%: lib/terminfo/%.tic
	mkdir -p $(@D) && \
	TERMINFO=$(@D) tic -v1 $< && \
	mv $(@D)/*/$(@F) $@
else
# As mentioned above, FreeBSD likes to make a terminfo.db file, which is a
# hashed binary database, rather than the directory structure preferred by
# Linux and others.
# As this is a compiled-in choice, not a run-time command line argument, this
# is our best option currently (at least within the realms of my Makefile-fu)
# to support all platforms.
.generated/terminfo.db: lib/terminfo/*.tic
	TERMINFO=$(@D) tic -v1 $< && \
	mv $(@D).db $@
endif

# ----------------------------------------------------------------------------
# Redistributables.
# ----------------------------------------------------------------------------

.generated/amd64_Cygwin/bin/libwinpthread-1.dll: /usr/x86_64-w64-mingw32/sys-root/mingw/bin/libwinpthread-1.dll
	$(COPY_RULE)

.generated/i386_Cygwin/bin/libwinpthread-1.dll: /usr/i686-w64-mingw32/sys-root/mingw/bin/libwinpthread-1.dll
	$(COPY_RULE)

# https://www.neowin.net/forum/topic/1194367-is-libwinpthread-1dll-covered-by-the-gcc-runtime-library-exception/
.generated/amd64_Cygwin/bin/winpthreads.COPYING.txt: /usr/share/doc/mingw64-x86_64-winpthreads/COPYING
	$(COPY_RULE)

.generated/i386_Cygwin/bin/winpthreads.COPYING.txt: /usr/share/doc/mingw64-i686-winpthreads/COPYING
	$(COPY_RULE)

# ----------------------------------------------------------------------------
# How to build a .app directory and package it into an installer file.
# ----------------------------------------------------------------------------

# The output of this make invocation is captured and processed, so we mustn't echo it.
.PHONY: installer-file-list
installer-file-list:
	@$(MAKE_INSTALLER_FILE_LIST)

# Unfortunately, the start-up scripts tend to go looking for salma-hayek, so we can't just have Resources/bin etc; we have to keep the multi-directory structure, at least for now.
.PHONY: $(MACHINE_PROJECT_NAME).app
$(MACHINE_PROJECT_NAME).app: $(BUILD_TARGETS) .generated/build-revision.txt
	$(BUILD_SCRIPT_PATH)/package-for-distribution.rb $(HUMAN_PROJECT_NAME) $(MACHINE_PROJECT_NAME) $(SALMA_HAYEK)

# We no longer use .dmg because (a) there have been bugs in 10.5 and 10.6 that
# cause false "the disk image you are opening may be damaged" warnings and (b)
# the whole mounting a virtual disk image thing is just weird and annoying.
# Zip file support isn't buggy, most people from most backgrounds are familiar
# with zip files, and Safari will extract zip files automatically and dispose
# of the useless .zip husk automatically.
# FIXME: if we made it possible to read the license from the app (as you can
# on Linux), we could dispense with the top-level "terminator" directory and
# just zip Terminator.app, which would leave users with a double-clickable app
# in their downloads window (rather than a directory containing a
# double-clickable app).
$(INSTALLER.zip): $(MACHINE_PROJECT_NAME).app
	@echo "-- Creating Mac OS .zip file..."
	mkdir -p $(@D) && \
	$(RM) $@ && \
	cd $(dir $(PACKAGING_DIRECTORY)) && \
	zip -r -9 -y $@ $(notdir $(PACKAGING_DIRECTORY))

$(INSTALLER.pkg): $(MACHINE_PROJECT_NAME).app
	@echo "-- Creating package stream..."
	mkdir -p $(@D) && \
	$(RM) $@  && \
	pkgmk -o -d $(@D) -f $(PACKAGING_DIRECTORY)/prototype -r $(PACKAGING_DIRECTORY)/root/ && \
	pkgtrans -s $(@D) $(@F) SJO$(MACHINE_PROJECT_NAME) && \
	pkginfo -l -d $@

$(INSTALLER.deb): $(MACHINE_PROJECT_NAME).app
	@echo "-- Creating GNU/Linux .deb package..."
	mkdir -p $(@D) && \
	$(RM) $@ && \
	fakeroot dpkg-deb --build $(PACKAGING_DIRECTORY) $@ && \
	dpkg-deb --info $@ # && dpkg-deb --contents $@

$(INSTALLER.rpm): $(INSTALLER.deb)
	@echo "-- Creating GNU/Linux .rpm package..."
	mkdir -p $(@D)
	$(RM) $@
	@# While we used Debian Squeeze for i386 builds, we had to endure:
	@# https://bugs.debian.org/cgi-bin/bugreport.cgi?bug=622846
	@# alien: error message: find: `<pkgname>-<version>': No such file or directory
	cd $(@D) && \
	fakeroot alien --to-rpm $(abspath $<)

$(INSTALLER.gz): $(MACHINE_PROJECT_NAME).app
	@echo "-- Creating .tar.gz distribution..."
	mkdir -p $(@D) && \
	$(RM) $@ && \
	tar -zcf $@ -C $(PACKAGING_DIRECTORY)/.. $(notdir $(PACKAGING_DIRECTORY))

# ----------------------------------------------------------------------------
# WiX
# ----------------------------------------------------------------------------

# Later we add more dependencies when we know $(ALL_PER_DIRECTORY_TARGETS).
%/component-definitions.wxi: $(MAKEFILE_LIST) $(FILE_LIST_TO_WXI) $(MACHINE_PROJECT_NAME).app
	mkdir -p $(@D) && \
	( cd $(PACKAGING_DIRECTORY) && find . -type f -print ) | cut -c3- | $(FILE_LIST_TO_WXI) > $@.tmp && \
	$(MOVE_GENERATED_TARGET_INTO_PLACE)

# This silliness is probably sufficient (as well as sadly necessary).
%/component-references.wxi: %/component-definitions.wxi $(MAKEFILE_LIST)
	mkdir -p $(@D) && \
	ruby -w -ne '$$_.match(/Include/) && puts($$_); $$_.match(/<Component (Id='\''component\d+'\'')/) && puts("<ComponentRef #{$$1} />")' < $< > $@.tmp && \
	$(MOVE_GENERATED_TARGET_INTO_PLACE)

%.wixobj: %.wxs $(patsubst %,$(WIX_COMPILATION_DIRECTORY)/component-%.wxi,references definitions)
	@echo "-- Compiling $(notdir $<)..."
	HUMAN_PROJECT_NAME=$(HUMAN_PROJECT_NAME) \
	MACHINE_PROJECT_NAME=$(MACHINE_PROJECT_NAME) \
	OPEN_HERE_GUID=$(makeGuid) \
	PATH_GUID=$(makeGuid) \
	PRODUCT_GUID=$(makeGuid) \
	ProgramFilesFolder=$(ProgramFilesFolder) \
	SHORTCUT_GUID=$(makeGuid) \
	STANDARD_FILES_GUID=$(makeGuid) \
	TARGET_DIRECTORY=$(TARGET_DIRECTORY) \
	UPGRADE_GUID=$(UPGRADE_GUID) \
	VERSION_STRING=$(VERSION_STRING) \
	candle -nologo -arch $(WIX_ARCH) -out $(call convertToNativeFilenames,$@ $<)

# Cygwin 1.7 removes all inheritable security when it creates directories.
# This causes the .msi (but, for no obvious reason, not the .wixobj) to be unreadable.
# This prevents msiexec from running the installer.
# ug+r is not enough (perhaps it has to be readable by SYSTEM or some such).
$(INSTALLER.msi): $(WIX_COMPILATION_DIRECTORY)/$(MACHINE_PROJECT_NAME).wixobj $(BUILD_TARGETS)
	@echo "-- Creating Windows installer..."
	cd $(PACKAGING_DIRECTORY) && \
	light -nologo -out $(call convertToNativeFilenames,$(abspath $@) $(abspath $<)) && \
	chmod a+r $@

$(WIX_COMPILATION_DIRECTORY)/$(MACHINE_PROJECT_NAME).wxs: $(SALMA_HAYEK)/lib/build/installer.wxs
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
.generated/local-variables.make: $(SALMA_HAYEK)/lib/build/per-directory.make $(SALMA_HAYEK)/lib/build/universal.make
	mkdir -p $(@D) && \
	ruby -w -ne '($$_.match(/^ *(\S+)\s*[:+]?=/) || $$_.match(/^\s*define\s*(\S+)/)) && puts("LOCAL_VARIABLES += #{$$1}")' $< | sort -u > $@.tmp && \
	$(MOVE_GENERATED_TARGET_INTO_PLACE)

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

.PHONY: native
native: $(ALL_PER_DIRECTORY_TARGETS)

.PHONY: installer
installer: $(PUBLISHABLE_INSTALLERS)

# We still need the default salma-hayek build during the nightly build.
install installer native-dist: $(BUILD_TARGETS)

include $(DIST_MAKEFILE)

.PHONY: install
install: $(addprefix run-installer,$(suffix $(PUBLISHABLE_INSTALLERS)))

$(addprefix run-installer,$(suffix $(PUBLISHABLE_INSTALLERS))): $(PUBLISHABLE_INSTALLERS)

.PHONY: remove
remove: $(addprefix run-remover,$(suffix $(PUBLISHABLE_INSTALLERS)))

.PHONY: run-installer.pkg
run-installer.pkg:
	@echo "-- Running Solaris installer..."
	yes | sudo /usr/sbin/pkgadd -G -d $(INSTALLER.pkg) all

.PHONY: run-installer.deb
run-installer.deb:
	@echo "-- Running Debian installer..."
	sudo dpkg -i $(INSTALLER.deb)

# We use Debian packaging tools to build RPM installers, so "make installer" is unlikely work on RedHat.
# And "make install" always rebuilds the installer because of the bogus ".app" target.
# The command you want on RedHat is:
# sudo rpm -i `make -f ../salma-hayek/lib/build/universal.make echo.INSTALLER.rpm`
.PHONY: run-installer.rpm
run-installer.rpm:;

.PHONY: run-installer.zip
run-installer.zip:
	@echo "-- Running Mac OS installer..."
	open $(INSTALLER.zip)

.PHONY: run-installer.msi
run-installer.msi:
	@echo "-- Running Windows installer..."
	msiexec /i $(NATIVE_NAME_FOR_MSI_INSTALLER)

# This only works if this is precisely the same version that was installed.
# We can't uninstall by GUID because the GUID you give to the uninstaller is
# one of the GUIDs that we have to change each time we build in order for Windows
# to realize that we're building a replacement version.
# We could search the Registry for the installed GUID but it's indexed by GUID
# rather than name.
.PHONY: run-remover.msi
run-remover.msi:
	msiexec /x $(NATIVE_NAME_FOR_MSI_INSTALLER)

.PHONY: test
test: build
	@echo "-- Running unit tests..."
	# Beware of passing absolute Cygwin paths to Java.
	$(SCRIPT_PATH)/org.jessies.TestRunner .generated/classes

.PHONY: findbugs
findbugs: build
	@echo "-- Running findbugs..."
	findbugs -textui -emacs -auxclasspath $(call makeNativePath,$(EXTRA_JARS)) -sourcepath $(subst $(SPACE),:,$(foreach PROJECT_ROOT,$(DISTINCT_PROJECT_ROOTS),$(PROJECT_ROOT)/src)) $(foreach PROJECT_ROOT,$(DISTINCT_PROJECT_ROOTS),$(PROJECT_ROOT)/.generated/classes)

# A rule for preprocessing C++.
%.i: %.cpp
	$(filter-out -c,$(COMPILE.cpp)) -E -dD $< -o $@
