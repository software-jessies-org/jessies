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

JDK_INCLUDE.$(TARGET_OS) = $(JDK_ROOT)/include
JDK_INCLUDE.Darwin = /Developer/SDKs/MacOSX10.6.sdk/$(JDK_ROOT)/Headers
JDK_INCLUDE = $(JDK_INCLUDE.$(TARGET_OS))

JDK_BIN_DIR.$(TARGET_OS) = bin
JDK_BIN_DIR.Darwin = Commands
JDK_BIN = $(JDK_ROOT)/$(JDK_BIN_DIR.$(TARGET_OS))

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

# ----------------------------------------------------------------------------
# Extra compiler and (mainly) linker flags for building JNI.
# ----------------------------------------------------------------------------

# Tradition has it this way.
SHARED_LIBRARY_PREFIX.$(TARGET_OS) = lib

SHARED_LIBRARY_EXTENSION.$(TARGET_OS) = so
# http://developer.apple.com/documentation/Porting/Conceptual/PortingUnix/compiling/chapter_4_section_9.html
# "By default, the names of dynamic libraries in Mac OS X end in .dylib instead of .so."
SHARED_LIBRARY_EXTENSION.Darwin = dylib

JNI_LIBRARY_EXTENSION.$(TARGET_OS) = $(SHARED_LIBRARY_EXTENSION.$(TARGET_OS))
JNI_LIBRARY_EXTENSION.Darwin = jnilib
JNI_LIBRARY_EXTENSION = $(JNI_LIBRARY_EXTENSION.$(TARGET_OS))

SHARED_LIBRARY_LDFLAGS.Darwin += -dynamiclib
# The default $(LD) doesn't know about -dynamiclib on Darwin.
# This doesn't hurt on Linux, indeed it generally saves having to specify nonsense like -lstdc++.
LD = $(CXX)

# Note that our Solaris build assumes GCC rather than Sun's compiler.
# GCC's -shared option, which we use on Linux, exists, but produces link
# errors. -G, as used in Sun's tutorial examples with their own compiler works.
EXTRA_INCLUDE_PATH.SunOS += $(JDK_INCLUDE)/solaris
SHARED_LIBRARY_LDFLAGS.SunOS += -G

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

ifeq "$(wildcard $(foreach include,$(EXTRA_INCLUDE_PATH),$(include)/jni_md.h))" ""
	JDK_ROOT := $(error $(JDK_ROOT) is not a sane location for JDK headers)
endif

SHARED_LIBRARY_LDFLAGS = $(SHARED_LIBRARY_LDFLAGS.$(TARGET_OS))
SHARED_LIBRARY_PREFIX = $(SHARED_LIBRARY_PREFIX.$(TARGET_OS))
SHARED_LIBRARY_EXTENSION = $(SHARED_LIBRARY_EXTENSION.$(TARGET_OS))

# ----------------------------------------------------------------------------
# Extra flags to always build Universal Binaries on Mac OS.
# http://developer.apple.com/documentation/Porting/Conceptual/PortingUnix/compiling/chapter_4_section_3.html
# ----------------------------------------------------------------------------

universal_binary_flags = -mmacosx-version-min=10.4 -isysroot /Developer/SDKs/MacOSX10.6.sdk -arch ppc -arch i386
C_AND_CXX_FLAGS.Darwin += $(universal_binary_flags)
LDFLAGS.Darwin += $(universal_binary_flags)
LDFLAGS.Darwin += -lobjc
# Mac OS 10.6 won't link PowerPC C++ without this.
LDFLAGS.Darwin += -lstdc++
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
MINGW_FLAGS.i686-pc-mingw32-g++ += -static-libgcc -static-libstdc++
C_AND_CXX_FLAGS.Cygwin += $(if $(COMPILING_MINGW),$(MINGW_FLAGS.$(MINGW_COMPILER)))

# Facilitate overriding for CXX that's conditional on a per-target, per-directory basis.
DEFAULT_CXX := $(CXX)
CXX.$(TARGET_OS) = $(DEFAULT_CXX)
CXX = $(CXX.$(TARGET_OS))

# Cygwin 1.7 has a g++-4 which can be installed as the default compiler.
# Its compiler driver has no -mno-cygwin option.
MINGW_COMPILER = g++-3
# The newly packaged cross-compiler isn't available in 1.7.7.
# We have trouble forking after loading the JVM in later versions.
#MINGW_COMPILER = i686-pc-mingw32-g++
CXX.Cygwin = $(if $(COMPILING_MINGW),$(MINGW_COMPILER),$(DEFAULT_CXX))

