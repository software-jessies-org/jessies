#!/bin/bash

PACKAGES=""

# FIXME: Shouldn't these be in Evergreen's Depends list?
# They would go more accurately in Recommends but who installs all the recommended packages?
PACKAGES="$PACKAGES exuberant-ctags"
PACKAGES="$PACKAGES ri"

# We use various gcc-specific flags, though we don't depend on any non-standard features..
PACKAGES="$PACKAGES g++"

# We require make-3.81.
PACKAGES="$PACKAGES make"

# We'd rather be writing this script in Ruby.
PACKAGES="$PACKAGES ruby"

# We use subversion commands during building.
PACKAGES="$PACKAGES subversion"

# The history is murky but I think these were required for finish-gnome-startup.
PACKAGES="$PACKAGES x-dev"
PACKAGES="$PACKAGES libx11-dev"

# You definitely want ispell(1) installed. Choosing a language is difficult, because a lot of people are parochial. So only install international English ispell if they haven't already installed ispell. (Last time I looked, whichever dictionary you install last becomes the default.)
PACKAGES="$PACKAGES iamerican"

# It's important to have a non-free JDK, because the free ones aren't finished.
# Debian sid now includes a Sun JDK called sun-java5-jdk.
# Most of our users are on sarge but none of our builders are.
# Sarge had a sun-j2sdk1.5 source package which we once knew how to build.
PACKAGES="$PACKAGES sun-java5-jdk"

# To build .deb installers.
PACKAGES="$PACKAGES build-essential"
PACKAGES="$PACKAGES fakeroot"

# The nightly build now builds an .rpm.
PACKAGES="$PACKAGES alien"

# FIXME: it would be nice if we could reliably test whether we need to do this because it will upgrade all packages, even if they're already installed.
# We used to use apt-get install -t stable to discourage libc updates but that was back when we were running it automatically every night.
sudo aptitude install $PACKAGES
