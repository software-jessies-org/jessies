SRCS=$(wildcard *.cpp)
OBJS=$(patsubst %.cpp,%.o,$(SRCS))
UNAME=$(shell uname)
ARCH=$(shell arch)
BINARY=pty-$(UNAME)-$(ARCH)

$(BINARY): $(SRCS)
	g++ -g -W -Wall -o $@ $^

.PHONY: clean
clean:
	rm -f $(OBJS) $(BINARY)
