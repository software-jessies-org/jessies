#!/usr/bin/ruby -w

require 'fileutils.rb'
require 'pathname.rb'

# Cope with symbolic links to this script.
salma_hayek = Pathname.new("#{__FILE__}/..").realpath().dirname()
UNAME=`uname`.chomp()

def cygpath(filenameOrPath)
  if UNAME != "CYGWIN_NT-5.0"
    return filenameOrPath
  end
  args = [ "cygpath", "--windows" ]
  if filenameOrPath =~ /:/
    args.push("--path")
  end
  args.push('"' + filenameOrPath + '"')
  return `#{args.join(" ")}`.chomp
end

args = [ "java", "-Xmx1g", "-cp", cygpath("#{salma_hayek}/classes"), "e.tools.JavaHpp" ]
args.concat(ARGV)
system(*args)
