#!/usr/bin/perl -w -n

# Rewrite text of the form "this can be done with the monkey(1) command"
# to include a link to the web version of the Mac OS X man page for that
# command.

# FIXME: only works for section 1.
# FIXME: doesn't use curl(1) to check that the man page exists.

s|(\w+)\(1\)|<a href="http://developer.apple.com/documentation/Darwin/Reference/ManPages/man1/\1.1.html">\1(1)</a>|g ; print $_
