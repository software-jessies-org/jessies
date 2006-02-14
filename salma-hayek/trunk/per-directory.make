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

COMPILATION_DIRECTORY = $(patsubst $(PROJECT_ROOT)/%,$(PROJECT_ROOT)/.generated/%/$(TARGET_OS),$(SOURCE_DIRECTORY))
BIN_DIRECTORY = $(PROJECT_ROOT)/.generated/$(TARGET_OS)/bin
LIB_DIRECTORY = $(PROJECT_ROOT)/.generated/$(TARGET_OS)/lib

$(foreach EXTENSION,$(SOURCE_EXTENSIONS),$(eval $(call defineObjectsPerLanguage,$(EXTENSION))))
OBJECTS = $(strip $(foreach EXTENSION,$(SOURCE_EXTENSIONS),$(OBJECTS.$(EXTENSION))))
SOURCE_LINKS = $(patsubst $(SOURCE_DIRECTORY)/%,$(COMPILATION_DIRECTORY)/%,$(SOURCES))
HEADER_LINKS = $(patsubst $(SOURCE_DIRECTORY)/%,$(COMPILATION_DIRECTORY)/%,$(HEADERS))

# ----------------------------------------------------------------------------
# Locate the executables.
# ----------------------------------------------------------------------------

EXECUTABLES += $(BIN_DIRECTORY)/$(BASE_NAME)$(EXE_SUFFIX)

EXECUTABLES.Cygwin += $(BIN_DIRECTORY)/$(BASE_NAME)w$(EXE_SUFFIX)

EXECUTABLES += $(EXECUTABLES.$(TARGET_OS))

# ----------------------------------------------------------------------------
# Locate the JNI library and its intermediate files.
# ----------------------------------------------------------------------------

JNI_LIBRARY = $(LIB_DIRECTORY)/$(JNI_LIBRARY_PREFIX)$(BASE_NAME).$(JNI_LIBRARY_EXTENSION)

# $(foreach) generates a space-separated list even where the elements either side are empty strings.
# $(strip) removes spurious spaces.
JNI_SOURCE = $(strip $(foreach SOURCE,$(SOURCES),$(if $(wildcard src/$(subst _,/,$(basename $(notdir $(SOURCE)))).java),$(SOURCE))))
JNI_BASE_NAME = $(basename $(notdir $(JNI_SOURCE)))
NEW_JNI_HEADER = $(COMPILATION_DIRECTORY)/new/$(JNI_BASE_NAME).h
JNI_HEADER = $(COMPILATION_DIRECTORY)/$(JNI_BASE_NAME).h
JNI_OBJECT = $(COMPILATION_DIRECTORY)/$(JNI_BASE_NAME).o
JNI_CLASS_NAME = $(subst _,.,$(JNI_BASE_NAME))
JNI_CLASS_FILE = classes/$(subst .,/,$(JNI_CLASS_NAME)).class

define JAVAHPP_RULE
$(JAVAHPP) -classpath classes $(JNI_CLASS_NAME) > $(NEW_JNI_HEADER) && \
{ cmp -s $(NEW_JNI_HEADER) $(JNI_HEADER) || cp $(NEW_JNI_HEADER) $(JNI_HEADER); }
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
# The WiX installer and intermediate files.
# ----------------------------------------------------------------------------

WIX_SOURCE = $(filter %.wxs,$(SOURCES))
WIX_BASE_NAME = $(basename $(notdir $(WIX_SOURCE)))
WIX_COMPONENT_DEFINITIONS = $(COMPILATION_DIRECTORY)/component-definitions.wxi
WIX_COMPONENT_REFERENCES = $(COMPILATION_DIRECTORY)/component-references.wxi
WIX_OBJECTS = $(COMPILATION_DIRECTORY)/$(WIX_BASE_NAME).wixobj
WIX_INSTALLER = $(BIN_DIRECTORY)/$(WIX_BASE_NAME).msi
WIX_MODULE = $(COMPILATION_DIRECTORY)/$(WIX_BASE_NAME).msm
WIX_TARGET := $(if $(WIX_SOURCE),$(if $(shell grep "Product Id" $(WIX_SOURCE)),INSTALLER,MODULE))

# ----------------------------------------------------------------------------
# Decide on the default target.
# ----------------------------------------------------------------------------

