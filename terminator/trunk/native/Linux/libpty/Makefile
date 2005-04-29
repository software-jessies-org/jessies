SRCS=$(wildcard ../../shared/libpty/*.cpp)
HEADERS=$(wildcard *.h)
UNAME=$(shell uname)
ARCH=$(shell arch)
LIBDIR=.
LIBRARY=$(LIBDIR)/libpty.so
JNI_PATH=`locate jni.h|tail -n 1|xargs dirname`
JNI_MD_PATH=`locate jni_md.h|tail -n 1|xargs dirname`

$(LIBRARY): $(SRCS) $(HEADERS) $(MAKEFILE_LIST)
	test -d $(LIBDIR) || mkdir $(LIBDIR)
	g++ -shared -I$(JNI_PATH) -I$(JNI_MD_PATH) -fPIC -g -W -Wall -o $(LIBRARY) $(SRCS)

.PHONY: clean
clean:
	rm -f $(LIBRARY)
