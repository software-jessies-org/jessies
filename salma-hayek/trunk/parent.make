.PHONY: default
default: build update
build: | update

SUBDIRS += salma-hayek
SUBDIRS += edit
SUBDIRS += terminator
SUBDIRS += scm

%:
	$(foreach SUBDIR,$(SUBDIRS),$(MAKE) -C $(SUBDIR) $@;)

.PHONY: update
update:
	{ $(foreach SUBDIR,$(SUBDIRS),( cd $(SUBDIR) && svn update &);) } | cat
