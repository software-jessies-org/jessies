include ../salma-hayek/universal.make

X11_TARGETS += clock lock menu speckeysd window x11-reaper

.PHONY: x11
x11:
	@# FIXME: this assumes that we've already run xmkmf(1) in each of the directories.
	@for target in $(X11_TARGETS) ; do make -C $$target ; done

makefiles:
	@for target in $(X11_TARGETS) ; do cd $$target ; xmkmf ; cd .. ; done

#clean:
#	@for target in $(X11_TARGETS) ; do make -C $$target clean ; done

%: force
	@$(MAKE) -f ../salma-hayek/universal.make $@
force: ;
