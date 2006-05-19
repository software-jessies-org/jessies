#!/bin/bash
# FIXME: rewrite this in Ruby! (The fact that Ruby isn't installed by default on Solaris or Ubuntu points to one possible advantage to keeping this in sh, or at least in having a bootstrap stage that installs Ruby.  A proper packaging system will let us specify a dependency on Ruby, so we shouldn't feel constrained.)

LOG=/tmp/install-everything.log
exec 3>&1
exec &> $LOG

restore_stdio() {
    exec 1>&3
    exec 2>&3
}

die() {
    restore_stdio
    echo $*
    echo
    echo Log was:
    cat $LOG
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

# Install various packages we need.
if test -f /etc/debian_version ; then
    apt-get update
    # When these are installed, we can get the users to switch to using them.
    # When the users are using them, we won't need much of the rest of this file.
    # (Though various parts of it would still be useful elsewhere.)
    apt-get -y install org.jessies.evergreen org.jessies.scm org.jessies.terminator
fi

# Create a directory in /usr/local for all our stuff.
mkdir -p /usr/local/www.jessies.org/ && \
cd /usr/local/www.jessies.org/ || die "making install directory"

# Download and extract the latest nightly builds.
MACHINE_PROJECT_NAMES="salma-hayek evergreen knowall scm terminator"
BROKEN_PROJECTS=""
WGET_OPTIONS="-C off"
if ! wget --no-cache 2>&1 | grep unrecogni[sz]ed > /dev/null
then
    WGET_OPTIONS="--no-cache"
fi
for MACHINE_PROJECT_NAME in $MACHINE_PROJECT_NAMES; do
    wget $WGET_OPTIONS -N http://software.jessies.org/$MACHINE_PROJECT_NAME/$MACHINE_PROJECT_NAME.tgz || die "downloading $MACHINE_PROJECT_NAME"
    PROJECT_DIRECTORY_BASE_NAME=`tar -ztf $MACHINE_PROJECT_NAME.tgz | head -1 | cut -f1 -d/`
    rm -rf $PROJECT_DIRECTORY_BASE_NAME || die "removing old copy of $PROJECT_DIRECTORY_BASE_NAME"
    tar --no-same-owner -zxf $MACHINE_PROJECT_NAME.tgz || { mv $MACHINE_PROJECT_NAME.tgz corrupt-$MACHINE_PROJECT_NAME.tgz; die "extracting corrupt-$MACHINE_PROJECT_NAME.tgz"; }
    if ! make -C $PROJECT_DIRECTORY_BASE_NAME ; then
        # A number of people, on a number of both recent and previous occasions have had one-off failures.
        # The goal here is to leave them with a working installation at the expense of a messy build mail.
        # I suspect the reason a second, manual attempt has worked, however, is that it's being run from
        # a subtly different environment.
        # So I suspect that this won't work.
        if ! make -C $PROJECT_DIRECTORY_BASE_NAME ; then
            BROKEN_PROJECTS="$MACHINE_PROJECT_NAME $BROKEN_PROJECTS"
        fi
    fi
done

# This script runs as root, so we have an opportunity to get Terminator's terminfo installed system-wide.
tic terminator/lib/terminfo/terminator.tic

# Put links to each of our shell scripts in /usr/local/bin.
# This avoids the need to mess with anyone's $PATH.
scripts=`find */bin -type f -perm +1`
for script in $scripts
do
    link_in_usr_local_bin `pwd`/$script
done

if test -f /etc/debian_version ; then
    # FIXME: We don't meet the Debian Policy's command line requirements for an x-terminal-emulator.
    # FIXME: This belongs in terminator.deb.
    update-alternatives --install /usr/bin/x-terminal-emulator x-terminal-emulator /usr/local/bin/terminator 50
    update-alternatives --auto x-terminal-emulator
fi

if [ "$BROKEN_PROJECTS" != "" ]
then
    die "failed to build $BROKEN_PROJECTS"
fi

restore_stdio
echo "Success!"
HOSTNAME=`hostname`
echo "(A complete log is in /net/$HOSTNAME$LOG.)"
exit 0
