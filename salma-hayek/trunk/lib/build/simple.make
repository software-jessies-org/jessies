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
#   must include ../salma-hayek/lib/build/simple.make

#   probably shouldn't have any extra rules - they won't fit in

#   must export any variables that it wants to influence salma-hayek's makefiles before the include

# Must be used in every project for which an .msi installer is generated.
export UPGRADE_GUID
# Should be used in every project whose name isn't entirely lowercase.
ifneq "$(HUMAN_PROJECT_NAME)" ""
export HUMAN_PROJECT_NAME
endif

# ----------------------------------------------------------------------------
# Ensure we're running a suitable version of make(1).
# ----------------------------------------------------------------------------

REQUIRED_MAKE_VERSION = 3.81
REAL_MAKE_VERSION = $(firstword $(MAKE_VERSION))
EARLIER_MAKE_VERSION = $(firstword $(sort $(REAL_MAKE_VERSION) $(REQUIRED_MAKE_VERSION)))
ifneq "$(REQUIRED_MAKE_VERSION)" "$(EARLIER_MAKE_VERSION)"
    $(warning This makefile assumes at least GNU make $(REQUIRED_MAKE_VERSION), but you're using $(REAL_MAKE_VERSION))
    $(warning )
    $(warning If you don't have build errors, you can ignore these warnings.)
    $(warning If you do have build errors, they are probably not make-related.)
    $(warning Exceptions include errors like:)
    $(warning make: *** virtual memory exhausted.  Stop.)
    $(warning ../salma-hayek/lib/build/universal.make:494: *** makefile bug: local variable FIND_FALSE from scope setpgid (with value "! -prune") was referred to in scope setpgid.  Stop.)
endif

# universal.make produces suggestions for where to download make-3.81, if we manage to work out TARGET_OS.
export REQUIRED_MAKE_VERSION
export EARLIER_MAKE_VERSION

# ----------------------------------------------------------------------------
# Disable legacy make behavior.
# ----------------------------------------------------------------------------

.SUFFIXES:
.DEFAULT:
.DELETE_ON_ERROR:

# ----------------------------------------------------------------------------
# Define useful stuff not provided by GNU make.
# ----------------------------------------------------------------------------

SPACE = $(subst :, ,:)

ifneq "$(findstring $(SPACE),$(CURDIR))" ""
$(error Your work area is under a directory whose name contains a space.  While it's possible to beat make into working with such directories, it's surprisingly hard and causes more problems than it solves.  So, sorry, but don't do that)
endif

# ----------------------------------------------------------------------------
# Locate salma-hayek.
# ----------------------------------------------------------------------------

# $(lastword) isn't available in 3.80.
MOST_RECENT_MAKEFILE = $(word $(words $(MAKEFILE_LIST)),$(MAKEFILE_LIST))
# The location of this makefile shouldn't change with later includes.
SIMPLE_MAKEFILE := $(MOST_RECENT_MAKEFILE)
# $(dir $(dir)) doesn't do what you want.
dirWithoutSlash = $(patsubst %/,%,$(dir $(1)))
MAKEFILE_DIRECTORY = $(call dirWithoutSlash,$(SIMPLE_MAKEFILE))
ABSOLUTE_MAKEFILE_DIRECTORY = $(patsubst ../%,$(dir $(CURDIR))%,$(MAKEFILE_DIRECTORY))
SALMA_HAYEK = $(call dirWithoutSlash,$(call dirWithoutSlash,$(ABSOLUTE_MAKEFILE_DIRECTORY)))

# Begin 2007-11-11 hack to cope with salma-hayek/simple.make.
ifeq "$(notdir $(ABSOLUTE_MAKEFILE_DIRECTORY))" "salma-hayek"
SALMA_HAYEK = $(ABSOLUTE_MAKEFILE_DIRECTORY)
$(warning please update $(firstword $(MAKEFILE_LIST)) to include ../salma-hayek/lib/build/simple.make instead of ../salma-hayek/simple.make)
endif
# End 2007-11-11 hack.

# make does special things when running commands including the magic string $(MAKE),
# including clearing -n from $(MAKEFLAGS), so we snarf it here.
DRY_RUNNING := $(findstring n,$(MAKEFLAGS))
SHOULD_NOT_FILTER_OUTPUT = $(strip $(filter echo.%,$(MAKECMDGOALS)) $(DRY_RUNNING))

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
	$(if $(DRY_RUNNING),,@)$(if $(SHOULD_NOT_FILTER_OUTPUT),,ruby $(SALMA_HAYEK)/lib/build/filter-build-output.rb) $(MAKE) $(if $(DRY_RUNNING),-n) $(MAKECMDGOALS) -f $(SALMA_HAYEK)/lib/build/universal.make
