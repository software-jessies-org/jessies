#!/usr/bin/ruby -w

require "pathname"

salma_hayek = Pathname.new(__FILE__).realpath().dirname().dirname()

require "#{salma_hayek}/lib/build/patch-to-html-email.rb"

from_address = ENV["LOGNAME"]
if from_address == ""
  from_address = `whoami`.chomp()
end
to_address = from_address
reply_to_address = nil
subject = "patch"
preamble = ""
changes = ARGF.readlines()

sendHtmlEmail(from_address, to_address, reply_to_address, subject, preamble, changes)

exit(0)
