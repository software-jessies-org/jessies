#!/bin/bash

# cron-config
# Install as service, run as yourself (otherwise today exim crashes into /var/log/cron.log), don't set CYGWIN.
# This requires your Windows password, which, judging by the prompts, you can update with passwd -R.
# In crontab -l:
# 20 09 * * * /bin/cat /home/mdorey/jessies/work/salma-hayek/lib/build/vxd0141.sh | /bin/bash --login

# exim-config
# Install as service, run under the LocalSystem account, don't set CYGWIN.
# In /etc/exim.conf:
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

# Using Samba is hard and insanely slow from Cygwin.
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

NIGHTLY_BUILD_SCRIPT=~/jessies/work/salma-hayek/bin/nightly-build.rb
NIGHTLY_BUILD_TREE=~/jessies/nightlies/
export http_proxy=http://http.corp.hds.com:8080
export https_proxy=http://http.corp.hds.com:8080
$NIGHTLY_BUILD_SCRIPT $NIGHTLY_BUILD_TREE clean native-dist
