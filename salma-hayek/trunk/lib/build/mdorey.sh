#!/bin/bash

# ssh-host-config sets up sshd on Cygwin
# Follow all the defaults and, when it's finished and says "do net start sshd", do as it says.
# Ensure a copy of all your keys is in ~/.ssh.
# Perhaps because the SYSTEM user doesn't have permission to read my private keys,
# I had to have an authorized_keys file.
# This let me login without a password but that means I can't then use Samba, per:
# http://cygwin.com/cygwin-ug-net/ntsec.html#ntsec-switch
# Meaning that this doesn't let me do the Cygwin build from the same work area as the Debian build:
#echo make -C /cygdrive/f/software.jessies.org/nightlies/terminator native-dist | ssh mdorey bash --login

# So then I did cron-config and followed all the suggestions, one of which led me to believe I'd be able to access
# files over the network.
# Cron wanted me to install exim and run exim-config.
# The prompts there all looked reassuring but left me reading /var/spool/mail/martind with less(1).
# Anyway, I could indeed access network files but it was insanely slow to do an svn update.
# I realized that I really didn't want the Windows machine connecting over Samba and doing make clean while
# the Debian build was happening.
# So I cloned local salma-hayek and terminator repositories.
# Then I couldn't build them because "Documents and Settings" contains a space and it's basically a big waste of time
# to try to get make to cope with file names containing spaces.
# It can be done but it's not worth the effort.

# exim configuration
# the rewrite section is empty by default
# add this:
# *@+local_domains ${local_part}@bluearc.com EFfrstcb

# 20 09 * * * cat ~/software.jessies.org/work/salma-hayek/lib/build/mdorey.sh | bash --login

NIGHTLY_BUILD_SCRIPT=~/software.jessies.org/work/salma-hayek/bin/nightly-build.rb
NIGHTLY_BUILD_TREE=~/software.jessies.org/nightlies/
$NIGHTLY_BUILD_SCRIPT $NIGHTLY_BUILD_TREE clean native-dist
