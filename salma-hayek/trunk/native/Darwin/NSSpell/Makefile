COMPILE.m = $(COMPILE.c)
.SUFFIXES: .o .m
.m.o:
	$(COMPILE.m) $(OUTPUT_OPTION) $<

CFLAGS += -g -W -Wall -Werror -std=c99
LDFLAGS += -framework AppKit

OBJS += $(patsubst %.cpp,%.o,$(wildcard *.cpp))
OBJS += $(patsubst %.m,%.o,$(wildcard *.m))

CURRENT_DIRECTORY := $(shell pwd)
EXECUTABLE_NAME := $(shell basename $(CURRENT_DIRECTORY))

$(EXECUTABLE_NAME): $(OBJS)
	$(CXX) -o $(EXECUTABLE_NAME) $(LDFLAGS) $^

.PHONY: clean
clean:
	rm -f $(EXECUTABLE_NAME) $(OBJS)
