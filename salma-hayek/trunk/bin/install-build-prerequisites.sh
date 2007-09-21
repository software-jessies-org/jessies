#!/bin/bash

PACKAGES=""

# The real build prerequisites are below.

# We use various gcc-specific flags, though we don't depend on any non-standard features..
PACKAGES="$PACKAGES g++"

# We require make-3.81.
PACKAGES="$PACKAGES make"

# We'd rather be writing this script in Ruby.
# We require 1.8 or later.
PACKAGES="$PACKAGES ruby"

# We use subversion commands during building.
PACKAGES="$PACKAGES subversion"

# Required for "gnome-startup", or anything else that uses X11 directly.
PACKAGES="$PACKAGES x-dev"
PACKAGES="$PACKAGES libx11-dev"

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
