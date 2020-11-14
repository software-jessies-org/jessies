#!/usr/bin/ruby -w

# Reports (via its status code on exit) whether we're running on a suitable macOS installation.

# This script is based on the idea at
#  http://weblog.bignerdranch.com/?p=13
# which was written in response to the complaint at
#  http://www.gigliwood.com/weblog/Cocoa/AppleBugFriday__Min.html
# about Apple's solution.
# The original only works on 10.4 and doesn't tell you what version is required or what version you have.

# Usage: ensure-suitable-mac-os-version.rb && program-requiring-10.4

# FIXME: we should take the required macOS version as parameters.
# FIXME: we should be clever enough to offer to open Software Update if you only need to go up a minor revision.

require "pathname.rb"
salma_hayek = Pathname.new(__FILE__).realpath().dirname().dirname()
require "#{salma_hayek}/bin/show-alert.rb"

# Do we have a good enough version of macOS?
actual_mac_os_version = `sw_vers -productVersion`.chomp()
if actual_mac_os_version.match(/^(\d+\.\d+)/) == nil
    show_alert("Failed to parse macOS version number out of #{actual_mac_os_version.inspect()}")
    exit(1)
end
# 10.10 (Yosemite) sorts after 10.9 (Mavericks).
tuple = $1.split(".").map() {
    |component|
    component.to_i()
}
if (tuple <=> [10, 4]) == -1
    show_alert("This application requires a newer version of macOS.", "This application requires at least macOS 10.4, but you have macOS #{actual_mac_os_version}.\n\nPlease upgrade.")
    exit(1)
end

# Everything's cool.
exit(0)
