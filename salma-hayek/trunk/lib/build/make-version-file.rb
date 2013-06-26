#!/usr/bin/ruby -w

require 'pathname'

#
# Mercurial
#

def getMercurialVersion(directory)
  return `hg parents | grep changeset:`
end

def extractMercurialVersionNumber(versionString)
  if versionString.match(/^changeset:\s*(\d+):/)
    return $1.to_i()
  end
  return 0
end

#
# Subversion
#

def getSubversionVersion(directory)
  command = "svnversion #{directory}"
  output = `#{command}`.chomp()
  if $?.success?()
    return output
  end
  require "pathname"
  absoluteDirectory = Pathname.new(directory).realpath()
  # Skip header:
  # Filesystem           1K-blocks      Used Available Use% Mounted on
  dfOutput = `df #{absoluteDirectory} | tail -n +2`
  # df sometimes outputs one line per mount:
  # /dev/sda7            448043100 383662960  41620860  91% /u219
  # But sometimes two:
  # whitewater.us.dev.bluearc.com:/u219/martind
  #                      448043104 383662944  41620864  91% /home/martind
  if dfOutput.match(/^(\S+):(\S+)\s+\d+\s+\d+\s+\d+\s+\d+%\s+(\S+)$/) == nil
    raise Exception.new("Failed to parse NFS server out of df output:\n#{dfOutput}")
  end
  fileHost = $1
  export = $2
  mountedOn = $3
  remoteDirectory = absoluteDirectory.sub(mountedOn, export)
  command = "ssh #{fileHost} svnversion #{remoteDirectory}"
  $stderr.puts("Trying #{command}...") # In case ssh(1) prompts for a password.
  output = `#{command}`.chomp()
  return output
end

def extractSubversionVersionNumber(versionString)
  # The second number, if there are two, is the more up-to-date.
  # (In Subversion's model, commit doesn't necessarily require update.)
  if versionString.match(/^(?:\d+:)?(\d+)/)
    return $1.to_i()
  end
  raise Exception.new("Failed to parse version out of svnversion output:\n#{versionString}")
end

# --------------------------------------------------------------------------------------------------------

def getWorkingCopyVersion(directory)
  if (Pathname.new(directory) + ".hg").exist?()
    return extractMercurialVersionNumber(getMercurialVersion(directory))
  elsif (Pathname.new(directory) + ".svn").exist?()
    return extractSubversionVersionNumber(getSubversionVersion(directory))
  else
    # An end-user building from source, maybe?
    # Returning 0 as the version number lets us build without warnings.
    return 0
  end
end

# Windows's installer's broken idea of "version number" forces us to have a
# version number of the form "a.b.c", and also forces us to ensure that
# a <= 255, b <= 255, and c <= 65535. If we don't, upgrading (replacing an
# old version with a new version) won't work. See here for the depressing
# details:
# http://msdn.microsoft.com/library/default.asp?url=/library/en-us/msi/setup/productversion.asp
# This script takes advantage of the fact that we have enough bits (8+8+16=32)
# to encode our project and salma hayek revision numbers, even if they're not
# conveniently arranged.
def mungeVersions(projectVersionNumber, salmaHayekVersionNumber)
  fields = []
  # The third field is called the build version or the update version and has a maximum value of 65,535.
  fieldSize = 64 * 1024
  # We try to separate the project_root and salma_hayek versions into different fields.
  unifiedVersion = projectVersionNumber * fieldSize + salmaHayekVersionNumber
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
  return fields.reverse().join(".")
end

def makeVersionString(projectRootDirectory, salmaHayekRootDirectory)
  return mungeVersions(getWorkingCopyVersion(projectRootDirectory), getWorkingCopyVersion(salmaHayekRootDirectory))
end

# Despite its name, run as a script, this generates the contents for "build-revision.txt".
if __FILE__ == $0
  if ARGV.length() != 2
    $stderr.puts("usage: #{File.basename($0)} <project-root-dir> <salma-hayek-root-dir>")
    exit(1)
  end
  projectRootDirectory = ARGV.shift()
  salmaHayekDirectory = ARGV.shift()
  projectVersion = getWorkingCopyVersion(projectRootDirectory)
  salmaHayekVersion = getWorkingCopyVersion(salmaHayekDirectory)
  
  require "time.rb"
  puts(Time.now().iso8601())
  puts(projectVersion)
  puts(salmaHayekVersion)
  puts(mungeVersions(projectVersion, salmaHayekVersion))
end
