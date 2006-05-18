#!/bin/bash
# Add this to /etc/apt/sources.list on every machine:
# deb http://deb/software.jessies.org/ ./
# "deb" is a DNS alias for a machine in the local domain running apache on which the following command has been run:
# sudo ln -s $DEB_DIRECTORY /var/www/software.jessies.org

# Run the latest version of the nightly build script, rather than the version from yesterday.
# This has the advantage that the update won't overwrite the running script - which Ruby doesn't like.
~/software.jessies.org/work/salma-hayek/bin/nightly-build.rb clean native-dist
DEB_DIRECTORY=/u/u154/software.jessies.org/
mkdir -p $DEB_DIRECTORY
cd $DEB_DIRECTORY
find ~/software.jessies.org/nightlies/ -name "*.deb" | xargs cp --target-directory=.
dpkg-scanpackages . /dev/null | gzip -9 > Packages.gz
scp Packages.gz software@jessies.org:~/downloads/debian/Packages.gz
