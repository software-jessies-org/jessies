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
# On Cygwin 1.5, even if you choose to run cron as yourself, you need to grant yourself "Create a token object" privilege in Control Panel, Administrative Tools, Local Security Policy, Local Policies, User Rights Assignment.

# exim-config
# the rewrite section is empty by default
# add this on a line on its own:
# *@+local_domains ${local_part}@bluearc.com EFfrstcb

# # martind 2012-05-22
# smarthost:
# driver = manualroute
# domains = ! +local_domains
# transport = remote_smtp
# route_list = * mail.us.dev.bluearc.com byname
# host_find_failed = defer
# same_domain_copy_routing = yes
# no_more

# Anyway, I could indeed access network files but it was insanely slow to do an svn update.
# I realized that I really didn't want the Windows machine connecting over Samba and doing make clean while
# the Debian build was happening.
# So I cloned local salma-hayek and terminator repositories.
# Then I couldn't build them because "Documents and Settings" contains a space and it's basically a big waste of time
# to try to get make(1) to cope with file names containing spaces.
# It can be done but it's not worth the effort.
# Mounting C:\Documents and Settings\martind on /home/martind, however, works fine.

# When switching to a different major version of Cygwin:
# cygrunsrv --stop cygserver
# cygrunsrv --remove cygserver
# cygserver-config

# 20 09 * * * echo ~/software.jessies.org/work/salma-hayek/lib/build/mdorey.sh | bash --login

NIGHTLY_BUILD_SCRIPT=~/software.jessies.org/work/salma-hayek/bin/nightly-build.rb
NIGHTLY_BUILD_TREE=~/software.jessies.org/nightlies/
$NIGHTLY_BUILD_SCRIPT $NIGHTLY_BUILD_TREE clean native-dist
