#!/bin/bash

# Use this as the command in your crontab:
# echo install-everything.sh | sudo bash --login

# apt-get's output is routinely splattered with incomprehensible errors.
apt-get update &> /dev/null

apt-get -y install org.jessies.{evergreen,scm,terminator}

# FIXME: We don't meet the Debian Policy's command line requirements for an x-terminal-emulator.
# FIXME: This belongs in terminator.deb.
update-alternatives --install /usr/bin/x-terminal-emulator x-terminal-emulator /usr/bin/terminator 50
update-alternatives --auto x-terminal-emulator

# Hard-wiring the existing path to the script in our users' crontabs would only lead to worse problems.
ln -f -s /usr/share/software.jessies.org/terminator/Resources/salma-hayek/bin/install-everything.sh /usr/local/bin/install-everything.sh

# This will clean up our old installations.
# I expect we want to run it manually rather than every night from here.
exit 0
rm -rf /usr/local/www.jessies.org/
find /usr/local -type l | xargs ls -l | perl -ne 'm@(/usr/local/.*) -> /usr/local/www.jessies.org/@ && print("$1\n")' | xargs rm
