#!/bin/bash

# BlueArc's local copy of the archive:
DEB_DIRECTORY=/u/u154/software.jessies.org/
# deb http://deb/software.jessies.org/ ./
# sudo apt-key add - < $DEB_DIRECTORY/software.jessies.org.gpg
#
# "deb" is a DNS alias for a machine in the local domain running apache on which the following command has been run:
# sudo ln -s $DEB_DIRECTORY /var/www/software.jessies.org

# When I built on wide, once, it upgraded my work areas to Subversion 1.4.
# Wide is amd64, which is only supported in etch, which only has svn 1.4.
# ithaki runs stable, which is currently sarge, which only has svn 1.1.

# This was my solution:

# /net/ithaki/usr/local/bin/svn now contains:
# #!/bin/bash
# LD_LIBRARY_PATH=/net/duezer/usr/lib:$LD_LIBRARY_PATH exec /net/duezer/usr/bin/svn "$@"

# /net/ithaki/usr/local/bin/svnversion now contains:
# #!/bin/bash
# LD_LIBRARY_PATH=/net/duezer/usr/lib:$LD_LIBRARY_PATH exec /net/duezer/usr/bin/svnversion "$@"

# Now all my svn updates on ithaki terminate with "Killed by signal 15" (SIGTERM) but seem to work.

mkdir -p $DEB_DIRECTORY
cd $DEB_DIRECTORY
# Remove any .debs with obsolete names.
rm *.deb

# Run the latest version of the nightly build script, rather than the version from yesterday.
# This has the advantage that the update won't overwrite the running script - which Ruby doesn't like.
NIGHTLY_BUILD_SCRIPT=~martind/software.jessies.org/work/salma-hayek/bin/nightly-build.rb
$NIGHTLY_BUILD_SCRIPT clean native-dist
echo $NIGHTLY_BUILD_SCRIPT native-dist | ssh wide bash --login
find ~martind/software.jessies.org/nightlies/ -name "*.deb" | xargs cp --target-directory=.

# If we don't have a Packages file (as well as Packages.gz) we get:
# Failed to fetch http://deb/software.jessies.org/./Release  Unable to find expected entry  Packages in Meta-index file (malformed Release file?)
apt-ftparchive packages . > Packages
gzip -9 < Packages > Packages.gz
apt-ftparchive release . > Release
# This has to run as martind@bluearc.com to sign with the right key.
gpg -sba - - < Release > Release.gpg
scp Packages Packages.gz Release Release.gpg software@jessies.org:~/downloads/debian/
