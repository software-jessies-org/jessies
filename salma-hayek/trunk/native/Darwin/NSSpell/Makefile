COMPILE.m = $(COMPILE.c)
.SUFFIXES: .o .m
.m.o:
	$(COMPILE.m) $(OUTPUT_OPTION) $<

COMPILE.mm = $(COMPILE.cpp)
.SUFFIXES: .o .mm
.mm.o:
	$(COMPILE.mm) $(OUTPUT_OPTION) $<

C_AND_CXX_FLAGS = -g -W -Wall -Werror
CFLAGS += $(C_AND_CXX_FLAGS)
CXXFLAGS += $(C_AND_CXX_FLAGS)

CFLAGS += -std=c99
LDFLAGS += -framework Cocoa

OBJS += $(patsubst %.cpp,%.o,$(wildcard *.cpp))
OBJS += $(patsubst %.mm,%.o,$(wildcard *.mm))
OBJS += $(patsubst %.m,%.o,$(wildcard *.m))

CURRENT_DIRECTORY := $(shell pwd)
EXECUTABLE_NAME := $(shell basename $(CURRENT_DIRECTORY))

$(EXECUTABLE_NAME): $(OBJS)
	$(CXX) -o $(EXECUTABLE_NAME) $(LDFLAGS) $^

.PHONY: clean
clean:
	rm -f $(EXECUTABLE_NAME) $(OBJS)
