BADGER:=$(notdir $(patsubst %/,%,$(dir $(word $(words $(MAKEFILE_LIST)),$(MAKEFILE_LIST)))))
SALMA_HAYEK=$(dir $(word $(words $(MAKEFILE_LIST)),$(MAKEFILE_LIST)))

ARCHIVE_HOST=locke

KNOWN_JRE_LOCATIONS+=$(JAVA_HOME)/jre/lib/rt.jar
KNOWN_JRE_LOCATIONS+=/home/elliotth/download/j2sdk1.4.2/jre/lib/rt.jar
KNOWN_JRE_LOCATIONS+=/System/Library/Frameworks/JavaVM.framework/Classes/classes.jar
KNOWN_JRE_LOCATIONS+=MRJ141Stubs.jar

SOURCE_FILES=$(shell find `pwd`/src -type f -name "*.java")
TAR_FILE_OF_THE_DAY=`date +$(PROJECT_NAME)-%Y-%m-%d.tar`

space=$(subst a,,a a)
BOOT_CLASS_PATH=$(subst $(space),:,$(wildcard $(KNOWN_JRE_LOCATIONS)))

REVISION_CONTROL_SYSTEM := $(if $(wildcard .svn),svn,cvs)

define GENERATE_CHANGE_LOG.svn
  svn log > ChangeLog
endef

define GENERATE_CHANGE_LOG.cvs
  $(if $(shell which cvs2cl),cvs2cl,cvs2cl.pl) --hide-filenames
endef

.PHONY: build
build: $(SOURCE_FILES)
	@echo Recompiling the world... && \
	 make clean && \
	 mkdir -p classes && \
	 jikes -bootclasspath $(BOOT_CLASS_PATH) -classpath $(SALMA_HAYEK)/classes -d classes/ -sourcepath src/ +D +P +Pall +Pno-serial +Pno-redundant-modifiers $(SOURCE_FILES)
	 @#javac -d classes/ -sourcepath src/ $(SOURCE_FILES)

.PHONY: clean
clean:
	@$(RM) -rf classes

.PHONY: dist
dist: build
	$(GENERATE_CHANGE_LOG.$(REVISION_CONTROL_SYSTEM)); \
	find . -name "*.bak" | xargs --no-run-if-empty rm; \
	cd $(if $(wildcard ../trunk),../..,..) && \
	tar cvf $(TAR_FILE_OF_THE_DAY) $(PROJECT_NAME)/ && \
	gzip $(TAR_FILE_OF_THE_DAY) && \
	scp $(TAR_FILE_OF_THE_DAY).gz $(ARCHIVE_HOST):~/$(PROJECT_NAME)/
