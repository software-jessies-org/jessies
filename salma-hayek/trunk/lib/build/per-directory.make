# This makefile fragment compiles all the C/C++/Objective-C/Objective-C++ source found
# in $(SOURCE_DIRECTORY) into a single executable or JNI library.

# It is only suitable for inclusion by universal.make.

# Unusually, it is included multiple times so be careful with += etc.
# Do not define any variables here which aren't dependent
# on the particular directory being built.

# ----------------------------------------------------------------------------
# Choose the basename(1) for the target
# This is used in reporting scoping errors, so should be defined first.
# ----------------------------------------------------------------------------

BASE_NAME = $(notdir $(SOURCE_DIRECTORY))

# ----------------------------------------------------------------------------
# Initialize any directory-specific variables we want to append to here
# ----------------------------------------------------------------------------

BUILDING_SHARED_LIBRARY =
LOCAL_C_AND_CXX_FLAGS.$(TARGET_OS) =
LOCAL_C_AND_CXX_FLAGS = $(LOCAL_C_AND_CXX_FLAGS.$(TARGET_OS))
LOCAL_LDFLAGS.$(TARGET_OS) =
LOCAL_LDFLAGS = $(LOCAL_LDFLAGS.$(TARGET_OS))
MISSING_PREREQUISITES =

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

COMPILATION_DIRECTORY = $(patsubst $(PROJECT_ROOT)/%,$(PROJECT_ROOT)/.generated/%/$(TARGET_DIRECTORY),$(SOURCE_DIRECTORY))

$(foreach EXTENSION,$(SOURCE_EXTENSIONS),$(eval $(call defineObjectsPerLanguage,$(EXTENSION))))
OBJECTS = $(strip $(foreach EXTENSION,$(SOURCE_EXTENSIONS),$(OBJECTS.$(EXTENSION))))
SOURCE_LINKS = $(patsubst $(SOURCE_DIRECTORY)/%,$(COMPILATION_DIRECTORY)/%,$(SOURCES))
HEADER_LINKS = $(patsubst $(SOURCE_DIRECTORY)/%,$(COMPILATION_DIRECTORY)/%,$(HEADERS))

# ----------------------------------------------------------------------------
# Locate the executables.
# ----------------------------------------------------------------------------

EXECUTABLES = $(BIN_DIRECTORY)/$(BASE_NAME)$(EXE_SUFFIX)

# ----------------------------------------------------------------------------
# Locate the JNI library and its intermediate files.
# ----------------------------------------------------------------------------

# $(foreach) generates a space-separated list even where the elements either side are empty strings.
# $(strip) removes spurious spaces.
JNI_SOURCE = $(strip $(foreach SOURCE,$(SOURCES),$(if $(wildcard src/$(subst _,/,$(basename $(notdir $(SOURCE)))).java),$(SOURCE))))
JNI_BASE_NAME = $(basename $(notdir $(JNI_SOURCE)))
NEW_JNI_HEADER = $(COMPILATION_DIRECTORY)/new/$(JNI_BASE_NAME).h
JNI_HEADER = $(COMPILATION_DIRECTORY)/$(JNI_BASE_NAME).h
JNI_OBJECT = $(COMPILATION_DIRECTORY)/$(JNI_BASE_NAME).o
JNI_CLASS_NAME = $(subst _,.,$(JNI_BASE_NAME))

BUILDING_JNI = $(JNI_SOURCE)

# Cocoa won't build or link for x86_64 on Mac OS X 10.4 and we use Cocoa freely except in JNI code.
# John Russell and Andy Miller both wanted to use Terminator on 10.5 with that platform's x86_64-only JDK 6.
LOCAL_C_AND_CXX_FLAGS.Darwin += $(if $(BUILDING_JNI),-arch x86_64,-x objective-c++)
LOCAL_LDFLAGS.Darwin += $(if $(BUILDING_JNI),-arch x86_64 -framework JavaVM)

# ----------------------------------------------------------------------------
# Build shared libraries.
# ----------------------------------------------------------------------------

LOCAL_SHARED_LIBRARY_EXTENSION = $(if $(BUILDING_JNI),$(JNI_LIBRARY_EXTENSION),$(SHARED_LIBRARY_EXTENSION))
POTENTIAL_SHARED_LIBRARY = $(LIB_DIRECTORY)/$(patsubst liblib%,lib%,$(SHARED_LIBRARY_PREFIX)$(BASE_NAME)).$(LOCAL_SHARED_LIBRARY_EXTENSION)

BUILDING_SHARED_LIBRARY += $(filter lib%,$(notdir $(SOURCE_DIRECTORY)))
BUILDING_SHARED_LIBRARY += $(BUILDING_JNI)

LOCAL_LDFLAGS += $(if $(strip $(BUILDING_SHARED_LIBRARY)),$(SHARED_LIBRARY_LDFLAGS))
SHARED_LIBRARY = $(if $(strip $(BUILDING_SHARED_LIBRARY)),$(POTENTIAL_SHARED_LIBRARY))

# ----------------------------------------------------------------------------
# Add Cocoa frameworks if we're building Objective-C/C++.
# ----------------------------------------------------------------------------

PRIVATE_FRAMEWORKS_DIRECTORY = /System/Library/PrivateFrameworks

BUILDING_COCOA = $(filter $(OBJECTIVE_SOURCE_PATTERNS),$(SOURCES))

LOCAL_LDFLAGS += $(if $(BUILDING_COCOA),-F$(PRIVATE_FRAMEWORKS_DIRECTORY))

headerToFramework = $(PRIVATE_FRAMEWORKS_DIRECTORY)/$(basename $(notdir $(1))).framework
frameworkToLinkerFlag = -framework $(basename $(notdir $(1)))

