# This makefile is included at the start of other makefiles.

# ----------------------------------------------------------------------------
# Disable legacy make behavior.
# ----------------------------------------------------------------------------

# We used to disable suffix rules, but the default compilation rules are suffix
# rules, and we want to use them in "native.make".
#.SUFFIXES:

.DEFAULT:
.DELETE_ON_ERROR:
.SECONDARY:

# ----------------------------------------------------------------------------
# Define useful stuff not provided by GNU make.
# ----------------------------------------------------------------------------

pathsearch = $(firstword $(wildcard $(addsuffix /$(1),$(subst :, ,$(PATH)))))
makepath = $(subst $(SPACE),:,$(strip $(1)))
getAbsolutePath = $(patsubst @%,$(CURDIR)/%,$(patsubst @/%,/%,$(patsubst %,@%,$(1))))

SPACE := $(subst :, ,:)

# ----------------------------------------------------------------------------
# Locate salma-hayek.
# ----------------------------------------------------------------------------

SALMA_HAYEK := $(call getAbsolutePath,$(MOST_RECENT_MAKEFILE_DIRECTORY))
export SALMA_HAYEK

# ----------------------------------------------------------------------------
# Locate Java.
# ----------------------------------------------------------------------------

# Assume the tools are on the path if $(JAVA_HOME) isn't specified.
# Note the := to evaluate $(JAVA_HOME) before native.make defaults it to a $(error).
JAVA_PATH := $(if $(JAVA_HOME),$(JAVA_HOME)/bin/)

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

CFLAGS += -std=c99
C_AND_CXXFLAGS += -fPIC
C_AND_CXXFLAGS += -g
# Maximum warnings...
C_AND_CXX_FLAGS += -W -Wall -Werror -pedantic
# ... but assume that C++ will eventually subsume C99.
CXXFLAGS += -Wno-long-long
CPPFLAGS += $(addprefix -I,$(JNI_PATH))

CFLAGS += $(C_AND_CXX_FLAGS)
CXXFLAGS += $(C_AND_CXX_FLAGS)

# ----------------------------------------------------------------------------
# Extra compiler and (mainly) linker flags for building JNI.
# ----------------------------------------------------------------------------

JNI_PATH.Darwin += /System/Library/Frameworks/JavaVM.framework/Versions/Current/Headers
SHARED_LIBRARY_LDFLAGS.Darwin += -dynamiclib -framework JavaVM
SHARED_LIBRARY_EXTENSION.Darwin = jnilib
# The default $(LD) doesn't know about -dynamiclib on Darwin.
# This doesn't hurt on Linux, indeed it generally saves having to specify nonsense like -lstdc++.
LD = $(CXX)
# The default $(CC) used by $(LINK.o) doesn't know about the Darwin equivalent of -lstdc++.
CC = $(CXX)

JNI_PATH.Linux += $(JAVA_HOME)/include
JNI_PATH.Linux += $(JAVA_HOME)/include/linux
SHARED_LIBRARY_LDFLAGS.Linux += -shared
SHARED_LIBRARY_EXTENSION.Linux = so

JNI_PATH += $(JNI_PATH.$(TARGET_OS))
SHARED_LIBRARY_LDFLAGS += $(SHARED_LIBRARY_LDFLAGS.$(TARGET_OS))
SHARED_LIBRARY_EXTENSION = $(SHARED_LIBRARY_EXTENSION.$(TARGET_OS))

# ----------------------------------------------------------------------------
# Add the Cocoa framework if we're building Objective-C/C++.
# ----------------------------------------------------------------------------

BUILDING_COCOA = $(filter %.m %.mm,$(SOURCES))

LDFLAGS += $(if $(BUILDING_COCOA),-framework Cocoa)
