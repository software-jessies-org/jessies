PROJECT_NAME=TerminatorN

DIST=terminator-dist
DIST_ZIP=$(DIST).zip
DIST_FILES=README COPYING doc pty

.PHONY: default
default: build build.pty

.PHONY: build.pty
build.pty:
	@$(MAKE) -C pty

jar: default
	rm -rf $(DIST_ZIP) $(DIST)
	mkdir $(DIST)
	mkdir $(DIST)/temp
	cp -R classes/* $(DIST)/temp/
	cp -R ../salma-hayek/classes/* $(DIST)/temp/
	find $(DIST)/temp -name 'CVS'|xargs rm -rf
	(cd $(DIST)/temp && jar cf ../terminator.jar *)
	rm -rf $(DIST)/temp
	cp -R $(DIST_FILES) $(DIST)/
	find $(DIST) -name 'CVS'|xargs rm -rf
	cp terminator-dist-script $(DIST)/terminator
	zip -9r $(DIST_ZIP) $(DIST)
	rm -rf $(DIST)
	echo Created $(DIST_ZIP)

include ../salma-hayek/java.make
