#!/bin/bash

die() {
    echo $*
    exit 1
}

link_in_usr_local_bin() {
    echo "adding a link to $* in /usr/local/bin"
    ln -fs $* /usr/local/bin || die "couldn't link $*"
}

cd /usr/local/ || die "couldn't cd to /usr/local"

# Install Java in /usr/local, and put links to java and javac in /usr/local/bin.
java_installer=/net/mirror/mirror-link/java/jdk-1_5_0_01-linux-i586.bin
JAVA_INSTALL_DIR=/usr/local/jdk1.5.0_01
if test -f $java_installer && ! test -d $JAVA_INSTALL_DIR ; then
    $java_installer || die "installing Java"
fi
if test -d $JAVA_INSTALL_DIR ; then
    link_in_usr_local_bin $JAVA_INSTALL_DIR/bin/java
    link_in_usr_local_bin $JAVA_INSTALL_DIR/bin/javac
    for BROWSER in mozilla mozilla-firefox; do
        if test -d /usr/lib/$BROWSER/plugins; then
            ln -s $JAVA_INSTALL_DIR/jre/plugin/i386/ns7/libjavaplugin_oji.so /usr/lib/$BROWSER/plugins
        fi
    done
fi

# Create a directory in /usr/local for all our stuff.
mkdir -p /usr/local/www.jessies.org/ && \
cd /usr/local/www.jessies.org/ || die "making install directory"

# Download and extract the latest nightly builds.
PROJECTS="salma-hayek edit scm terminator"
for PROJECT in $PROJECTS; do
    rm -f $PROJECT.tgz
    wget -C off -N http://www.jessies.org/~enh/software/$PROJECT/$PROJECT.tgz || die "downloading $PROJECT"
    rm -rf $PROJECT || die "removing old copy of $PROJECT"
    tar zxf $PROJECT.tgz || die "extracting $PROJECT"
    make -C $PROJECT || die "building $PROJECT"
done

# Put links to each of our shell scripts in /usr/local/bin.
# This avoids the need to mess with anyone's $PATH.
scripts=`find */bin -type f -perm +1`
for script in $scripts
do
    link_in_usr_local_bin `pwd`/$script
done

if test -f /etc/debian_version ; then
    apt-get update
    apt-get -y install exuberant-ctags ri
fi

echo "All done!"
exit 0
