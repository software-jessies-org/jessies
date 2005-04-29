SRCS=$(wildcard ../../shared/libpty/*.cpp) Darwin_stubs.c
HEADERS=$(wildcard *.h)
UNAME=$(shell uname)
ARCH=$(shell arch)
LIBDIR=.
LIBRARY=$(LIBDIR)/libpty.jnilib

$(LIBRARY): $(SRCS) $(HEADERS) $(MAKEFILE_LIST)
	test -d $(LIBDIR) || mkdir $(LIBDIR)
	c++ -dynamiclib -I/System/Library/Frameworks/JavaVM.framework/Versions/A/Headers -fPIC -g -W -Wall -o $(LIBRARY)  $(SRCS) -framework JavaVM

.PHONY: clean
clean:
	rm -f $(OBJS) $(LIBRARY)
