# FIXME: ...default to guessing an executable name?
#CURRENT_DIRECTORY := $(shell pwd)
#EXECUTABLE_NAME ?= $(shell basename $(CURRENT_DIRECTORY))

# ----------------------------------------------------------------------------
# Rules for compiling Objective C and Objective C++ source.
# ----------------------------------------------------------------------------

COMPILE.m = $(COMPILE.c)
.SUFFIXES: .o .m
.m.o:
	$(COMPILE.m) $(OUTPUT_OPTION) $<

COMPILE.mm = $(COMPILE.cpp)
.SUFFIXES: .o .mm
.mm.o:
	$(COMPILE.mm) $(OUTPUT_OPTION) $<

# ----------------------------------------------------------------------------
# Sensible compiler/linker flags.
# ----------------------------------------------------------------------------

# Hack around include paths by putting all directories on the path.
SOURCE_DIRECTORIES=$(shell find `pwd`/src -type d)
C_AND_CXX_FLAGS += $(patsubst %,-I%,$(SOURCE_DIRECTORIES))

C_AND_CXX_FLAGS += -g -W -Wall #-Werror
CFLAGS += $(C_AND_CXX_FLAGS)
CXXFLAGS += $(C_AND_CXX_FLAGS)

CFLAGS += -std=c99
LDFLAGS += -framework Cocoa

# ----------------------------------------------------------------------------
# Find the source and work out what we need to build.
# ----------------------------------------------------------------------------

SOURCE_FILES=$(shell find `pwd`/src -type f -name "*.c" -or -name "*.cpp" -or -name "*.mm" -or -name "*.m")
OBJS += $(patsubst %.c,%.o,$(patsubst %.cpp,%.o,$(patsubst %.mm,%.o,$(patsubst %.m,%.o,$(SOURCE_FILES)))))

# ----------------------------------------------------------------------------
# Our targets; the executable (the default target), and "clean".
# ----------------------------------------------------------------------------

$(EXECUTABLE_NAME): $(OBJS)
	$(CXX) -o $(EXECUTABLE_NAME) $(LDFLAGS) $^

.PHONY: clean
clean:
	rm -f $(EXECUTABLE_NAME) $(OBJS)
