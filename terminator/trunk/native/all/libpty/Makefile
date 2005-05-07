BASE_NAME := $(notdir $(CURDIR))
SOURCES := $(wildcard *.cpp)
HEADERS := $(wildcard *.h)

TARGET_OS := $(shell uname)
ARCH := $(shell arch)

JNI_PATH.Darwin += /System/Library/Frameworks/JavaVM.framework/Versions/A/Headers
SHARED_LIBRARY_LDFLAGS.Darwin += -dynamiclib -framework JavaVM
SHARED_LIBRARY_EXTENSION.Darwin = jnilib
# The default linker doesn't do the right thing on Darwin.
# This doesn't hurt on Linux, indeed it generally saves having to specify nonsense like -lstdc++.
LD = $(CXX)

JNI_PATH.Linux += $(if $(JAVA_HOME),,$(error Please set $$(JAVA_HOME) (the calling Makefile should have done this for you)))
JNI_PATH.Linux += $(JAVA_HOME)/include
JNI_PATH.Linux += $(JAVA_HOME)/include/linux
SHARED_LIBRARY_LDFLAGS.Linux += -shared
SHARED_LIBRARY_EXTENSION.Linux = so

JNI_PATH += $(JNI_PATH.$(TARGET_OS))
SHARED_LIBRARY_LDFLAGS += $(SHARED_LIBRARY_LDFLAGS.$(TARGET_OS))
SHARED_LIBRARY_EXTENSION = $(SHARED_LIBRARY_EXTENSION.$(TARGET_OS))

CPPFLAGS += -fPIC
CPPFLAGS += -g
# Maximum warnings...
CPPFLAGS += -W -Wall -pedantic
# ... but assume that C++ will eventually subsume C99.
CPPFLAGS += -Wno-long-long
CPPFLAGS += $(addprefix -I,$(JNI_PATH))

GENERATED_DIRECTORY = $(TARGET_OS)

OBJECTS = $(patsubst %.cpp,$(GENERATED_DIRECTORY)/%.o,$(SOURCES))
SOURCE_LINKS = $(addprefix $(GENERATED_DIRECTORY)/,$(SOURCES) $(HEADERS))

# There is no default rule for shared library building on my system.
$(GENERATED_DIRECTORY)/$(BASE_NAME).$(SHARED_LIBRARY_EXTENSION): $(OBJECTS)
	$(LD) $(OBJECTS) -o $@ $(SHARED_LIBRARY_LDFLAGS)

# Making all of the objects dependent on all of the headers is a conservative approach
# which avoids the need for dependency generation.
$(OBJECTS): $(SOURCE_LINKS) $(MAKEFILE_LIST)

# If we link to the source in the directory in which we build, GNU style,
# then we can use the built-in compilation rules which assume everything's
# in the same directory.
$(SOURCE_LINKS): $(GENERATED_DIRECTORY)/%: %
	mkdir -p $(dir $@) && \
	rm -f $@ && \
	ln -s ../$< $@

# If all the generated files are in one place, clean becomes easy.
.PHONY: clean
clean:
	rm -rf $(GENERATED_DIRECTORY)
