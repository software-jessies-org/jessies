SALMA_HAYEK=$(dir $(word $(words $(MAKEFILE_LIST)),$(MAKEFILE_LIST)))
#SALMA_HAYEK=~/Projects/salma-hayek/

# By default, distributions end up under http://www.jessies.org/~enh/
DIST_SCP_DESTINATION=enh@jessies.org:public_html/software/$(PROJECT_NAME)

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

GENERATED_FILES += classes

.PHONY: clean
clean:
	@$(RM) -rf $(GENERATED_FILES)

.PHONY: dist
dist: build
	$(GENERATE_CHANGE_LOG.$(REVISION_CONTROL_SYSTEM)); \
	find . -name "*.bak" | xargs --no-run-if-empty rm; \
	scp -r www/* $(DIST_SCP_DESTINATION) && \
	cd $(if $(wildcard ../trunk),../..,..) && \
	tar --exclude=CVS --exclude=.svn -cvf $(TAR_FILE_OF_THE_DAY) $(PROJECT_NAME)/ && \
	gzip $(TAR_FILE_OF_THE_DAY) && \
	scp $(TAR_FILE_OF_THE_DAY).gz $(DIST_SCP_DESTINATION)/$(PROJECT_NAME).tgz

JAR=$(if $(JAVA_HOME),$(JAVA_HOME)/bin/)jar
CREATE_OR_UPDATE_JAR=cd $(2) && $(JAR) $(1)f $(CURDIR)/$@ -C classes $(notdir $(wildcard $(2)/classes/*))

$(PROJECT_NAME).jar: build
	@$(call CREATE_OR_UPDATE_JAR,c,$(CURDIR)) && \
	$(call CREATE_OR_UPDATE_JAR,u,$(SALMA_HAYEK))

GENERATED_FILES += $(PROJECT_NAME).jar

grep-v = $(filter-out @@%,$(filter-out %@@,$(subst $(1),@@ @@,$(2))))
DIRECTORY_NAME := $(notdir $(CURDIR))

BINDIST_FILES += README COPYING $(PROJECT_NAME).jar
FILTERED_BINDIST_FILES = $(shell find $(BINDIST_FILES) -type f | grep -v CVS)

.PHONY: bindist
bindist: $(PROJECT_NAME)-bindist.tgz

$(PROJECT_NAME)-bindist.tgz: build $(BINDIST_FILES)
	@cd .. && tar -zcf $(addprefix $(DIRECTORY_NAME)/,$@ $(FILTERED_BINDIST_FILES))

GENERATED_FILES += $(PROJECT_NAME)-bindist.tgz
