# You may use:
#   make
#   make clean
#   make native
#   make installer
#   make install
#   make remove
#   make native-clean
#   make native-dist
#   make source-dist
#   make www-dist

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
.SECONDARY:

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
TARGET_OS := $(shell ruby $(TARGET_OS_SCRIPT))

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
    ifeq "$(TARGET_OS)" "Solaris"
        $(warning Try installing the Blastwave "gmake" package.)
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

JNI_LIBRARY_LDFLAGS += $(LDFLAGS)

JNI_LIBRARY_LDFLAGS.Darwin += -dynamiclib -framework JavaVM
JNI_LIBRARY_PREFIX.Darwin = lib
JNI_LIBRARY_EXTENSION.Darwin = jnilib
# The default $(LD) doesn't know about -dynamiclib on Darwin.
# This doesn't hurt on Linux, indeed it generally saves having to specify nonsense like -lstdc++.
LD = $(CXX)

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

EXTANT_INCLUDE_DIRECTORIES := $(wildcard $(EXTRA_INCLUDE_PATH))
NON_EXISTENT_INCLUDE_DIRECTORIES = $(filter-out $(EXTANT_INCLUDE_DIRECTORIES),$(EXTRA_INCLUDE_PATH))
ifneq "$(NON_EXISTENT_INCLUDE_DIRECTORIES)" ""
  $(warning Could not find $(NON_EXISTENT_INCLUDE_DIRECTORIES) - perhaps the first java on your PATH isn't in a JDK)
endif

JNI_LIBRARY_LDFLAGS += $(JNI_LIBRARY_LDFLAGS.$(TARGET_OS))
JNI_LIBRARY_PREFIX = $(JNI_LIBRARY_PREFIX.$(TARGET_OS))
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

BIN_DIRECTORY = $(PROJECT_ROOT)/.generated/$(TARGET_OS)/bin
LIB_DIRECTORY = $(PROJECT_ROOT)/.generated/$(TARGET_OS)/lib

# By default, distributions end up under http://software.jessies.org/
DIST_SSH_USER_AND_HOST=software@jessies.org
# The html files are copied into the parent directory.
DIST_DIRECTORY=/home/software/downloads/$(MACHINE_PROJECT_NAME)

$(takeProfileSample)
SOURCE_FILES := $(if $(wildcard $(PROJECT_ROOT)/src),$(shell find $(PROJECT_ROOT)/src -type f -name "*.java"))
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

CREATE_OR_UPDATE_JAR=cd $(2)/classes && jar $(1)f $(CURDIR)/$@ $(notdir $(wildcard $(2)/classes/*))

GENERATED_FILES += ChangeLog
GENERATED_FILES += ChangeLog.html
GENERATED_FILES += classes
GENERATED_FILES += .generated
GENERATED_FILES += $(MACHINE_PROJECT_NAME).jar

MAKE_VERSION_FILE_COMMAND = ruby $(SCRIPT_PATH)/make-version-file.rb $(PROJECT_ROOT) $(SALMA_HAYEK)
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
CLASS_PATH += $(SALMA_HAYEK)/classes
CLASS_PATH += $(SALMA_HAYEK)/AppleJavaExtensions.jar
CLASS_PATH += $(SALMA_HAYEK)/swing-worker.jar

# "tools.jar" doesn't exist on Mac OS (the classes are automatically available).
# Java 6 will do likewise for the other platforms, at which point this can be removed.
TOOLS_JAR := $(wildcard $(JDK_ROOT)/lib/tools.jar)
CLASS_PATH += $(TOOLS_JAR)

JAVAC_FLAGS += $(addprefix -classpath ,$(call makeNativePath,$(CLASS_PATH)))

# ----------------------------------------------------------------------------
# Set default javac flags.
# ----------------------------------------------------------------------------

JAVAC_FLAGS += -d classes/
JAVAC_FLAGS += -sourcepath src/
JAVAC_FLAGS += -g

# Turn on warnings.
JAVAC_FLAGS += -deprecation
JAVAC_FLAGS += -Xlint -Xlint:-serial

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

define BUILD_JAVA
  @echo Recompiling the world... && \
  $(RM) -r classes && \
  mkdir -p classes && \
  $(JAVA_COMPILER) $(JAVAC_FLAGS) $(call convertToNativeFilenames,$(SOURCE_FILES))
endef

# ----------------------------------------------------------------------------
# Installer variables
# ----------------------------------------------------------------------------

# We get the shell to find candle and light on the path but we mention
# file-list-to-wxi in a prerequisite and so must know its exact location.
FILE_LIST_TO_WXI = $(SCRIPT_PATH)/file-list-to-wxi.rb

WIX_COMPILATION_DIRECTORY = .generated/WiX

INSTALLER.wix.$(MACHINE_PROJECT_NAME) = $(BIN_DIRECTORY)/$(MACHINE_PROJECT_NAME).msi
INSTALLER.wix.salma-hayek = $(WIX_COMPILATION_DIRECTORY)/$(MACHINE_PROJECT_NAME).msm
INSTALLER.wix = $(INSTALLER.wix.$(MACHINE_PROJECT_NAME))
INSTALLERS.Cygwin += $(INSTALLER.wix)

INSTALLER.dmg += $(BIN_DIRECTORY)/$(MACHINE_PROJECT_NAME).dmg
INSTALLERS.Darwin += $(INSTALLER.dmg)

INSTALLER.deb += $(BIN_DIRECTORY)/org.jessies.$(MACHINE_PROJECT_NAME).deb
INSTALLERS.Linux += $(INSTALLER.deb)
# alien festoons the name with suffixes.
INSTALLER.rpm += $(BIN_DIRECTORY)/org.jessies.$(MACHINE_PROJECT_NAME)-$(VERSION_STRING)-2.i386.rpm
INSTALLERS.Linux += $(INSTALLER.rpm)

INSTALLERS = $(INSTALLERS.$(TARGET_OS))

STANDALONE_INSTALLERS.$(MACHINE_PROJECT_NAME) = $(INSTALLERS)
STANDALONE_INSTALLERS.salma-hayek =
STANDALONE_INSTALLERS = $(STANDALONE_INSTALLERS.$(MACHINE_PROJECT_NAME))

# Among its many breakages, msiexec is more restrictive about slashes than Win32.
NATIVE_NAME_FOR_INSTALLERS := '$(subst /,\,$(call convertToNativeFilenames,$(STANDALONE_INSTALLERS)))'

# We copy the files we want to install into a directory tree whose layout mimics where they'll be installed.
PACKAGING_DIRECTORY = $(PROJECT_ROOT)/.generated/native/$(TARGET_OS)/$(MACHINE_PROJECT_NAME)

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

BUILD_TARGETS += build.$(findstring java,$(SOURCE_FILES))
BUILD_TARGETS += $(if $(wildcard .svn),.generated/build-revision.txt)
TIC_SOURCE := $(wildcard lib/terminfo/*.tic)
# We deliberately omit the intermediate directory.
COMPILED_TERMINFO = $(patsubst lib/terminfo/%.tic,.generated/terminfo/%,$(TIC_SOURCE))
BUILD_TARGETS += $(COMPILED_TERMINFO)

# ----------------------------------------------------------------------------
# Variables above this point,
# rules below...
# ----------------------------------------------------------------------------

.PHONY: build
build: $(BUILD_TARGETS)

.PHONY: build.
build.:;

.PHONY: build.java
build.java: $(SOURCE_FILES)
	$(BUILD_JAVA)

.PHONY: clean
clean:
	@$(RM) -r $(GENERATED_FILES) && \
	find . -name "*.bak" | xargs $(RM)

.PHONY: native-clean
native-clean:
	@$(RM) -r .generated/native

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

$(MACHINE_PROJECT_NAME).jar: build.java
	@$(call CREATE_OR_UPDATE_JAR,c,$(CURDIR)) && \
	$(call CREATE_OR_UPDATE_JAR,u,$(SALMA_HAYEK))

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
	@mkdir -p $(@D) && \
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

.PHONY: installer-file-list
installer-file-list:
	@$(MAKE_INSTALLER_FILE_LIST)

# Unfortunately, the start-up scripts tend to go looking for salma-hayek, so we can't just have Resources/bin etc; we have to keep the multi-directory structure, at least for now.
# This isn't recursing into a sub-tree, so we don't want it to recurse if passed -n
# but passing it -k (implicitly, in MAKEFLAGS) is probably right.
.PHONY: $(MACHINE_PROJECT_NAME).app
$(MACHINE_PROJECT_NAME).app: build .generated/build-revision.txt
	@$(SCRIPT_PATH)/package-for-distribution.rb $(HUMAN_PROJECT_NAME) $(MACHINE_PROJECT_NAME) $(SALMA_HAYEK)

$(INSTALLER.dmg): $(MACHINE_PROJECT_NAME).app
	@mkdir -p $(@D) && \
	$(RM) $@ && \
	echo -n "Creating disk image..." && \
	hdiutil create -fs UFS -volname $(HUMAN_PROJECT_NAME) -srcfolder $(PACKAGING_DIRECTORY) $@

$(INSTALLER.deb): $(MACHINE_PROJECT_NAME).app
	@mkdir -p $(@D) && \
	$(RM) $@ && \
	echo -n "Creating .deb package..." && \
	fakeroot dpkg-deb --build $(PACKAGING_DIRECTORY) $@ && \
	dpkg-deb --info $@ # && dpkg-deb --contents $@

$(INSTALLER.rpm): $(INSTALLER.deb)
	@mkdir -p $(@D) && \
	$(RM) $@ && \
	echo -n "Creating .rpm package..." && \
	cd $(@D) && \
	fakeroot alien --to-rpm $<

# ----------------------------------------------------------------------------
# WiX
# ----------------------------------------------------------------------------

# Later we add more dependencies when we know $(ALL_PER_DIRECTORY_TARGETS).
%/component-definitions.wxi: $(MAKEFILE_LIST) $(FILE_LIST_TO_WXI)
	{ find classes -type f -print; $(MAKE_INSTALLER_FILE_LIST); } | $(FILE_LIST_TO_WXI) $(if $(filter %.msi,$(INSTALLER.wix)),--diskId) > $@

# This silliness is probably sufficient (as well as sadly necessary).
%/component-references.wxi: %/component-definitions.wxi $(MAKEFILE_LIST)
	ruby -w -ne '$$_.match(/Include/) && puts($$_); $$_.match(/<Component (Id='\''component\d+'\'')/) && puts("<ComponentRef #{$$1} />")' < $< > $@

%.wixobj: %.wxs .generated/build-revision.txt $(patsubst %,$(WIX_COMPILATION_DIRECTORY)/component-%.wxi,references definitions)
	HUMAN_PROJECT_NAME=$(HUMAN_PROJECT_NAME) \
	MACHINE_PROJECT_NAME=$(MACHINE_PROJECT_NAME) \
	PATH_GUID=$(makeGuid) \
	PRODUCT_GUID=$(makeGuid) \
	SHORTCUT_GUID=$(makeGuid) \
	STANDARD_FILES_GUID=$(makeGuid) \
	UPGRADE_GUID=$(UPGRADE_GUID) \
	VERSION_STRING=$(VERSION_STRING) \
	candle -nologo -out $(call convertToNativeFilenames,$@ $<)

$(INSTALLER.wix): $(WIX_COMPILATION_DIRECTORY)/$(MACHINE_PROJECT_NAME).wixobj
	light -nologo -out $(call convertToNativeFilenames,$@ $<)

$(WIX_COMPILATION_DIRECTORY)/$(MACHINE_PROJECT_NAME).wxs: $(SALMA_HAYEK)/lib/$(if $(filter %.msi,$(INSTALLER.wix)),installer,module).wxs
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
	@mkdir -p $(@D) && \
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

# Defined here because this can only be correctly evaluated after we've included per-directory.make...
ALL_PER_DIRECTORY_TARGETS = $(foreach SUBDIR,$(SUBDIRS),$(DESIRED_TARGETS.$(notdir $(SUBDIR))))

# ... and this depends on the above variable.
# FIXME: we should move .jar files into a subdirectory of the project root. lib/ or lib/jars/, maybe.
MAKE_INSTALLER_FILE_LIST = find $(wildcard doc bin lib) $(patsubst $(PROJECT_ROOT)/%,%,$(ALL_PER_DIRECTORY_TARGETS) $(filter $(PROJECT_ROOT)/%.jar,$(CLASS_PATH))) .generated/build-revision.txt $(COMPILED_TERMINFO) -name .svn -prune -o -type f -print

# The installer uses find(1) to discover what to include - so it must be built last.
# Depending on the PHONY build.java may cause the Java to be built more than
# once unless make orders the jobs to avoid that.
$(WIX_COMPILATION_DIRECTORY)/component-definitions.wxi: $(ALL_PER_DIRECTORY_TARGETS) build.java

# Presumably we need a similar dependency for non-WiX installers, which need the per-directory targets slightly later but still before the installer.
$(MACHINE_PROJECT_NAME).app: $(ALL_PER_DIRECTORY_TARGETS)

.PHONY: native
native: $(ALL_PER_DIRECTORY_TARGETS)
build: native

.PHONY: installer
installer: $(INSTALLERS)

.PHONY: native-dist
native-dist: $(addprefix upload.,$(STANDALONE_INSTALLERS))

# For WiX, we need the salma-hayek installer during the nightly build.
native-dist: $(filter %.msm,$(INSTALLERS))

# For non-WiX platforms, we still need the default salma-hayek build during the nightly build.
native-dist: build

.PHONY: upload.%
$(addprefix upload.,$(STANDALONE_INSTALLERS)): upload.%: %
	ssh $(DIST_SSH_USER_AND_HOST) mkdir -p $(DIST_DIRECTORY) && \
	scp $< $(DIST_SSH_USER_AND_HOST):$(DIST_DIRECTORY)/$(<F)

# I like the idea of keeping several versions on the server but we're going to have a hard time
# linking to the one we expect people to use unless we create a symlink.
upload.$(INSTALLER.rpm): symlink-latest.rpm
.PHONY: symlink-latest.rpm
symlink-latest.rpm: $(INSTALLER.rpm)
	ssh $(DIST_SSH_USER_AND_HOST) $(RM) $(DIST_DIRECTORY)/org.jessies.$(MACHINE_PROJECT_NAME).rpm '&&' \
	ln -s $(notdir $(INSTALLER.rpm)) $(DIST_DIRECTORY)/org.jessies.$(MACHINE_PROJECT_NAME).rpm '&&' \
	find $(DIST_DIRECTORY) -name '"*.rpm"' -mtime +7 '|' xargs $(RM)

.PHONY: install
install: $(addprefix run-installer,$(suffix $(STANDALONE_INSTALLERS)))

.PHONY: remove
remove: $(addprefix run-remover,$(suffix $(STANDALONE_INSTALLERS)))

.PHONY: run-installer
run-installer:;
.PHONY: run-remover
run-remover:;

# If we make this target dependent on $(STANDALONE_INSTALLERS), it keeps
# rebuilding it, which takes ages.
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
