#!/bin/bash
# FIXME: rewrite this in Ruby! (The fact that Ruby isn't installed by default on Solaris or Ubuntu points to one possible advantage to keeping this in sh, or at least in having a bootstrap stage that installs Ruby.  A proper packaging system will let us specify a dependency on Ruby, so we shouldn't feel constrained.)

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

# The motivation to make the installation conditional was to stop spurious libc updates
# which usually eventually require a reboot to get the system properly working again.
# By installing the packages one at a time, we ensure that packages which can't be
# installed don't stop those which can.
installMissingExecutable() {
    # We could scrape the output of dpkg --status but -x is easy and robust under eg i18n.
    if [[ ! -x $1 ]]
    then
        # We don't care what version gets installed, so use -t to encourage apt-get to pick
        # an old version which is, we might guess, less likely to cause a libc update.
        # I haven't seen a case where this worked yet but it seems not to be harmful.
        # One day, we'll have a proper installer and all this dubious cruft can go.
        apt-get -y install -t stable $2
    fi
}

# Install various packages we need.
if test -f /etc/debian_version ; then
    apt-get update
    # Some of these are needed to build our stuff, some to run it, and some are "optional" to take best advantage of our features.
    installMissingExecutable /usr/bin/ctags-exuberant exuberant-ctags
    installMissingExecutable /usr/bin/g++ g++
    installMissingExecutable /usr/bin/make make
    installMissingExecutable /usr/bin/ri ri
    installMissingExecutable /usr/bin/ruby ruby
    installMissingExecutable /usr/bin/svn subversion
    # You definitely want ispell(1) installed. Choosing a language is difficult, because a lot of people are parochial. So only install international English ispell if they haven't already installed ispell. (Last time I looked, whichever dictionary you install last becomes the default.)
    installMissingExecutable /usr/bin/ispell iamerican
    # It's important to have a non-free JDK, because the free ones aren't finished.
    # We build this package ourselves, so it's unlikely to be updated often and to cause spurious libc updates.
    apt-get -y install sun-j2sdk1.5
    # The first of the things we'll have to install to be able to build .deb installers.
    # This particular package is unlikely to cause libc updates.
    # The need for further packages will be removed by the Build-Depends line in the .deb's control file.
    apt-get -y install build-essential
fi

# Create a directory in /usr/local for all our stuff.
mkdir -p /usr/local/www.jessies.org/ && \
cd /usr/local/www.jessies.org/ || die "making install directory"

# Download and extract the latest nightly builds.
PROJECTS="salma-hayek edit KnowAll scm terminator"
BROKEN_PROJECTS=""
WGET_OPTIONS="-C off"
if ! wget --no-cache 2>&1 | grep unrecogni[sz]ed > /dev/null
then
    WGET_OPTIONS="--no-cache"
fi
for PROJECT in $PROJECTS; do
    wget $WGET_OPTIONS -N http://software.jessies.org/$PROJECT/$PROJECT.tgz || die "downloading $PROJECT"
    rm -rf $PROJECT || die "removing old copy of $PROJECT"
    tar --no-same-owner -zxf $PROJECT.tgz || die "extracting $PROJECT"
    if ! make -C $PROJECT ; then
        BROKEN_PROJECTS="$PROJECT $BROKEN_PROJECTS"
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
    update-alternatives --install /usr/bin/x-terminal-emulator x-terminal-emulator /usr/local/bin/terminator 50
    update-alternatives --auto x-terminal-emulator
fi

if [ "$BROKEN_PROJECTS" != "" ]
then
    die "failed to build $BROKEN_PROJECTS"
fi
echo "All done!"
exit 0