PRIVATE_FRAMEWORK_HEADERS = $(filter $(SOURCE_DIRECTORY)/PrivateFrameworks/%,$(HEADERS))
PRIVATE_FRAMEWORKS_USED = $(foreach HEADER,$(PRIVATE_FRAMEWORK_HEADERS),$(call headerToFramework,$(HEADER)))
LOCAL_LDFLAGS += $(foreach PRIVATE_FRAMEWORK,$(PRIVATE_FRAMEWORKS_USED),$(call frameworkToLinkerFlag,$(PRIVATE_FRAMEWORK)))

MISSING_PRIVATE_FRAMEWORKS := $(filter-out $(wildcard $(PRIVATE_FRAMEWORKS_USED)),$(PRIVATE_FRAMEWORKS_USED))
MISSING_PREREQUISITES += $(MISSING_PRIVATE_FRAMEWORKS)

# ----------------------------------------------------------------------------
# Extra linker flags for building for Windows without Cygwin.
# ----------------------------------------------------------------------------

BUILDING_MINGW = $(filter $(CURDIR)/native/Mingw/%,$(SOURCE_DIRECTORY))

LOCAL_LDFLAGS += $(if $(BUILDING_MINGW),$(MINGW_FLAGS.$(MINGW_COMPILER)))

# ----------------------------------------------------------------------------
# Post linking changes.
# ----------------------------------------------------------------------------

NEEDS_SETUID.Linux = $(shell grep -w pututxline $(SOURCES))
NEEDS_SETUID := $(NEEDS_SETUID.$(TARGET_OS))
LOCAL_LDFLAGS += $(if $(NEEDS_SETUID),&& sudo chown root: $(EXECUTABLES) && sudo chmod u+s,a+rx $(EXECUTABLES))

# ----------------------------------------------------------------------------
# Decide on the default target.
# ----------------------------------------------------------------------------

DESIRED_TARGETS = $(if $(strip $(SHARED_LIBRARY)),$(SHARED_LIBRARY),$(EXECUTABLES))
DEFAULT_TARGETS = $(if $(strip $(MISSING_PREREQUISITES)),missing-prerequisites.$(BASE_NAME),$(DESIRED_TARGETS))

define MISSING_PREREQUISITES_RULE
  @echo "*** Can't build $(BASE_NAME) because of missing prerequisites:" && \
  $(foreach PREREQUISITE,$(MISSING_PREREQUISITES),echo "  \"$(PREREQUISITE)\"" &&) \
  true
endef

# ----------------------------------------------------------------------------
# Target-specific variables.
# These need to be assigned while the right hand side is valid so need to use :=
# (or $(eval)).
# That means they should be after the right hand side is finalized which means
# after other assignments.
# ----------------------------------------------------------------------------

# LOCAL_C_AND_CXX_FLAGS is evaluated here, so it can refer to local variables but not automatic ones.
$(eval $(OBJECTS): C_AND_CXX_FLAGS += $(LOCAL_C_AND_CXX_FLAGS))
# LOCAL_LDFLAGS is evaluated here, so it can refer to local variables but not automatic ones.
$(eval $(EXECUTABLES) $(SHARED_LIBRARY): LDFLAGS += $(LOCAL_LDFLAGS))
$(NEW_JNI_HEADER): JNI_BASE_NAME := $(JNI_BASE_NAME)
$(NEW_JNI_HEADER): JNI_CLASS_NAME := $(JNI_CLASS_NAME)
missing-prerequisites.$(BASE_NAME): RULE := $(MISSING_PREREQUISITES_RULE)

# ----------------------------------------------------------------------------
# Variables above this point,
# rules below...
# ----------------------------------------------------------------------------

# ----------------------------------------------------------------------------
# Our linked targets.
# ----------------------------------------------------------------------------

$(EXECUTABLES) $(SHARED_LIBRARY): $(OBJECTS)
	@echo "-- Linking $(notdir $@)..."
	mkdir -p $(@D) && \
	$(LD) $^ -o $@ $(LDFLAGS)

# ----------------------------------------------------------------------------
# Generate our JNI header.
# ----------------------------------------------------------------------------

ifneq "$(JNI_SOURCE)" ""

$(NEW_JNI_HEADER): $(PROJECT_ROOT)/.generated/java.build-finished $(JAVAHPP) $(SALMA_HAYEK)/.generated/java.build-finished $(SALMA_HAYEK)/.generated/$(TARGET_DIRECTORY)/bin/java-launcher$(EXE_SUFFIX)
	@echo "-- Generating JNI header $(JNI_BASE_NAME).h..."
	mkdir -p $(@D) && \
	$(RM) $@ && \
	$(JAVAHPP) -classpath .generated/classes $(JNI_CLASS_NAME) > $@.tmp && \
	mv $@.tmp $@

$(JNI_OBJECT): $(JNI_HEADER)
build: $(NEW_JNI_HEADER)
$(JNI_HEADER): $(NEW_JNI_HEADER);
	{ cmp -s $< $@ || { cp $< $@.tmp && mv $@.tmp $@; }; }

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

# This way, we can use compilation rules which assume everything's
# in the same directory.
# FIXME: Do dangling links need to be removed?
$(SOURCE_LINKS) $(HEADER_LINKS): $(COMPILATION_DIRECTORY)/%: $(SOURCE_DIRECTORY)/%
	$(SYMLINK_RULE)

# ----------------------------------------------------------------------------
# Dependencies.
# ----------------------------------------------------------------------------

# Rather than have the compiler track dependencies we
# conservatively assume that if a header files changes, we have to recompile
# everything.
$(OBJECTS): $(HEADER_LINKS) $(HEADERS) $(MAKEFILE_LIST)
$(OBJECTS): $(wildcard $(SALMA_HAYEK)/native/Headers/*)
