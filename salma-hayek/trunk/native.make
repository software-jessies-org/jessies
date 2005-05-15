# This makefile compiles all the C/C++/Objective-C/Objective-C++ source found
# in $(SOURCE_DIRECTORY) into a single executable or JNI library.

# It is only suitable for inclusion by java.make.

# Unusually, it is included multiple times so be careful with += etc.
# As a rule of thumb, do not define any variables here which aren't dependent
# on the particular directory being built.

# ----------------------------------------------------------------------------
# Initialize any directory-specific variables we want to append to here
# ----------------------------------------------------------------------------

LOCAL_LDFLAGS := $(LDFLAGS)

# ----------------------------------------------------------------------------
# Choose the basename(1) for the target
# ----------------------------------------------------------------------------

BASE_NAME = $(notdir $(SOURCE_DIRECTORY))

# ----------------------------------------------------------------------------
# Find the source.
# ----------------------------------------------------------------------------

# BSD find's -false is a synonym for -not.  I kid you false.
FIND_FALSE = -not -prune
findCode = $(shell find $(SOURCE_DIRECTORY) -type f '(' $(foreach EXTENSION,$(1),-name "*.$(EXTENSION)" -or) $(FIND_FALSE) ')')
SOURCES := $(call findCode,$(SOURCE_EXTENSIONS))
HEADERS := $(call findCode,$(HEADER_EXTENSIONS))

# ----------------------------------------------------------------------------
# Work out what we're going to generate.
# ----------------------------------------------------------------------------

GENERATED_DIRECTORY = $(patsubst $(PROJECT_ROOT)/%,$(PROJECT_ROOT)/generated/%/$(TARGET_OS),$(SOURCE_DIRECTORY))

OBJECTS = $(foreach EXTENSION,$(SOURCE_EXTENSIONS),$(patsubst $(SOURCE_DIRECTORY)/%.$(EXTENSION),$(GENERATED_DIRECTORY)/%.o,$(filter %.$(EXTENSION),$(SOURCES))))
SOURCE_LINKS = $(patsubst $(SOURCE_DIRECTORY)/%,$(GENERATED_DIRECTORY)/%,$(SOURCES))
HEADER_LINKS = $(patsubst $(SOURCE_DIRECTORY)/%,$(GENERATED_DIRECTORY)/%,$(HEADERS))

EXECUTABLE = $(GENERATED_DIRECTORY)/$(BASE_NAME)
JNI_LIBRARY = $(GENERATED_DIRECTORY)/$(JNI_LIBRARY_PREFIX)$(BASE_NAME).$(JNI_LIBRARY_EXTENSION)

BUILDING_JNI_LIBRARY = $(JNI_SOURCE)
DEFAULT_TARGET = $(if $(BUILDING_JNI_LIBRARY),$(JNI_LIBRARY),$(EXECUTABLE))

# $(foreach) generates a space-separated list even where the elements either side are empty strings.
# $(strip) removes spurious spaces.
JNI_SOURCE = $(strip $(foreach SOURCE,$(SOURCES),$(if $(findstring _,$(SOURCE)),$(SOURCE))))
JNI_BASE_NAME = $(basename $(notdir $(JNI_SOURCE)))
GENERATED_JNI_DIRECTORY = $(GENERATED_DIRECTORY)/jni
GENERATED_JNI_HEADER = $(GENERATED_JNI_DIRECTORY)/$(JNI_BASE_NAME).h
COMPILED_JNI_HEADER = $(GENERATED_DIRECTORY)/$(JNI_BASE_NAME).h
JNI_OBJECT = $(GENERATED_DIRECTORY)/$(JNI_BASE_NAME).o
JNI_CLASS_NAME = $(subst _,.,$(JNI_BASE_NAME))
CLASSES_DIRECTORY = $(PROJECT_ROOT)/classes
JNI_CLASS_FILE = $(CLASSES_DIRECTORY)/$(subst .,/,$(JNI_CLASS_NAME)).class

# ----------------------------------------------------------------------------
# Add Cocoa frameworks if we're building Objective-C/C++.
# ----------------------------------------------------------------------------

PRIVATE_FRAMEWORKS_DIRECTORY = /System/Library/PrivateFrameworks

BUILDING_COCOA = $(filter %.m %.mm,$(SOURCES))

