PROJECT_NAME=edit
ARCHIVE_HOST=locke

KNOWN_JRE_LOCATIONS+=/home/elliotth/download/j2sdk1.4.2/jre/lib/rt.jar
KNOWN_JRE_LOCATIONS+=/System/Library/Frameworks/JavaVM.framework/Classes/classes.jar
KNOWN_JRE_LOCATIONS+=MRJ141Stubs.jar

SOURCE_FILES=$(shell find `pwd`/src -type f -name "*.java")
TAR_FILE_OF_THE_DAY=`date +$(PROJECT_NAME)-%Y-%m-%d.tar`

space=$(subst a,,a a)
BOOT_CLASS_PATH=$(subst $(space),:,$(wildcard $(KNOWN_JRE_LOCATIONS)))

.PHONY: build
build: $(SOURCE_FILES)
	@echo Recompiling the world... && \
	 make clean && \
	 mkdir -p classes && \
	 jikes -bootclasspath $(BOOT_CLASS_PATH) -d classes/ -sourcepath src/ +D +P +Pall +Pno-serial +Pno-redundant-modifiers $(SOURCE_FILES)
	 @#javac -d classes/ -sourcepath src/ $(SOURCE_FILES)

.PHONY: clean
clean:
	@$(RM) -rf classes

.PHONY: dist
dist: build
	rm -f edit.log; cd ..; \
	tar cvf $(TAR_FILE_OF_THE_DAY) $(PROJECT_NAME)/; \
	gzip $(TAR_FILE_OF_THE_DAY); \
	scp $(TAR_FILE_OF_THE_DAY).gz $(ARCHIVE_HOST):~/$(PROJECT_NAME)/
