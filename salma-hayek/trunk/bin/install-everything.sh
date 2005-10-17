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

if ! test -w . ; then
  die "cannot write to /usr/local - this script needs to be run as root"
fi

# Install Java in /usr/local, and put links to java and javac in /usr/local/bin.
java_installer=/net/mirror/mirror-link/java/jdk-1_5_0_03-linux-i586.bin
export JAVA_HOME=/usr/local/jdk1.5.0_03
if sudo -u devadmin test -f $java_installer && ! test -d $JAVA_HOME ; then
    sudo -u devadmin cp $java_installer /tmp/jdk &&
    /tmp/jdk ||
    die "installing Java"
fi
if test -d $JAVA_HOME ; then
    link_in_usr_local_bin $JAVA_HOME/bin/java
    link_in_usr_local_bin $JAVA_HOME/bin/javac
    for BROWSER in mozilla mozilla-firefox; do
        if test -d /usr/lib/$BROWSER/plugins; then
            ln -s $JAVA_HOME/jre/plugin/i386/ns7/libjavaplugin_oji.so /usr/lib/$BROWSER/plugins
        fi
    done
fi

if test -f /etc/debian_version ; then
    if [[ ! -x /usr/bin/ctags-exuberant || ! -x /usr/bin/ri || ! -x /usr/bin/svn ]]
    then
        apt-get update
        apt-get -y install exuberant-ctags ri subversion
    fi
fi

# Create a directory in /usr/local for all our stuff.
mkdir -p /usr/local/www.jessies.org/ && \
cd /usr/local/www.jessies.org/ || die "making install directory"

# Download and extract the latest nightly builds.
PROJECTS="salma-hayek edit scm terminator"
BROKEN_PROJECTS=""
WGET_OPTIONS="-C off"
if ! wget --no-cache 2>&1 | grep unrecogni[sz]ed > /dev/null
then
    WGET_OPTIONS="--no-cache"
fi
for PROJECT in $PROJECTS; do
    wget $WGET_OPTIONS -N http://www.jessies.org/~software/downloads/$PROJECT/$PROJECT.tgz || die "downloading $PROJECT"
    rm -rf $PROJECT || die "removing old copy of $PROJECT"
    tar --no-same-owner -zxf $PROJECT.tgz || die "extracting $PROJECT"
    if ! make -C $PROJECT native
    then
        TARGET_OS=`./salma-hayek/bin/target-os.rb`
        if wget $WGET_OPTIONS -N http://www.jessies.org/~software/downloads/$PROJECT/$PROJECT-$TARGET_OS.tgz
        then
            tar --no-same-owner -zxf $PROJECT-$TARGET_OS.tgz || die "extracting $PROJECT-$TARGET_OS"
        else
            BROKEN_PROJECTS="$PROJECT $BROKEN_PROJECTS"
        fi
    fi
done

# Although /proc/mounts is Linux-specific, it doesn't hang when one of the mounts is
# currently unavailable.
cat /proc/mounts | perl -ne '
if (m@^/dev/\w+ (/home/\w+) @) {
    system("echo rm -f $1/.terminal-logs/.terminator-server-port | bash -x");
}'
#rm -f ~/.e.edit.Edit/edit-server-port
tic terminator/doc/terminfo/terminator.tic

# Put links to each of our shell scripts in /usr/local/bin.
# This avoids the need to mess with anyone's $PATH.
scripts=`find */bin -type f -perm +1`
for script in $scripts
do
    link_in_usr_local_bin `pwd`/$script
done

if test -f /etc/debian_version ; then
    update-alternatives --install /usr/bin/x-terminal-emulator x-terminal-emulator /usr/local/bin/terminator 50
    update-alternatives --auto x-terminal-emulator
fi

if [ "$BROKEN_PROJECTS" != "" ]
then
    die "failed to build $BROKEN_PROJECTS"
fi
echo "All done!"
exit 0
