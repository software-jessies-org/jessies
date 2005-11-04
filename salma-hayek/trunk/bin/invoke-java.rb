#!/usr/bin/ruby -w

require 'fileutils.rb'
require 'pathname.rb'

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

def invoke_java(dock_name, dock_icon, class_name, extra_arguments)
  project_root = Pathname.new("#{$0}/..").realpath().dirname()
  salma_hayek = Pathname.new("#{project_root}/../salma-hayek").realpath()
  
  # Cope with symbolic links to this script.
  require "#{salma_hayek}/bin/target-os.rb"
  
  heap_size="1g"
  if target_os() == "Cygwin"
    heap_size="100m"
  end
  
  class_path = [ "#{project_root}/classes", "#{salma_hayek}/classes" ].uniq()
  if target_os() != "Darwin"
    class_path << "#{salma_hayek}/MRJ141Stubs.jar"
  end
  # Until Java 6, we need the back-ported SwingWorker.
  class_path << "#{salma_hayek}/swing-worker.jar"
  
  args = [ "java", "-Xmx#{heap_size}", "-cp", cygpath(class_path.join(":")) ]
  if target_os() == "Darwin"
    args << "-Xdock:name=#{dock_name}"
    # Does it matter that we set this to the empty string where previously we left the icon argument off?
    args << "-Xdock:icon=#{dock_icon}"
  end
  args << class_name
  args.concat(extra_arguments)
  args.concat(ARGV)
  return system(*args)
end

if __FILE__ == $0
  # Just an example.
  invoke_java("invoke-java", "", "e/util/Launcher", [])
end