# Mac OS 10.6 ships with gcc 4.2.1 as the default but, until we want to drop 10.4, we need headers like /Developer/SDKs/MacOSX10.4u.sdk/usr/include/c++/4.0.0/sstream
# The compiler really is one sub-minor version ahead of the directory.
CXX.Darwin = $(DEFAULT_CXX) -V4.0.1

HAVE_MINGW_SOURCE := $(wildcard $(CURDIR)/native/Mingw)
CRT_SHARED_LIBRARIES.Cygwin += $(if $(HAVE_MINGW_SOURCE),.generated/$(TARGET_DIRECTORY)/bin/mingwm10.dll)
CRT_SHARED_LIBRARIES += $(CRT_SHARED_LIBRARIES.$(TARGET_OS))

# One day we might have to look in eg /usr/share/doc/mingw32-runtime/mingwm10.dll.gz.
MINGW_DLL_ALTERNATIVES += /usr/i686-pc-mingw32/sys-root/mingw/bin/mingwm10.dll
MINGW_DLL_ALTERNATIVES += /bin/mingwm10.dll
EXTANT_MINGW_DLL_ALTERNATIVES := $(wildcard $(MINGW_DLL_ALTERNATIVES))
MINGW_DLL = $(firstword $(EXTANT_MINGW_DLL_ALTERNATIVES))

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

REVISION_CONTROL_SYSTEM_DIRECTORIES += .hg
REVISION_CONTROL_SYSTEM_DIRECTORIES += .svn
REVISION_CONTROL_SYSTEM_DIRECTORIES += CVS
REVISION_CONTROL_SYSTEM_DIRECTORIES += SCCS

REVISION_CONTROL_SYSTEM_.hg = hg
REVISION_CONTROL_SYSTEM_.svn = svn
REVISION_CONTROL_SYSTEM_CVS = cvs
REVISION_CONTROL_SYSTEM_SCCS = bk

REVISION_CONTROL_SYSTEM_DIRECTORY := $(firstword $(wildcard $(REVISION_CONTROL_SYSTEM_DIRECTORIES)))
REVISION_CONTROL_SYSTEM = $(if $(REVISION_CONTROL_SYSTEM_DIRECTORY),$(REVISION_CONTROL_SYSTEM_$(REVISION_CONTROL_SYSTEM_DIRECTORY)),unknown)

FIND_EXPRESSION_TO_IGNORE_REVISION_CONTROL_SYSTEM_DIRECTORY = $(if $(REVISION_CONTROL_SYSTEM_DIRECTORY),-name $(REVISION_CONTROL_SYSTEM_DIRECTORY) -prune -o)

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
# Choose a Java compiler.
# ----------------------------------------------------------------------------

JAVA_COMPILER ?= $(JDK_BIN)/javac
ifeq "$(wildcard $(JAVA_COMPILER)$(EXE_SUFFIX))" ""
  JAVA_COMPILER := $(error Unable to find $(JAVA_COMPILER) --- do you only have a JRE installed or did you explicitly supply a non-absolute path?)
endif

