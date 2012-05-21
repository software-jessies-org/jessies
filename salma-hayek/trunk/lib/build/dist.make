# ----------------------------------------------------------------------------
# The purpose of dist.make is to implement the native-dist target.
# This should upload $(PUBLISHABLE_INSTALLERS), where that list may be empty (eg for salma-hayek) and may contain two elements (eg .msi and .tar.gz).
# dist.make also seems the most natural current home for other rules which are run on jessies.org.
# These rules need to know about the same directory structure.
# From being included from universal.make, this script inherits and uses definitions of a number of make(1) settings and variables in addition to PUBLISHABLE_INSTALLERS.
# ----------------------------------------------------------------------------

# When $< is "org.jessies.evergreen-4.31.1934-2.x86_64.rpm", we want "org.jessies.evergreen.x86_64.rpm".
# When $< is "org.jessies.evergreen_4.31.1934_amd64.deb", we want "org.jessies.evergreen.amd64.deb".
# When $< is "evergreen-4.31.1934.msi", we want "evergreen.msi".
# I wonder if we shouldn't say "latest" somewhere in the name.
# It would be easy to do that were it not for the odd "-2" part that alien adds to the name.
# When I upload, perhaps I should get rid of that.
# Passing --keep-version to alien turns the "-2" into "-1".
# Perhaps the rpm version number format doesn't support our Windows-style version numbers.
LATEST_INSTALLER_LINK = $(subst --2.,.,$(subst __,.,$(subst -.,.,$(call makeInstallerName$(suffix $<),))))

# ----------------------------------------------------------------------------
# Distribution variables - where we upload to.
# ----------------------------------------------------------------------------

SOURCE_DIST_FILE = $(MACHINE_PROJECT_NAME)-source.tar.gz

# Distributions end up under http://software.jessies.org/
DIST_SSH_USER_AND_HOST = software@jessies.org
# Different file types end up in different directories.
# Note the use of the prerequisite's extension rather than that of the target, which is always phony.
DIST_SUBDIRECTORY_FOR_PREREQUISITE = $(DIST_SUBDIRECTORY$(suffix $<))
DIST_DIRECTORY = /home/software/downloads/$(if $(DIST_SUBDIRECTORY_FOR_PREREQUISITE),$(DIST_SUBDIRECTORY_FOR_PREREQUISITE),$(error sub-directory not specified for extension "$(suffix $<)"))
# The html files are copied, with rsync, from www/ into this directory.
DIST_SUBDIRECTORY = $(MACHINE_PROJECT_NAME)
# ChangeLog.html is copied to the same place.
DIST_SUBDIRECTORY.html = $(DIST_SUBDIRECTORY)
# $(suffix)'s definition means we need .gz here not .tar.gz.
# We have to distinguish between the SOURCE_DIST and OS-specific .tar.gz distributions here.
DIST_SUBDIRECTORY.gz = $(DIST_SUBDIRECTORY_FOR_$(notdir $<))
DIST_SUBDIRECTORY_FOR_$(SOURCE_DIST_FILE) = $(MACHINE_PROJECT_NAME)
# Pick "deb" (or "rpm") from "gz deb rpm".
PRIMARY_INSTALLER_EXTENSION = $(firstword $(filter-out gz,$(INSTALLER_EXTENSIONS.$(TARGET_OS))))
DIST_SUBDIRECTORY_FOR_$(notdir $(INSTALLER.gz)) = $(DIST_SUBDIRECTORY.$(PRIMARY_INSTALLER_EXTENSION))
# Debian's mirrors are in a top-level directory called debian.
# I thought there might be some tool dependency on that.
DIST_SUBDIRECTORY.deb = debian
DIST_SUBDIRECTORY.zip = mac
DIST_SUBDIRECTORY.msi = windows
DIST_SUBDIRECTORY.pkg = sunos
DIST_SUBDIRECTORY.rpm = redhat

PLATFORM_NAME.deb = Debian
PLATFORM_NAME.zip = Mac
PLATFORM_NAME.msi = Windows
PLATFORM_NAME.pkg = Solaris
PLATFORM_NAME.rpm = RedHat

