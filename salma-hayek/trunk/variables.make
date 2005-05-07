# This makefile is included at the start of other makefiles.

# ----------------------------------------------------------------------------
# Disable legacy make behavior.
# ----------------------------------------------------------------------------

# We used to disable suffix rules, but the default compilation rules are suffix
# rules, and we want to use them in "native.make".
#.SUFFIXES:

.DEFAULT:
.DELETE_ON_ERROR:
.SECONDARY:

# ----------------------------------------------------------------------------
# Define useful stuff not provided by GNU make.
# ----------------------------------------------------------------------------

pathsearch = $(firstword $(wildcard $(addsuffix /$(1),$(subst :, ,$(PATH)))))
makepath = $(subst $(SPACE),:,$(strip $(1)))
getAbsolutePath = $(patsubst @%,$(CURDIR)/%,$(patsubst @/%,/%,$(patsubst %,@%,$(1))))

SPACE := $(subst :, ,:)

# ----------------------------------------------------------------------------
# Locate salma-hayek.
# ----------------------------------------------------------------------------

SALMA_HAYEK := $(call getAbsolutePath,$(MOST_RECENT_MAKEFILE_DIRECTORY))
export SALMA_HAYEK

# ----------------------------------------------------------------------------
# Locate Java.
# ----------------------------------------------------------------------------

# Assume the tools are on the path if $(JAVA_HOME) isn't specified.
# Note the := to evaluate $(JAVA_HOME) before native.make defaults it to a $(error).
JAVA_PATH := $(if $(JAVA_HOME),$(JAVA_HOME)/bin/)

JAR = $(JAVA_PATH)jar
JAVAH := $(JAVA_PATH)javah

# ----------------------------------------------------------------------------
# Find the source.
# ----------------------------------------------------------------------------

SOURCE_EXTENSIONS += c
SOURCE_EXTENSIONS += cpp
SOURCE_EXTENSIONS += m
SOURCE_EXTENSIONS += mm

# ----------------------------------------------------------------------------
# Work out what we're going to generate.
# ----------------------------------------------------------------------------

TARGET_OS := $(shell uname)
