PROJECT_NAME=TerminatorN

DIST=terminator-dist

.PHONY: default
default: build build.pty

.PHONY: build.pty
build.pty:
	@$(MAKE) -C pty

jar: default
	rm -f $(DIST).zip
	mkdir $(DIST)
	mkdir $(DIST)/temp
	cp -R classes/* $(DIST)/temp/
	cp -R ../salma-hayek/classes/* $(DIST)/temp/
	find $(DIST)/temp -name 'CVS'|xargs rm -rf
	(cd $(DIST)/temp && jar cf ../terminator.jar *)
	rm -rf $(DIST)/temp
	cp pty/pty-* $(DIST)/pty
	cp -R doc $(DIST)/
	find $(DIST) -name 'CVS'|xargs rm -rf
	cp terminator-dist-script $(DIST)/terminator
	cp README $(DIST)/
	zip -9r $(DIST).zip $(DIST)
	rm -rf $(DIST)
	echo Created $(DIST).zip

include ../salma-hayek/java.make
