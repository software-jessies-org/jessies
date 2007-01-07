#!/bin/bash

# BlueArc's local copy of the archive:
DEB_DIRECTORY=/u/u154/software.jessies.org/
# deb http://deb/software.jessies.org/ ./
# sudo apt-key add - < $DEB_DIRECTORY/software.jessies.org.gpg
#
# "deb" is a DNS alias for a machine in the local domain running apache on which the following command has been run:
# sudo ln -s $DEB_DIRECTORY /var/www/software.jessies.org

mkdir -p $DEB_DIRECTORY
cd $DEB_DIRECTORY
# Remove any .debs with obsolete names.
rm *.deb

# Run the latest version of the nightly build script, rather than the version from yesterday.
# This has the advantage that the update won't overwrite the running script - which Ruby doesn't like.
NIGHTLY_BUILD_SCRIPT=~martind/software.jessies.org/work/salma-hayek/bin/nightly-build.rb
$NIGHTLY_BUILD_SCRIPT clean native-dist
# We have to copy the i386 .debs before we clean, before we build the amd64 .debs,
# because we use the same directories for the intermediate binary files.
find ~martind/software.jessies.org/nightlies/ -name "*.deb" | xargs cp --target-directory=.
echo $NIGHTLY_BUILD_SCRIPT clean native-dist | ssh wide bash --login
find ~martind/software.jessies.org/nightlies/ -name "*.deb" | xargs cp --target-directory=.

# If we don't have a Packages file (as well as Packages.gz) we get:
# Failed to fetch http://deb/software.jessies.org/./Release  Unable to find expected entry  Packages in Meta-index file (malformed Release file?)
apt-ftparchive packages . > Packages
gzip -9 < Packages > Packages.gz
apt-ftparchive release . > Release
# This has to run as martind@bluearc.com to sign with the right key.
gpg -sba - - < Release > Release.gpg
scp Packages Packages.gz Release Release.gpg software@jessies.org:~/downloads/debian/
