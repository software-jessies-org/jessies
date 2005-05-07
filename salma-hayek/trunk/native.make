# This makefile compiles all the C/C++/Objective-C/Objective-C++ source found
# in the directory into a single executable or shared library.

# These flags, some of them traditional, are used, but you should probably refrain from
# modifying them outside this file, for portability's sake:
#
#  CFLAGS          - flags for the C/Objective-C compiler.
#  CXXFLAGS        - flags for the C++/Objective-C++ compiler.
#  C_AND_CXX_FLAGS - flags for both compilers.
#  LDFLAGS         - flags for the linker.

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
C_AND_CXXFLAGS += -W -Wall -Werror -pedantic
# ... but assume that C++ will eventually subsume C99.
CXXFLAGS += -Wno-long-long
CPPFLAGS += $(addprefix -I,$(JNI_PATH))

CFLAGS += $(C_AND_CXX_FLAGS)
CXXFLAGS += $(C_AND_CXX_FLAGS)

# ----------------------------------------------------------------------------
# Extra compiler and (mainly) linker flags for building JNI.
# ----------------------------------------------------------------------------

JAVA_HOME ?= $(error Please set $$(JAVA_HOME) (the calling Makefile should have done this for you))

JNI_PATH.Darwin += /System/Library/Frameworks/JavaVM.framework/Versions/A/Headers
SHARED_LIBRARY_LDFLAGS.Darwin += -dynamiclib -framework JavaVM
SHARED_LIBRARY_EXTENSION.Darwin = jnilib
# The default linker doesn't do the right thing on Darwin.
# This doesn't hurt on Linux, indeed it generally saves having to specify nonsense like -lstdc++.
CC = $(CXX)

JNI_PATH.Linux += $(JAVA_HOME)/include
JNI_PATH.Linux += $(JAVA_HOME)/include/linux
SHARED_LIBRARY_LDFLAGS.Linux += -shared
SHARED_LIBRARY_EXTENSION.Linux = so

JNI_PATH += $(JNI_PATH.$(TARGET_OS))
SHARED_LIBRARY_LDFLAGS += $(SHARED_LIBRARY_LDFLAGS.$(TARGET_OS))
SHARED_LIBRARY_EXTENSION = $(SHARED_LIBRARY_EXTENSION.$(TARGET_OS))

# ----------------------------------------------------------------------------
# Find the source.
# ----------------------------------------------------------------------------

SOURCE_EXTENSIONS += c
SOURCE_EXTENSIONS += cpp
SOURCE_EXTENSIONS += m
SOURCE_EXTENSIONS += mm

SOURCES := $(wildcard $(addprefix *.,$(SOURCE_EXTENSIONS)))
HEADERS := $(wildcard *.h)

# ----------------------------------------------------------------------------
# Work out what we're going to generate.
# ----------------------------------------------------------------------------

TARGET_OS := $(shell uname)

GENERATED_DIRECTORY = $(TARGET_OS)

OBJECTS = $(foreach EXTENSION,$(SOURCE_EXTENSIONS),$(patsubst %.$(EXTENSION),$(GENERATED_DIRECTORY)/%.o,$(filter %.$(EXTENSION),$(SOURCES))))
SOURCE_LINKS = $(addprefix $(GENERATED_DIRECTORY)/,$(SOURCES) $(HEADERS))

# ----------------------------------------------------------------------------
# Add the Cocoa framework if we're building Objective-C/C++.
# ----------------------------------------------------------------------------

ifneq "$(filter %.m %.mm,$(SOURCES))" ""
  LDFLAGS += -framework Cocoa
endif

# ----------------------------------------------------------------------------
# Our executable target.
# ----------------------------------------------------------------------------

$(GENERATED_DIRECTORY)/$(BASE_NAME): $(OBJECTS)

# ----------------------------------------------------------------------------
# Our shared library target.
# ----------------------------------------------------------------------------

# There is no default rule for shared library building on my system.
$(GENERATED_DIRECTORY)/$(BASE_NAME).$(SHARED_LIBRARY_EXTENSION): $(OBJECTS)
	$(LD) $(OBJECTS) -o $@ $(SHARED_LIBRARY_LDFLAGS)

# ----------------------------------------------------------------------------
# Create "the build tree", GNU-style.
# ----------------------------------------------------------------------------

# This way, we can use the built-in compilation rules which assume everything's
# in the same directory.
$(SOURCE_LINKS): $(GENERATED_DIRECTORY)/%: %
	mkdir -p $(dir $@) && \
	rm -f $@ && \
	ln -s ../$< $@

# ----------------------------------------------------------------------------
# Dependencies.
# ----------------------------------------------------------------------------

# Rather than have the compiler track dependencies we
# conservatively assume that if a header files changes, we have to recompile
# everything.
$(OBJECTS): $(SOURCE_LINKS) $(HEADERS) $(MAKEFILE_LIST)

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
