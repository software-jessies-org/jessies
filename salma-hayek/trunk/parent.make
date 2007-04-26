.DEFAULT:
.DELETE_ON_ERROR:
.SECONDARY:

SUBDIRS += salma-hayek

SUBDIRS += Evergreen
SUBDIRS += KnowAll
SUBDIRS += scm
SUBDIRS += terminator

SUBDIRS := $(wildcard $(SUBDIRS))

# variables
# ======================================================================
# rules

.PHONY: default
default: update build
.PHONY: build
build: recurse.build
.PHONY: installer
installer: recurse.installer
.PHONY: clean
clean: recurse.clean

.PHONY: recurse.%
recurse.%:
	$(foreach SUBDIR,$(SUBDIRS),$(MAKE) -C $(SUBDIR) $*;)

.PHONY: update
update:
	{ $(foreach SUBDIR,$(SUBDIRS),( cd $(SUBDIR) && svn update &);) } | cat
