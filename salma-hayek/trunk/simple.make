# You may use:
#   make build (the default target if none is specified)
#   make clean
#   make native
#   make installer
#   make install
#   make remove
#   make native-clean
#   make native-dist
#   make source-dist
#   make test
#   make www-dist

# Your calling Makefile:
#   must include ../salma-hayek/simple.make

#   probably shouldn't have any extra rules - they won't fit in

#   must export any variables that it wants to influence salma-hayek's makefiles before the include

# Used in Evergreen, terminator and scm.
export UPGRADE_GUID
# Used in terminator.
ifneq "$(HUMAN_PROJECT_NAME)" ""
export HUMAN_PROJECT_NAME
endif

# ----------------------------------------------------------------------------
# Disable legacy make behavior.
# ----------------------------------------------------------------------------

.SUFFIXES:
.DEFAULT:
.DELETE_ON_ERROR:

# ----------------------------------------------------------------------------
# Locate salma-hayek.
# ----------------------------------------------------------------------------

MOST_RECENT_MAKEFILE_DIRECTORY = $(patsubst %/,%,$(dir $(word $(words $(MAKEFILE_LIST)),$(MAKEFILE_LIST))))
SALMA_HAYEK := $(patsubst ../%,$(dir $(CURDIR))%,$(MOST_RECENT_MAKEFILE_DIRECTORY))

# make does special things when running commands including the magic string $(MAKE),
# including clearing -n from $(MAKEFLAGS), so we snarf it here.
DRY_RUNNING := $(filter n,$(MAKEFLAGS))
SHOULD_FILTER_OUTPUT = $(strip $(filter echo.%,$(MAKECMDGOALS)) $(DRY_RUNNING))

# This mad dance seems to be necessary to allow any target to recurse without listing them all here
# (bear in mind that you can ask to build a bunch of .o files if you like)
# without sometimes causing make to say "Nothing to be done for `<target>'" or "`<target>' is up to date".
.PHONY: default
default: recurse
	@exit 0

%: recurse
	@exit 0

# The difficulty of recursive make -n support is particularly distressing.
# We just don't want to be running make here but that's what everyone expects to type.
.PHONY: recurse
recurse:
	$(if $(DRY_RUNNING),,@)$(MAKE) $(if $(DRY_RUNNING),-n) $(MAKECMDGOALS) -f $(SALMA_HAYEK)/universal.make $(if $(SHOULD_FILTER_OUTPUT),,2>&1 | ruby $(SALMA_HAYEK)/lib/build/filter-build-output.rb)
