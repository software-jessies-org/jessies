SRCS=$(wildcard *.cpp)
HEADERS=$(wildcard *.h)
TARGET_OS=$(shell uname)
ARCH=$(shell arch)


ifeq "$(TARGET_OS)" "Darwin"
	JNI_PATHS=-I/System/Library/Frameworks/JavaVM.framework/Versions/A/Headers
	COMPILER=c++ -dynamiclib -framework JavaVM
	LIBRARY_EXTENSION=jnilib
else
	JNI_PATHS:=-I$(shell locate jni.h|tail -n 1|xargs dirname) -I$(shell locate jni_md.h|tail -n 1|xargs dirname)
	COMPILER=g++ -shared
	LIBRARY_EXTENSION=so
endif

LIB_DIR=$(TARGET_OS)
LIBRARY=$(LIB_DIR)/libpty.$(LIBRARY_EXTENSION)


$(LIBRARY): $(SRCS) $(HEADERS) $(MAKEFILE_LIST)
	test -d $(LIB_DIR) || mkdir $(LIB_DIR)
	$(COMPILER) $(JNI_PATHS) -fPIC -g -W -Wall -o $(LIBRARY) $(SRCS)

.PHONY: clean
clean:
	rm -f $(OBJS) $(LIBRARY)
