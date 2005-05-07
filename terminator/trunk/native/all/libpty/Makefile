SRCS=$(wildcard *.cpp)
HEADERS=$(wildcard *.h)
TARGET_OS=$(shell uname)
ARCH=$(shell arch)

ifeq "$(TARGET_OS)" "Darwin"
    JNI_PATHS=-I/System/Library/Frameworks/JavaVM.framework/Versions/A/Headers
    COMPILER=c++ -dynamiclib -framework JavaVM
    LIBRARY_EXTENSION=jnilib
else
    ifeq "$(JAVA_HOME)" ""
        $(error Please set $$(JAVA_HOME) (the calling Makefile should have done this for you))
    endif

    JNI_PATHS:=-I$(JAVA_HOME)/include -I$(JAVA_HOME)/include/linux
    COMPILER=g++ -shared
    LIBRARY_EXTENSION=so
endif

LIB_DIR=$(TARGET_OS)
LIBRARY=$(LIB_DIR)/libpty.$(LIBRARY_EXTENSION)


$(LIBRARY): $(SRCS) $(HEADERS) $(MAKEFILE_LIST)
	mkdir -p $(LIB_DIR) && \
    $(COMPILER) $(JNI_PATHS) -fPIC -g -W -Wall -pedantic -Wno-long-long -o $(LIBRARY) $(SRCS)

.PHONY: clean
clean:
	rm -f $(OBJS) $(LIBRARY)
