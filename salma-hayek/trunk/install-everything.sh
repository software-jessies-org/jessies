#!/bin/bash

die() {
    echo $*
    exit 1
}

link_in_usr_local_bin() {
    echo "adding a link to $* in /usr/local/bin"
    ln -fs $* /usr/local/bin || die "couldn't link $*"
}

# Install Java in /usr/local, and put links to java and javac in /usr/local/bin.
cd /usr/local/ && \
/home/elliotth/download/jdk-1_5_0-beta2-linux-i586.bin || die "installing Java"
link_in_usr_local_bin /usr/local/jdk1.5.0/bin/java
link_in_usr_local_bin /usr/local/jdk1.5.0/bin/javac

# Create a directory in /usr/local for all our stuff.
mkdir -p /usr/local/www.jessies.org/ && \
cd /usr/local/www.jessies.org/ || die "making install directory"

# Download and extract the latest nightly builds.
PROJECTS="salma-hayek edit scm terminator"
for PROJECT in $PROJECTS; do
    wget -N http://www.jessies.org/~enh/software/$PROJECT/$PROJECT.tgz || die "downloading $PROJECT"
    rm -rf $PROJECT || die "removing old copy of $PROJECT"
    tar zxf $PROJECT.tgz || die "extracting $PROJECT"
done

# Put links to each of our shell scripts in /usr/local/bin.
# This avoids the need to mess with anyone's $PATH.
scripts=`find * -type f -maxdepth 1 -perm +1`
for script in $scripts
do
    link_in_usr_local_bin `pwd`/$script
done

echo "All done!"
exit 0
