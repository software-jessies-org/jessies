#!/bin/bash

# apt-get's output is routinely splattered with incomprehensible errors.
apt-get update &> /dev/null

apt-get -y install org.jessies.{evergreen,scm,terminator}

# FIXME: We don't meet the Debian Policy's command line requirements for an x-terminal-emulator.
# FIXME: This belongs in terminator.deb.
update-alternatives --install /usr/bin/x-terminal-emulator x-terminal-emulator /usr/bin/terminator 50
update-alternatives --auto x-terminal-emulator