LOCAL_LDFLAGS += $(if $(BUILDING_COCOA),-framework Cocoa)
LOCAL_LDFLAGS += $(if $(BUILDING_COCOA),-F$(PRIVATE_FRAMEWORKS_DIRECTORY))
# TODO: This should come out if the Slideshow interface can be abstracted-out into
# native/Darwin/MacSlideshow/PrivateFrameworks/Slideshow.h.
LOCAL_LDFLAGS += $(if $(BUILDING_COCOA),-framework Slideshow)

headerToFramework = $(PRIVATE_FRAMEWORKS_DIRECTORY)/$(basename $(notdir $(1))).framework
frameworkToLinkerFlag = -framework $(basename $(notdir $(1)))

PRIVATE_FRAMEWORK_HEADERS = $(filter $(SOURCE_DIRECTORY)/PrivateFrameworks/%,$(HEADERS))
PRIVATE_FRAMEWORKS_USED = $(wildcard $(foreach HEADER,$(PRIVATE_FRAMEWORK_HEADERS),$(call headerToFramework,$(HEADER))))
LOCAL_LDFLAGS += $(foreach PRIVATE_FRAMEWORK,$(PRIVATE_FRAMEWORKS_USED),$(call frameworkToLinkerFlag,$(PRIVATE_FRAMEWORK)))

# ----------------------------------------------------------------------------
# Target-specific variables.
# These need to be assigned while the right hand side is valid so need to use :=
# That means they should be after the right hand side is finalized which means
# after other assignments.
# ----------------------------------------------------------------------------

$(EXECUTABLE): LDFLAGS := $(LOCAL_LDFLAGS)

# ----------------------------------------------------------------------------
# Variables above this point,
# rules below...
# ----------------------------------------------------------------------------

# ----------------------------------------------------------------------------
# Select the default target.
# ----------------------------------------------------------------------------

.PHONY: build
build: $(DEFAULT_TARGET)

# ----------------------------------------------------------------------------
# Our executable target.
# ----------------------------------------------------------------------------

$(EXECUTABLE): $(OBJECTS)

# ----------------------------------------------------------------------------
# Our JNI library target.
# ----------------------------------------------------------------------------

# There is no default rule for shared library building on my system.
$(JNI_LIBRARY): $(OBJECTS)
	$(LD) $(OBJECTS) -o $@ $(JNI_LIBRARY_LDFLAGS)

# ----------------------------------------------------------------------------
# Generate our JNI header.
# ----------------------------------------------------------------------------

ifneq "$(JNI_SOURCE)" ""

$(GENERATED_JNI_HEADER): $(JNI_CLASS_FILE)
	@echo Generating JNI header... && \
	mkdir -p $(@D) && \
	rm -f $@ && \
	"$(JAVAH)" -classpath $(call convertCygwinToWin32Path,$(CLASSES_DIRECTORY)) -d $(call convertCygwinToWin32Path,$(GENERATED_JNI_DIRECTORY)) $(JNI_CLASS_NAME) && \
	{ cmp --quiet $(GENERATED_JNI_HEADER) $(COMPILED_JNI_HEADER) || cp $(GENERATED_JNI_HEADER) $(COMPILED_JNI_HEADER); }

$(JNI_OBJECT): $(COMPILED_JNI_HEADER)
build: $(GENERATED_JNI_HEADER)
$(COMPILED_JNI_HEADER): | $(GENERATED_JNI_HEADER);

endif

# ----------------------------------------------------------------------------
# Create "the build tree", GNU-style.
# ----------------------------------------------------------------------------

# This way, we can use the built-in compilation rules which assume everything's
# in the same directory.
$(SOURCE_LINKS) $(HEADER_LINKS): $(GENERATED_DIRECTORY)/%: $(SOURCE_DIRECTORY)/%
	mkdir -p $(dir $@) && \
	rm -f $@ && \
	ln -s $< $@

# ----------------------------------------------------------------------------
# Dependencies.
# ----------------------------------------------------------------------------

# Rather than have the compiler track dependencies we
# conservatively assume that if a header files changes, we have to recompile
# everything.
$(OBJECTS): $(HEADER_LINKS) $(HEADERS) $(MAKEFILE_LIST)

# ----------------------------------------------------------------------------
# Rules for tidying-up.
# ----------------------------------------------------------------------------

.PHONY: clean
clean: clean.$(GENERATED_DIRECTORY)

.PHONY: clean.$(GENERATED_DIRECTORY)
clean.$(GENERATED_DIRECTORY):
	rm -rf $(GENERATED_DIRECTORY)
