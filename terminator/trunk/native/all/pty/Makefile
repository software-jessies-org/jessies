SRCS=$(wildcard *.cpp)
OBJS=$(patsubst %.cpp,%.o,$(SRCS))
HEADERS=$(wildcard *.h)
UNAME=$(shell uname)
ARCH=$(shell arch)
BINARY=pty-$(UNAME)-$(ARCH)

$(BINARY): $(SRCS) $(HEADERS) $(MAKEFILE_LIST)
	g++ -g -W -Wall -o $@ $(SRCS)

.PHONY: clean
clean:
	rm -f $(OBJS) $(BINARY)
