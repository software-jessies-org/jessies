#!/usr/bin/env ruby -w

# Reports (via its status code on exit) whether we're running on a suitable
# Mac OS installation.

# This script is based on the idea at
#  http://weblog.bignerdranch.com/?p=13
# which was written in response to the complaint at
#  http://www.gigliwood.com/weblog/Cocoa/AppleBugFriday__Min.html
# about Apple's solution (which only works on 10.4 and doesn't tell you what
# version is required or what version you have).

# We look like the best parts of each, I think, and also include information
# about what version you have in addition to what version is needed. Although
# a user may well know what exact version of Mac OS they're running (though I'm
# not convinced) they probably won't know what exact version of Apple's Java
# they have.

# Usage: ensure-suitable-mac-os-version.rb && program-requiring-10.4-with-Java-5.0

# FIXME: we should take the required Mac OS and Java versions as parameters.
# FIXME: we should be clever enough to offer to open Software Update if you only need to go up a minor revision.

def informational_alert(title, message)
    # The whole point of this script is to cope with old software, and "display alert" is only available on 10.4.
    # Before then we need "display dialog".
    # See http://daringfireball.net/2005/10/css_checker_101 for an explanation of this work-around.
    # Running osascript(1) twice is slow, but slightly less unclear and who cares about performance in a failure case anyway?
    has_display_alert = `osascript -e 'property AS_VERSION_1_10 : 17826208' -e '((system attribute \"ascv\") >= AS_VERSION_1_10)'`.chomp() == "true"
    if has_display_alert
        display_command = "display alert \"#{title}\" message \"#{message}\" as informational"
    else
        display_command = "display dialog \"#{title}\" & return & return & \"#{message}\" buttons { \"OK\" } default button 1 with icon note"
    end
    system("osascript -e 'tell application \"Finder\"' -e 'activate' -e '#{display_command}' -e 'end tell' > /dev/null")
end

# Do we have a good enough version of Mac OS?
actual_mac_os_version = `sw_vers -productVersion`.chomp()
if actual_mac_os_version.match(/^10\.[4-9]/) == nil
    informational_alert("This application requires a newer version of Mac OS X.", "This application requires Mac OS 10.4, but you have Mac OS #{actual_mac_os_version}.\n\nPlease upgrade.")
    exit(1)
end

# Do we have a good enough version of Java?
actual_java_version = `java -fullversion 2>&1`.chomp()
actual_java_version.match(/java full version "(.*)"/)
actual_java_version = $1
if actual_java_version.match(/^1\.[5-9]\.0/) == nil
    informational_alert("This application requires a newer version of Java.", "This application requires Java 5, but you have Java #{actual_java_version} for Mac OS #{actual_mac_os_version}.\n\nPlease upgrade.")
    exit(1)
end

# Everything's cool.
exit(0)
