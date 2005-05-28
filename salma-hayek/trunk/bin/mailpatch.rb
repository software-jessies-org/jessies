#!/usr/bin/ruby -w

require 'pathname'

script_path=Pathname.new(__FILE__).realpath.dirname
$: << script_path.dirname

require 'patch-to-html-email.rb'

from_address=ENV['LOGNAME']
if from_address == ""
  from_address=`whoami`.chomp
end
to_address=from_address
reply_to_address=to_address # Use "nil" to reply to author.
subject="patch"
preamble=""
changes = $<.readlines().map() {
    |line|
    line.chomp()
}

patchToHtmlEmail(from_address, to_address, reply_to_address, subject, preamble, changes)

exit(0)
