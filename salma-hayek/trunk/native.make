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
# Choose an executable name
# ----------------------------------------------------------------------------

EXECUTABLE_NAME = $(notdir $(CURDIR))

# ----------------------------------------------------------------------------
# Rules for compiling Objective C and Objective C++ source.
# ----------------------------------------------------------------------------

COMPILE.m = $(COMPILE.c)
%.o: %.m
	$(COMPILE.m) $(OUTPUT_OPTION) $<

COMPILE.mm = $(COMPILE.cpp)
%.o: %.mm
	$(COMPILE.mm) $(OUTPUT_OPTION) $<

# ----------------------------------------------------------------------------
# Sensible compiler flags.
# ----------------------------------------------------------------------------

C_AND_CXX_FLAGS += -g -W -Wall -Werror
CFLAGS += $(C_AND_CXX_FLAGS)
CXXFLAGS += $(C_AND_CXX_FLAGS)

CFLAGS += -std=c99

# ----------------------------------------------------------------------------
# Find the source and work out what we need to build.
# ----------------------------------------------------------------------------

SOURCE_EXTENSIONS += c
SOURCE_EXTENSIONS += cpp
SOURCE_EXTENSIONS += m
SOURCE_EXTENSIONS += mm

SOURCES := $(wildcard $(addprefix *.,$(SOURCE_EXTENSIONS)))
HEADERS := $(wildcard *.h)

OBJECTS = $(foreach EXTENSION,$(SOURCE_EXTENSIONS),$(patsubst %.$(EXTENSION),%.o,$(filter %.$(EXTENSION),$(SOURCES))))

# ----------------------------------------------------------------------------
# Add the Cocoa framework if we're building Objective-C/C++.
# ----------------------------------------------------------------------------

ifneq "$(filter %.m %.mm,$(SOURCES))" ""
  LDFLAGS += -framework Cocoa
endif

# ----------------------------------------------------------------------------
# Our targets; the executable (the default target), and "clean".
# ----------------------------------------------------------------------------

$(EXECUTABLE_NAME): $(OBJECTS)
	$(CXX) -o $(EXECUTABLE_NAME) $(LDFLAGS) $^

# Rather than track dependencies properly, or have the compiler do it, we
# conservatively assume that if a header files changes, we have to recompile
# everything.
$(OBJECTS): $(HEADERS) $(MAKEFILE_LIST)

.PHONY: clean
clean:
	rm -f $(EXECUTABLE_NAME) $(OBJECTS)
