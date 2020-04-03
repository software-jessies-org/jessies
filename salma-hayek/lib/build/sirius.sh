#!/bin/bash

# BlueArc's local copy of the archive:
DEB_DIRECTORY=/u/u154/software.jessies.org/
# deb http://deb/software.jessies.org/ ./
# sudo apt-key add - < $DEB_DIRECTORY/software.jessies.org.gpg
#
# "deb" is a DNS alias for a machine in the local domain running apache on which the following command has been run:
# sudo ln -s $DEB_DIRECTORY /var/www/software.jessies.org

mkdir -p $DEB_DIRECTORY
cd $DEB_DIRECTORY || exit 1
# Remove any .debs with obsolete names.
rm *.deb

# Run the latest version of the nightly build script, rather than the version from yesterday.
# This has the advantage that the update won't overwrite the running script - which Ruby doesn't like.
NIGHTLY_BUILD_SCRIPT=~martind/jessies/work/salma-hayek/bin/nightly-build.rb
# After restore-user-backup:
# martind@sirius:~/jessies/work$ ln -s ../scm/work/scm .
# martind@sirius:~/jessies/nightlies$ ln -s ../scm/nightlies/scm .
NIGHTLY_BUILD_TREE=~martind/jessies/nightlies
$NIGHTLY_BUILD_SCRIPT $NIGHTLY_BUILD_TREE clean
# salma-hayek/lib/build/drive.rb explains how to get nightlies/salma-hayek/lib/build/client_secrets.json
{
echo $NIGHTLY_BUILD_SCRIPT --no-update $NIGHTLY_BUILD_TREE native-dist
} |
# #schroot --quiet --chroot ia32 -- bash --login
# sudo aptitude install libx11-dev:i386 alien
TARGET_ARCHITECTURE=i386 \
bash
$NIGHTLY_BUILD_SCRIPT --no-update $NIGHTLY_BUILD_TREE native-dist
find -L $NIGHTLY_BUILD_TREE -name "*.deb" | xargs cp --target-directory=.

# If we don't have a Packages file (as well as Packages.gz) we get:
# Failed to fetch http://deb/software.jessies.org/./Release  Unable to find expected entry  Packages in Meta-index file (malformed Release file?)
apt-ftparchive packages . > Packages
# gzip -9 Packages would remove the original file, and we need it.
gzip -9 < Packages > Packages.gz
apt-ftparchive release . > Release
# This has to run as martind@bluearc.com to sign with the right key.
# The key was exported with gpg --export --armor, then imported by piping that to sudo apt-get add -.
# After a move from Jessie to Buster, I needed:
# gpg2 --import ~/.gnupg/secring.gpg
# H/T https://superuser.com/a/1112703/17633
gpg -sba - - < Release > Release.gpg
#scp Packages Packages.gz Release Release.gpg software@jessies.org:~/downloads/debian/
