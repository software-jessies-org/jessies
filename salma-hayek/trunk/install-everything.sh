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

mkdir -p /usr/local/www.jessies.org/ && \
cd /usr/local/www.jessies.org/ || die "making install directory"

# if i could remember how to write a list literal, i'd have a list of projects here...

wget -N http://www.jessies.org/~enh/software/edit/edit.tgz && \
wget -N http://www.jessies.org/~enh/software/salma-hayek/salma-hayek.tgz && \
wget -N http://www.jessies.org/~enh/software/scm/scm.tgz && \
wget -N http://www.jessies.org/~enh/software/terminator/terminator.tgz || die "downloading"

tar zxf edit.tgz && \
tar zxf salma-hayek.tgz && \
tar zxf scm.tgz && \
tar zxf terminator.tgz || die "extracting"

scripts=`find * -type f -maxdepth 1 -perm +1`
for script in $scripts
do
    link_in_usr_local_bin `pwd`/$script
done

echo "All done!"
exit 0
