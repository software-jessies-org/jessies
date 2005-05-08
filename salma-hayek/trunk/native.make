# This makefile compiles all the C/C++/Objective-C/Objective-C++ source found
# in the directory into a single executable or JNI library.

# The decision about whether to build an executable or JNI library is made
# based on the directory name. A JNI library must be in a directory whose
# name begins "lib".

# These flags, some of them traditional, are used, but you should probably
# refrain from modifying them outside this file, for portability's sake:
#
#  CFLAGS          - flags for the C/Objective-C compiler.
#  CXXFLAGS        - flags for the C++/Objective-C++ compiler.
#  C_AND_CXX_FLAGS - flags for both compilers.
#  LDFLAGS         - flags for the linker.

# ----------------------------------------------------------------------------
# Include GNU make boilerplate.
# ----------------------------------------------------------------------------

MOST_RECENT_MAKEFILE_DIRECTORY = $(dir $(word $(words $(MAKEFILE_LIST)),$(MAKEFILE_LIST)))
include $(MOST_RECENT_MAKEFILE_DIRECTORY)/variables.make

# ----------------------------------------------------------------------------
# Choose the basename(1) for the target
# ----------------------------------------------------------------------------

BASE_NAME = $(notdir $(CURDIR))

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
# Locate Java.
# ----------------------------------------------------------------------------

JAVA_HOME ?= $(error Please set $$(JAVA_HOME) (the calling Makefile should have done this for you))

# ----------------------------------------------------------------------------
# Extra compiler and (mainly) linker flags for building JNI.
# ----------------------------------------------------------------------------

JNI_PATH.Darwin += /System/Library/Frameworks/JavaVM.framework/Versions/A/Headers
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

# ----------------------------------------------------------------------------
# Find the source.
# ----------------------------------------------------------------------------

SOURCES := $(wildcard $(addprefix *.,$(SOURCE_EXTENSIONS)))
HEADERS := $(wildcard *.h)

# ----------------------------------------------------------------------------
# Work out what we're going to generate.
# ----------------------------------------------------------------------------

GENERATED_DIRECTORY = $(TARGET_OS)

OBJECTS = $(foreach EXTENSION,$(SOURCE_EXTENSIONS),$(patsubst %.$(EXTENSION),$(GENERATED_DIRECTORY)/%.o,$(filter %.$(EXTENSION),$(SOURCES))))
SOURCE_LINKS = $(addprefix $(GENERATED_DIRECTORY)/,$(SOURCES))
HEADER_LINKS = $(addprefix $(GENERATED_DIRECTORY)/,$(HEADERS))

EXECUTABLE = $(GENERATED_DIRECTORY)/$(BASE_NAME)
SHARED_LIBRARY = $(GENERATED_DIRECTORY)/$(BASE_NAME).$(SHARED_LIBRARY_EXTENSION)

BUILDING_SHARED_LIBRARY = $(filter lib%,$(BASE_NAME))
DEFAULT_TARGET = $(if $(BUILDING_SHARED_LIBRARY),$(SHARED_LIBRARY),$(EXECUTABLE))

JNI_SOURCE = $(foreach SOURCE,$(SOURCES),$(if $(findstring _,$(SOURCE)),$(SOURCE)))
JNI_BASE_NAME = $(basename $(JNI_SOURCE))
JNI_HEADER = $(GENERATED_DIRECTORY)/$(JNI_BASE_NAME).h
JNI_OBJECT = $(GENERATED_DIRECTORY)/$(JNI_BASE_NAME).o
JNI_CLASS_NAME = $(subst _,.,$(JNI_BASE_NAME))
JNI_DIRECTORY = $(GENERATED_DIRECTORY)
CLASSES_DIRECTORY = ../../../classes
JNI_CLASS_FILE = $(CLASSES_DIRECTORY)/$(subst .,/,$(JNI_CLASS_NAME)).class

# ----------------------------------------------------------------------------
# Variables above this point,
# rules below...
# ----------------------------------------------------------------------------

# ----------------------------------------------------------------------------
# Our default target.
# ----------------------------------------------------------------------------

.PHONY: default
default: $(DEFAULT_TARGET)

# ----------------------------------------------------------------------------
# Our executable target.
# ----------------------------------------------------------------------------

$(EXECUTABLE): $(OBJECTS)

# ----------------------------------------------------------------------------
# Our shared library target.
# ----------------------------------------------------------------------------

# There is no default rule for shared library building on my system.
$(SHARED_LIBRARY): $(OBJECTS)
	$(LD) $(OBJECTS) -o $@ $(SHARED_LIBRARY_LDFLAGS)

# ----------------------------------------------------------------------------
# Generate our JNI header.
# ----------------------------------------------------------------------------

$(JNI_HEADER): $(JNI_CLASS_FILE)
	rm -f $@ && \
	$(JAVAH) -classpath $(CLASSES_DIRECTORY) -d $(JNI_DIRECTORY) $(JNI_CLASS_NAME)

$(JNI_OBJECT): $(JNI_HEADER)

# ----------------------------------------------------------------------------
# Create "the build tree", GNU-style.
# ----------------------------------------------------------------------------

# This way, we can use the built-in compilation rules which assume everything's
# in the same directory.
$(SOURCE_LINKS) $(HEADER_LINKS): $(GENERATED_DIRECTORY)/%: %
	mkdir -p $(dir $@) && \
	rm -f $@ && \
	ln -s ../$< $@

# ----------------------------------------------------------------------------
# Dependencies.
# ----------------------------------------------------------------------------

# Rather than have the compiler track dependencies we
# conservatively assume that if a header files changes, we have to recompile
# everything.
$(OBJECTS): $(HEADER_LINKS) $(HEADERS) $(MAKEFILE_LIST)

# ----------------------------------------------------------------------------
# Implicit rules for compiling Objective C and Objective C++ source.
# ----------------------------------------------------------------------------

COMPILE.m = $(COMPILE.c)
%.o: %.m
	$(COMPILE.m) $(OUTPUT_OPTION) $<

COMPILE.mm = $(COMPILE.cpp)
%.o: %.mm
	$(COMPILE.mm) $(OUTPUT_OPTION) $<

# ----------------------------------------------------------------------------
# Rules for tidying-up.
# ----------------------------------------------------------------------------

.PHONY: clean
clean:
	rm -rf $(GENERATED_DIRECTORY)

# ----------------------------------------------------------------------------
# Boilerplate rules.
# ----------------------------------------------------------------------------
	
include $(SALMA_HAYEK)/rules.make