DESIRED_TARGETS =
DESIRED_TARGETS += $(if $(JNI_SOURCE),$(JNI_LIBRARY))
DESIRED_TARGETS += $(if $(WIX_TARGET),$(WIX_$(WIX_TARGET)))
DESIRED_TARGETS := $(if $(strip $(DESIRED_TARGETS)),$(DESIRED_TARGETS),$(EXECUTABLES))
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
$(NEW_JNI_HEADER): RULE := $(JAVAHPP_RULE)
ifeq "$(WIX_TARGET)" "INSTALLER"
$(WIX_COMPONENT_DEFINITIONS): FILE_LIST_TO_WXI_FLAGS := --diskId
endif
# We mustn't := evaluate $@ and $<
$(WIX_INSTALLER) $(WIX_MODULE): RULE = $(LIGHT) -nologo -out $(call convertToNativeFilenames,$@ $(filter %.wixobj,$^))
missing-prerequisites.$(BASE_NAME): RULE := $(MISSING_PREREQUISITES_RULE)

# ----------------------------------------------------------------------------
# Variables above this point,
# rules below...
# ----------------------------------------------------------------------------

# ----------------------------------------------------------------------------
# Our linked targets.
# ----------------------------------------------------------------------------

$(EXECUTABLES) $(JNI_LIBRARY): $(OBJECTS)
	mkdir -p $(@D) && \
	$(LD) $^ -o $@ $(LDFLAGS)

# ----------------------------------------------------------------------------
# Generate our JNI header.
# ----------------------------------------------------------------------------

ifneq "$(JNI_SOURCE)" ""

$(JNI_CLASS_FILE): $(SOURCE_FILES)
	$(BUILD_JAVA)

$(NEW_JNI_HEADER): $(JNI_CLASS_FILE) $(JAVAHPP) $(SALMA_HAYEK)/classes/e/tools/JavaHpp.class
	@echo Generating JNI header... && \
	mkdir -p $(@D) && \
	$(RM) $@ && \
	$(RULE)

$(JNI_OBJECT): $(JNI_HEADER)
build: $(NEW_JNI_HEADER)
$(JNI_HEADER): $(NEW_JNI_HEADER);

endif

# ----------------------------------------------------------------------------
# Rules for compiling Objective C and Objective C++ source.
# ----------------------------------------------------------------------------

$(OBJECTS.m): %.o: %.m
	$(COMPILE.m) $(OUTPUT_OPTION) $<

$(OBJECTS.mm): %.o: %.mm
	$(COMPILE.mm) $(OUTPUT_OPTION) $<

# ----------------------------------------------------------------------------
# WiX
# ----------------------------------------------------------------------------

ifneq "$(WIX_SOURCE)" ""

# universal.make adds more dependencies - only it knows $(ALL_NATIVE_TARGETS_EXCEPT_INSTALLERS).
$(WIX_COMPONENT_DEFINITIONS): $(MAKEFILE_LIST) $(FILE_LIST_TO_WXI)
	$(MAKE_INSTALLER_FILE_LIST) | $(FILE_LIST_TO_WXI) $(FILE_LIST_TO_WXI_FLAGS) > $@

# This silliness is probably sufficient (as well as sadly necessary).
$(WIX_COMPONENT_REFERENCES): $(WIX_COMPONENT_DEFINITIONS) $(MAKEFILE_LIST)
	perl -ne 'm/Include/ && print; m/<Component (Id='\''component\d+'\'')/ && print("<ComponentRef $$1 />\n")' < $< > $@

$(WIX_OBJECTS): $(WIX_COMPONENT_REFERENCES) $(WIX_COMPONENT_DEFINITIONS)

$(WIX_OBJECTS): %.wixobj: %.wxs .generated/build-revision.txt
	PRODUCT_GUID=$(makeGuid) VERSION_STRING=$(VERSION_STRING) $(CANDLE) -nologo -out $(call convertToNativeFilenames,$@ $<)

$(WIX_MODULE): %.msm: $(WIX_OBJECTS)
	$(RULE)

$(WIX_INSTALLER): %.msi: $(WIX_OBJECTS)
	$(RULE)

endif

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
# Using cp for the benefit of Windows native compilers which don't
# understand "symlinks".
# FIXME: Copies of files which no longer exist must be removed.
$(SOURCE_LINKS) $(HEADER_LINKS): $(COMPILATION_DIRECTORY)/%: $(SOURCE_DIRECTORY)/%
	mkdir -p $(@D) && \
	$(RM) $@ && \
	cp $< $@

# ----------------------------------------------------------------------------
# Dependencies.
# ----------------------------------------------------------------------------

# Rather than have the compiler track dependencies we
# conservatively assume that if a header files changes, we have to recompile
# everything.
$(OBJECTS): $(HEADER_LINKS) $(HEADERS) $(MAKEFILE_LIST)
$(OBJECTS): $(wildcard $(SALMA_HAYEK)/native/Headers/*)
