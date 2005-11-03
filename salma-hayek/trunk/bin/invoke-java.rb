#!/usr/bin/ruby -w

require 'fileutils.rb'
require 'pathname.rb'

def invoke_java(dock_name, dock_icon, class_name)
  salma_hayek = Pathname.new("#{__FILE__}/..").realpath().dirname()
  # Cope with symbolic links to this script.
  require "#{salma_hayek}/bin/target-os.rb"
  
  args = [ "java", "-cp", "#{salma_hayek}/classes" ]
  if target_os() == "Darwin"
    args << "-Xdock:name=#{dock_name}"
    args << "-Xdock:icon=#{dock_icon}"
  end
  args << class_name
  args.concat(ARGV)
  return system(*args)
end

if __FILE__ == $0
  # Just an example.
  invoke_java("invoke-java", "", "e/util/Launcher")
end