# ----------------------------------------------------------------------------
# Set up the classpath.
# TODO: Consider whether we could defer to invoke-java.rb to run the compiler
# and so lose this duplication.
# ----------------------------------------------------------------------------
EXTRA_JARS := $(wildcard $(foreach PROJECT_ROOT,$(DISTINCT_PROJECT_ROOTS),$(PROJECT_ROOT)/lib/jars/*.jar))
CLASS_PATH += $(SALMA_HAYEK)/.generated/classes
CLASS_PATH += $(EXTRA_JARS)

# "tools.jar" doesn't exist on Mac OS (the classes are automatically available).
# Java 6 will do likewise for the other platforms, at which point this can be removed.
TOOLS_JAR := $(wildcard $(JDK_ROOT)/lib/tools.jar)
CLASS_PATH += $(TOOLS_JAR)

JAVAC_FLAGS += -classpath $(call makeNativePath,$(CLASS_PATH))

# ----------------------------------------------------------------------------
# Set Sun javac flags.
# ----------------------------------------------------------------------------

JAVAC_FLAGS.javac += -d .generated/classes/
JAVAC_FLAGS.javac += -sourcepath src/
JAVAC_FLAGS.javac += -g

# Turn on warnings.
JAVAC_FLAGS.javac += -deprecation
JAVAC_FLAGS.javac += -Xlint:all -Xlint:-serial

# We should also ensure that we build class files that can be used on the current Java release, regardless of where we build.
JAVAC_FLAGS.javac += -target 1.6

# Ensure we give a clear error if the user attempts to use anything older than Java 6.
JAVAC_FLAGS.javac += -source 1.6

# ----------------------------------------------------------------------------
# Set ecj flags.
# ----------------------------------------------------------------------------

JAVAC_FLAGS.ecj += -d .generated/classes/
JAVAC_FLAGS.ecj += -sourcepath src/
JAVAC_FLAGS.ecj += -g
JAVAC_FLAGS.ecj += -Xemacs
JAVAC_FLAGS.ecj += -referenceInfo

# Turn on warnings.
JAVAC_FLAGS.ecj += -deprecation
JAVAC_FLAGS.ecj += -warn:+allDeprecation,conditionAssign,dep-ann,enumSwitch,fallthrough,finalBound,noEffectAssign,null,nullDereference,over-ann,pkgDefaultMethod,raw,semicolon,unused,uselessTypeCheck,varargsCast
JAVAC_FLAGS.ecj += -warn:-serial
JAVAC_FLAGS.ecj += -proceedOnError

# We should also ensure that we build class files that can be used on the current Java release, regardless of where we build.
JAVAC_FLAGS.ecj += -target 1.6

# Ensure we give a clear error if the user attempts to use anything older than Java 6.
JAVAC_FLAGS.ecj += -source 1.6

# ----------------------------------------------------------------------------
# Set GCJ flags.
# ----------------------------------------------------------------------------

JAVAC_FLAGS.gcj += -Wall -Wdeprecated
JAVAC_FLAGS.gcj += -Wno-indirect-static
JAVAC_FLAGS.gcj += -Wno-serial
JAVAC_FLAGS.gcj += -combine
JAVAC_FLAGS.gcj += -encoding UTF-8
JAVAC_FLAGS.gcj += -fjni
JAVAC_FLAGS.gcj += --main=$(GCJ_MAIN_CLASS)
JAVAC_FLAGS.gcj += -o $(MACHINE_PROJECT_NAME)

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

makeInstallerName.msi = $(MACHINE_PROJECT_NAME)-$(1).msi
INSTALLER_EXTENSIONS += msi
INSTALLER_EXTENSIONS.Cygwin += msi

# Some people can't use an installer that installs to "C:\Program Files".
# The .msi file's contents don't seem conducive to manual extraction (7-zip says it just contains file1, file2 etc).
makeInstallerName.gz = $(MACHINE_PROJECT_NAME)-$(1).tar.gz
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
TIC_SOURCE := $(wildcard lib/terminfo/*.tic)
# We deliberately omit the intermediate directory.
COMPILED_TERMINFO = $(patsubst lib/terminfo/%.tic,.generated/terminfo/%,$(TIC_SOURCE))
BUILD_TARGETS += $(COMPILED_TERMINFO)
BUILD_TARGETS += $(ALL_PER_DIRECTORY_TARGETS)
BUILD_TARGETS += $(CRT_SHARED_LIBRARIES)

FILES_TO_INSTALL += $(ALL_PER_DIRECTORY_TARGETS)
FILES_TO_INSTALL += .generated/build-revision.txt
FILES_TO_INSTALL += $(COMPILED_TERMINFO)
FILES_TO_INSTALL += $(CRT_SHARED_LIBRARIES)

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
.generated/terminfo/%: lib/terminfo/%.tic
	mkdir -p $(@D) && \
	TERMINFO=$(@D) tic -v1 $< && \
	mv $(@D)/*/$(@F) $@

.generated/$(TARGET_DIRECTORY)/bin/mingwm10.dll: $(MINGW_DLL)
	mkdir -p $(@D) && \
	cp $< $@.tmp && \
	$(MOVE_GENERATED_TARGET_INTO_PLACE)

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
	mkdir -p $(@D) && \
	$(RM) $@ && \
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
	SHORTCUT_GUID=$(makeGuid) \
	STANDARD_FILES_GUID=$(makeGuid) \
	UPGRADE_GUID=$(UPGRADE_GUID) \
	VERSION_STRING=$(VERSION_STRING) \
	candle -nologo -out $(call convertToNativeFilenames,$@ $<)

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

.PHONY: gcj
gcj:
	rm -rf .generated/classes/ && JAVA_COMPILER=/usr/bin/gcj make && rm -rf .generated && make && sudo mv $(MACHINE_PROJECT_NAME) /usr/bin

.PHONY: findbugs
findbugs: build
	@echo "-- Running findbugs..."
	findbugs -textui -emacs -auxclasspath $(call makeNativePath,$(EXTRA_JARS)) -sourcepath $(subst $(SPACE),:,$(foreach PROJECT_ROOT,$(DISTINCT_PROJECT_ROOTS),$(PROJECT_ROOT)/src)) $(foreach PROJECT_ROOT,$(DISTINCT_PROJECT_ROOTS),$(PROJECT_ROOT)/.generated/classes)

# A rule for preprocessing C++.
%.i: %.cpp
	$(filter-out -c,$(COMPILE.cpp)) -E -dD $< -o $@
