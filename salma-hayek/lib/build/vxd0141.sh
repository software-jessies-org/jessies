#!/bin/bash

# Work around HDS's firewall's newfound denial of ssh access with:
# http://stackoverflow.com/a/8081292/18096

# Work around drive.rb's requirement to avoid Ruby 1.8:
# ln -s ruby.exe /usr/bin/ruby1.9.1.exe

# Download the last JDK 6, so we can find a BOOT_JDK.

# cron-config
# Install as service, run as yourself (otherwise today exim crashes into /var/log/cron.log), don't set CYGWIN.
# This requires your Windows password, which, judging by the prompts, you can update with passwd -R.
# In crontab -l:
# 20 09 * * * /bin/cat /home/mdorey/jessies/work/salma-hayek/lib/build/vxd0141.sh | /bin/bash --login

# exim-config
# Install as service, run under the LocalSystem account, don't set CYGWIN.
# It asks whether the primary hostname is all in lower case.
# You say "yes", it says that Exim will auto-discover that.
# It won't.
# You need a line in /etc/exim.conf saying:
# primary hostname = vxd0141.corp.hds.com
# In /etc/exim.conf:
# the rewrite section is empty by default
# add this on a line on its own:
# *@+local_domains ${local_part}@bluearc.com EFfrstcb
# Yes, I really do mean bluearc.com, not hds.com.

# # martind 2012-05-22
# smarthost:
# driver = manualroute
# #domains = ! +local_domains
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
# cygrunsrv --stop exim; cygrunsrv --stop cron; cygrunsrv --stop cygserver
# cygrunsrv --remove cygserver
# cygserver-config
# cygrunsrv --start exim
# cygrunsrv --start cron

# You really want the --no flags, as the documentation is enormous.
# And, oh teh irony, who's going to do anything other than Google for answers anyway?
# We'll want:
# http://www.rubydoc.info/github/google/google-api-ruby-client/file/MIGRATING.md
# ... once we can abandon Ruby 1.9 - pfuff!
# We haven't even quite managed to abandon Debian Squeeze.
# That defaults to Ruby 1.8, though we do make it use Ruby 1.9.
# gem install --no-ri --no-rdoc --version "< 0.9" google-api-client

# See the note in salma-hayek/lib/build/drive.rb about "invalid_grant".

NIGHTLY_BUILD_SCRIPT=~/jessies/work/salma-hayek/bin/nightly-build.rb
NIGHTLY_BUILD_TREE=~/jessies/nightlies/
export http_proxy=http://http.corp.hds.com:8080
export https_proxy=http://http.corp.hds.com:8080
$NIGHTLY_BUILD_SCRIPT $NIGHTLY_BUILD_TREE clean native-dist