PLATFORM_NAME_FOR_PREREQUISITE = $(PLATFORM_NAME$(suffix $<))
PLATFORM_NAME = $(if $(PLATFORM_NAME_FOR_PREREQUISITE),$(PLATFORM_NAME_FOR_PREREQUISITE),$(error platform name not specified for extension "$(suffix $<)"))
SUMMARY = $(PLATFORM_NAME) installer for $(HUMAN_PROJECT_NAME) version $(VERSION_STRING)

# ----------------------------------------------------------------------------
# Variables above this point, rules below.
# See universal.make for an explanation.
# ----------------------------------------------------------------------------

# This is only designed to be run on jessies.org itself.
# It's run by a custom post-commit hook to generate a new source download for each revision.
.PHONY: source-dist
source-dist: ../$(SOURCE_DIST_FILE)
	mkdir -p $(DIST_DIRECTORY) && \
	mv $< $(DIST_DIRECTORY)/

# Including a generated file in a source distribution?
# The ChangeLog is generated too!
../$(SOURCE_DIST_FILE): ChangeLog .generated/build-revision.txt
	cd .. && \
	tar -X $(BUILD_SCRIPT_PATH)/dist-exclude -zcf $(SOURCE_DIST_FILE) $(PROJECT_DIRECTORY_BASE_NAME)/* $(PROJECT_DIRECTORY_BASE_NAME)/.generated/build-revision.txt

# This is only designed to be run on jessies.org itself.
.PHONY: www-dist
www-dist: www
	mkdir -p $(DIST_DIRECTORY) && \
	rsync -v -r www/* $(DIST_DIRECTORY)/

# This is only designed to be run on jessies.org itself.
.PHONY: publish-changelog
publish-changelog: ChangeLog.html
	mkdir -p $(DIST_DIRECTORY) && \
	mv ChangeLog.html $(DIST_DIRECTORY)/

.PHONY: native-dist
native-dist: $(addprefix googlecode-upload.,$(PUBLISHABLE_INSTALLERS))

# We mustn't overwrite a file on the server whose md5sum is in Packages file or Debian's tools complain mightily.
# If you want to upload a new installer, then please check-in first, so the version number changes.
.PHONY: upload.%
$(addprefix upload.,$(PUBLISHABLE_INSTALLERS)): upload.%: %
	@echo "-- Testing for $(<F) on the server..." && \
	ssh $(DIST_SSH_USER_AND_HOST) mkdir -p $(DIST_DIRECTORY)  '&&' test -f $(DIST_DIRECTORY)/$(<F) && { \
		echo Leaving $(<F) alone...; \
	} || { \
		echo Uploading $(<F)... && \
		scp $< $(DIST_SSH_USER_AND_HOST):$(DIST_DIRECTORY)/$(<F).tmp && \
		ssh $(DIST_SSH_USER_AND_HOST) mv $(DIST_DIRECTORY)/$(<F).tmp $(DIST_DIRECTORY)/$(<F); \
	}

# I like the idea of keeping several versions on the server but we're going to have a hard time
# linking to the one we expect people to use unless we create a symlink.
$(addprefix symlink-latest.,$(PUBLISHABLE_INSTALLERS)): symlink-latest.%: upload.%
.PHONY: symlink-latest.%
$(addprefix symlink-latest.,$(PUBLISHABLE_INSTALLERS)): symlink-latest.%: %
	@echo "-- Symlinking the latest $(LATEST_INSTALLER_LINK)..."
	ssh $(DIST_SSH_USER_AND_HOST) $(RM) $(DIST_DIRECTORY)/$(LATEST_INSTALLER_LINK) '&&' \
	ln -s $(<F) $(DIST_DIRECTORY)/$(LATEST_INSTALLER_LINK) '&&' \
	ls -t $(DIST_DIRECTORY)/$(call makeInstallerName$(suffix $<),'*') '|' tail -n +8 '|' xargs $(RM)

# Nightly builds won't be able to ask for the password,
# eg when the file has already been uploaded.
# So we shouldn't allow that during the daytime either.
.PHONY: googlecode-upload.%
$(addprefix googlecode-upload.,$(PUBLISHABLE_INSTALLERS)): googlecode-upload.%: %
	@echo "-- Uploading $(<F) to code.google.com..." && \
	$(SALMA_HAYEK)/lib/build/googlecode_upload.py -s '$(SUMMARY)' -p jessies $< < /dev/null
