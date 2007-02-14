#!/bin/bash

# Use this as the command in your crontab:
# echo install-everything.sh | sudo bash --login

# apt-get's output is routinely splattered with incomprehensible errors.
apt-get update &> /dev/null

# aptitude likes to remove packages which it thinks were installed automatically and which are no longer used by other packages.
# Sometimes people have come to rely on those packages.
# apt-get insists on confirmation of your willingness to install unauthenticated packages, even on upgrade.
# aptitude will happily install unauthenticated packages (at the moment).
apt-get -y install org.jessies.{evergreen,scm,terminator}

# FIXME: We don't meet the Debian Policy's command line requirements for an x-terminal-emulator.
# FIXME: This belongs in terminator.deb.
update-alternatives --install /usr/bin/x-terminal-emulator x-terminal-emulator /usr/bin/terminator 50
update-alternatives --auto x-terminal-emulator

# Hard-wiring the existing path to the script in our users' crontabs would only lead to worse problems.
ln -f -s /usr/share/software.jessies.org/terminator/Resources/salma-hayek/bin/install-everything.sh /usr/local/bin/install-everything.sh

exit 0
