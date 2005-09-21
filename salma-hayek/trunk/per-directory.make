# This makefile fragment compiles all the C/C++/Objective-C/Objective-C++ source found
# in $(SOURCE_DIRECTORY) into a single executable or JNI library.

# It is only suitable for inclusion by universal.make.

# Unusually, it is included multiple times so be careful with += etc.
# Do not define any variables here which aren't dependent
# on the particular directory being built.

# ----------------------------------------------------------------------------
# Initialize any directory-specific variables we want to append to here
# ----------------------------------------------------------------------------

LOCAL_LDFLAGS := $(LDFLAGS)
MISSING_PREREQUISITES :=
EXECUTABLES :=
EXECUTABLES.Cygwin :=

# ----------------------------------------------------------------------------
# Choose the basename(1) for the target
# ----------------------------------------------------------------------------

BASE_NAME = $(notdir $(SOURCE_DIRECTORY))

# ----------------------------------------------------------------------------
# Find the source.
# ----------------------------------------------------------------------------

# BSD find's -false is a synonym for -not.  I kid you false.
FIND_FALSE = ! -prune
findCode = $(shell find $(SOURCE_DIRECTORY) -type f '(' $(foreach EXTENSION,$(1),-name "*.$(EXTENSION)" -o) $(FIND_FALSE) ')')
SOURCES := $(call findCode,$(SOURCE_EXTENSIONS))
HEADERS := $(call findCode,$(HEADER_EXTENSIONS))

# ----------------------------------------------------------------------------
# Locate the common intermediate files.
# ----------------------------------------------------------------------------

GENERATED_DIRECTORY = $(patsubst $(PROJECT_ROOT)/%,$(PROJECT_ROOT)/.generated/%/$(TARGET_OS),$(SOURCE_DIRECTORY))

$(foreach EXTENSION,$(SOURCE_EXTENSIONS),$(eval $(call defineObjectsPerLanguage,$(EXTENSION))))
OBJECTS = $(strip $(foreach EXTENSION,$(SOURCE_EXTENSIONS),$(OBJECTS.$(EXTENSION))))
SOURCE_LINKS = $(patsubst $(SOURCE_DIRECTORY)/%,$(GENERATED_DIRECTORY)/%,$(SOURCES))
HEADER_LINKS = $(patsubst $(SOURCE_DIRECTORY)/%,$(GENERATED_DIRECTORY)/%,$(HEADERS))

# ----------------------------------------------------------------------------
# Locate the executables.
# ----------------------------------------------------------------------------

EXECUTABLES += $(GENERATED_DIRECTORY)/$(BASE_NAME)$(EXE_SUFFIX)

EXECUTABLES.Cygwin += $(GENERATED_DIRECTORY)/$(BASE_NAME)w$(EXE_SUFFIX)

EXECUTABLES += $(EXECUTABLES.$(TARGET_OS))

# ----------------------------------------------------------------------------
# Locate the JNI library and its intermediate files.
# ----------------------------------------------------------------------------

JNI_LIBRARY = $(GENERATED_DIRECTORY)/$(JNI_LIBRARY_PREFIX)$(BASE_NAME).$(JNI_LIBRARY_EXTENSION)

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

define JAVAHPP_RULE
$(JAVAHPP) -classpath $(call convertCygwinToWin32Path,$(CLASSES_DIRECTORY)) $(JNI_CLASS_NAME) > $(call convertCygwinToWin32Path,$(GENERATED_JNI_HEADER)) && \
{ cmp -s $(GENERATED_JNI_HEADER) $(COMPILED_JNI_HEADER) || cp $(GENERATED_JNI_HEADER) $(COMPILED_JNI_HEADER); }
endef

# ----------------------------------------------------------------------------
# Add Cocoa frameworks if we're building Objective-C/C++.
# ----------------------------------------------------------------------------

PRIVATE_FRAMEWORKS_DIRECTORY = /System/Library/PrivateFrameworks

BUILDING_COCOA = $(filter $(OBJECTIVE_SOURCE_PATTERNS),$(SOURCES))

LOCAL_LDFLAGS += $(if $(BUILDING_COCOA),-framework Cocoa)
LOCAL_LDFLAGS += $(if $(BUILDING_COCOA),-F$(PRIVATE_FRAMEWORKS_DIRECTORY))

headerToFramework = $(PRIVATE_FRAMEWORKS_DIRECTORY)/$(basename $(notdir $(1))).framework
frameworkToLinkerFlag = -framework $(basename $(notdir $(1)))

