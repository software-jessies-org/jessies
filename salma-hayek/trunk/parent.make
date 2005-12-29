.DEFAULT:
.DELETE_ON_ERROR:
.SECONDARY:

SUBDIRS += salma-hayek

SUBDIRS += edit
SUBDIRS += KnowAll
SUBDIRS += scm
SUBDIRS += terminator

SUBDIRS := $(wildcard $(SUBDIRS))

PROJECT_NAME = software.jessies.org
DIST_FILE_OF_THE_DAY := $(shell date +$(PROJECT_NAME)-%Y-%m-%d.tar.gz)

# Not immediately evaluated because it's expensive
FILE_LIST = $(foreach SUBDIR,$(SUBDIRS),$(addprefix $(SUBDIR)/,$(subst ",,$(shell $(MAKE) --no-print-directory -C $(SUBDIR) echo.FILE_LIST))))

# variables
# ======================================================================
# rules

.PHONY: default
default: update build
.PHONY: build
build: recurse.build
#build: | update
#build: | clean
.PHONY: clean
clean: recurse.clean
#clean: | update

.PHONY: recurse.%
recurse.%:
	$(foreach SUBDIR,$(SUBDIRS),$(MAKE) -C $(SUBDIR) $*;)

.PHONY: update
update:
	{ $(foreach SUBDIR,$(SUBDIRS),( cd $(SUBDIR) && svn update &);) } | cat

$(DIST_FILE_OF_THE_DAY): update clean build recurse.ChangeLog
	tar -zcf $@ $(FILE_LIST)

.PHONY: dist
dist: $(DIST_FILE_OF_THE_DAY)
	$(MAKE) clean build
# TODO: upload
