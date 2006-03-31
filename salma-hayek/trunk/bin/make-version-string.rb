#!/usr/bin/env ruby -w

# Despite its name, this script generates "build-revision.txt".

# Win32's installer's broken idea of "version number" forces us to have a
# version number of the form "a.b.c", and also forces us to ensure that
# a <= 255, b <= 255, and c <= 65535. If we don't, upgrading (replacing an
# old version with a new version) won't work. See here for the depressing
# details:
# http://msdn.microsoft.com/library/default.asp?url=/library/en-us/msi/setup/productversion.asp
# This script takes advantage of the fact that we have enough bits (8+8+16=32)
# to encode our project and salma hayek revision numbers, even if they're not
# conveniently arranged.

def getSubversionVersion(directory)
  command = "svnversion #{directory}"
  # svnversion is insanely slow on Cygwin using a samba share.
  if `uname` =~ /CYGWIN/ && directory.match(/^\/cygdrive\/([a-z])\/(.*)/)
    drive = $1.upcase()
    pathWithinMappedDrive = $2
    user = `whoami`.chomp()
    # "net use" tells us what machine to ssh to. martind saw one line:
    # OK           F:        \\duezer\martind          Microsoft Windows Network
    # elliotth saw two lines:
    # OK           U:        \\bertha.us.dev.bluearc.com\elliotth
    #                                                  Microsoft Windows Network
    if `net use`.match(/^OK\s+#{drive}:\s+\\\\(\S+)\\#{user}\b/)
      fileHost = $1
      # We assume that the drive is mapped to the user's home directory.
      command = "ssh #{fileHost} svnversion /home/#{user}/#{pathWithinMappedDrive}"
      $stderr.puts(command) # In case ssh(1) prompts for a password.
    end
  end
  return `#{command}`.chomp()
end

def extractVersionNumber(versionString)
  # The second number, if there are two, is the more up-to-date.
  # (In Subversion's model, commit doesn't necessarily require update.)
  if versionString.match(/^(?:\d+:)?(\d+)/)
    return $1.to_i()
  end
  return nil
end

project_root = ARGV.shift()
salma_hayek = ARGV.shift()

require "time.rb"
puts(Time.now().iso8601())
projectVersionString = getSubversionVersion(project_root)
puts(projectVersionString)
salmaHayekVersionString = getSubversionVersion(salma_hayek)
puts(salmaHayekVersionString)

fields = []
# The third field is called the build version or the update version and has a maximum value of 65,535.
fieldSize = 64 * 1024
# We try to separate the project_root and salma_hayek versions into different fields.
unifiedVersion = extractVersionNumber(projectVersionString) * fieldSize + extractVersionNumber(salmaHayekVersionString)
remainder = unifiedVersion
field = remainder % fieldSize
remainder /= fieldSize
fields.push(field)
# The second field is the minor version and has a maximum value of 255.
fieldSize = 256
field = remainder % fieldSize
remainder /= fieldSize
fields.push(field)
# The first field is the major version and has a maximum value of 255.
fieldSize = 256
field = remainder % fieldSize
remainder /= fieldSize
fields.push(field)

puts(fields.reverse.join("."))
