#!/usr/bin/env ruby -w

require 'tempfile.rb'

inbox_roots = "~/Library/Mail/*/INBOX.imapmbox/Messages"

filenames=`find #{inbox_roots} -name "*.emlx" -print0 | \
           xargs -0 grep -wl text/calendar`.split("\n")
filenames.each() {
  |filename|
  puts("Extracting meeting request from '#{filename}'...")
  filename =~ /\/(\d+).emlx$/
  message_id = $1

  # Pull just the .ics data out of the message.
  message = File.new(filename).read()
  message =~ /\btext\/calendar\b.*\n\n(.*)\n\n/m
  meeting_request = $1

  # iCal is clever enough (it seems) to recognize the identical
  # UID, so we don't have to bother.
  ics_filename = "/tmp/meeting_request.#{message_id}.ics"
  ics_file = File.new(ics_filename, "w")
  ics_file.puts(meeting_request)
  system("open #{ics_filename}")
}
exit(0)
