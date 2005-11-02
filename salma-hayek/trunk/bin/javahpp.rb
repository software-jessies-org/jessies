#!/usr/bin/ruby -w

require 'fileutils.rb'
require 'pathname.rb'

# Cope with symbolic links to this script.
salma_hayek = Pathname.new("#{__FILE__}/..").realpath().dirname()
require "#{salma_hayek}/bin/target-os.rb"

def cygpath(filenameOrPath)
  if target_os() != "Cygwin"
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