PRIVATE_FRAMEWORK_HEADERS = $(filter $(SOURCE_DIRECTORY)/PrivateFrameworks/%,$(HEADERS))
PRIVATE_FRAMEWORKS_USED = $(foreach HEADER,$(PRIVATE_FRAMEWORK_HEADERS),$(call headerToFramework,$(HEADER)))
LOCAL_LDFLAGS += $(foreach PRIVATE_FRAMEWORK,$(PRIVATE_FRAMEWORKS_USED),$(call frameworkToLinkerFlag,$(PRIVATE_FRAMEWORK)))

MISSING_PRIVATE_FRAMEWORKS := $(filter-out $(wildcard $(PRIVATE_FRAMEWORKS_USED)),$(PRIVATE_FRAMEWORKS_USED))
MISSING_PREREQUISITES += $(MISSING_PRIVATE_FRAMEWORKS)

# ----------------------------------------------------------------------------
# Decide on the default target.
# ----------------------------------------------------------------------------

BUILDING_JNI_LIBRARY = $(JNI_SOURCE)
DESIRED_TARGETS = $(if $(BUILDING_JNI_LIBRARY),$(JNI_LIBRARY),$(EXECUTABLES))
DEFAULT_TARGETS = $(if $(strip $(MISSING_PREREQUISITES)),missing-prerequisites.$(BASE_NAME),$(DESIRED_TARGETS))

define MISSING_PREREQUISITES_RULE
  @echo "*** Can't build $(BASE_NAME) because of missing prerequisites:" && \
  $(foreach PREREQUISITE,$(MISSING_PREREQUISITES),echo "  \"$(PREREQUISITE)\"" &&) \
  true
endef

# ----------------------------------------------------------------------------
# Target-specific variables.
# These need to be assigned while the right hand side is valid so need to use :=
# That means they should be after the right hand side is finalized which means
# after other assignments.
# ----------------------------------------------------------------------------

$(EXECUTABLES.Cygwin): LOCAL_LDFLAGS += -Wl,--subsystem,windows
$(EXECUTABLES): LDFLAGS := $(LOCAL_LDFLAGS)
$(JNI_LIBRARY): LDFLAGS := $(JNI_LIBRARY_LDFLAGS)
$(GENERATED_JNI_HEADER): RULE := $(JAVAHPP_RULE)
missing-prerequisites.$(BASE_NAME): RULE := $(MISSING_PREREQUISITES_RULE)

# ----------------------------------------------------------------------------
# Variables above this point,
# rules below...
# ----------------------------------------------------------------------------

# ----------------------------------------------------------------------------
# Select the default targets.
# ----------------------------------------------------------------------------

.PHONY: build
build: $(DEFAULT_TARGETS)

# ----------------------------------------------------------------------------
# Our linked targets.
# ----------------------------------------------------------------------------

$(EXECUTABLES) $(JNI_LIBRARY): $(OBJECTS)
	$(LD) $^ -o $@ $(LDFLAGS)

# ----------------------------------------------------------------------------
# Generate our JNI header.
# ----------------------------------------------------------------------------

ifneq "$(JNI_SOURCE)" ""

$(GENERATED_JNI_HEADER): $(JNI_CLASS_FILE) $(JAVAHPP) $(SALMA_HAYEK)/classes/e/tools/JavaHpp.class
	@echo Generating JNI header... && \
	mkdir -p $(@D) && \
	rm -f $@ && \
	$(RULE)

$(JNI_OBJECT): $(COMPILED_JNI_HEADER)
build: $(GENERATED_JNI_HEADER)
$(COMPILED_JNI_HEADER): | $(GENERATED_JNI_HEADER);

endif

# ----------------------------------------------------------------------------
# Rules for compiling Objective C and Objective C++ source.
# ----------------------------------------------------------------------------

$(OBJECTS.m): %.o: %.m
	$(COMPILE.m) $(OUTPUT_OPTION) $<

$(OBJECTS.mm): %.o: %.mm
	$(COMPILE.mm) $(OUTPUT_OPTION) $<

# ----------------------------------------------------------------------------
# What to do if something we need isn't installed but we want to continue building everything else.
# ----------------------------------------------------------------------------

.PHONY: missing-prerequisites.$(BASE_NAME)
missing-prerequisites.$(BASE_NAME):
	$(RULE)

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
