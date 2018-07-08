include ../salma-hayek/lib/build/simple.make

X11_TARGETS += clock lock menu speckeysd window x11-reaper

.PHONY: x11
x11:
	@for target in $(X11_TARGETS) ; do make -C $$target ; done

makefiles:
	@for target in $(X11_TARGETS) ; do cd $$target ; xmkmf ; cd .. ; done

#clean:
#	@for target in $(X11_TARGETS) ; do make -C $$target clean ; done

%: force
	@$(MAKE) -f ../salma-hayek/lib/build/simple.make $@
force: ;
