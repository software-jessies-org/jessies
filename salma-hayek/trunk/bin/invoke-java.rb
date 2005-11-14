#!/usr/bin/ruby -w

require "fileutils.rb"
require "pathname.rb"

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

class Java
  attr_accessor :dock_name, :dock_icon, :launcher

  def initialize(name, class_name)
    @dock_name = name
    @dock_icon = ""
    @class_name = class_name
    @launcher = "java"

    # Cope with symbolic links to this script.
    @project_root = Pathname.new("#{$0}/..").realpath().dirname()
    @salma_hayek = Pathname.new("#{@project_root}/../salma-hayek").realpath()
    require "#{@salma_hayek}/bin/target-os.rb"

    @extra_java_arguments = []

    init_default_heap_size()
    init_default_class_path()
  end

  def add_class_path_entries(new_entries)
    @class_path.concat(new_entries)
  end

  def add_extra_java_arguments(new_java_arguments)
    @extra_java_arguments.concat(new_java_arguments)
  end

  def init_default_heap_size()
    # Default heap size.
    @heap_size = "1g"
    if target_os() == "Cygwin"
      @heap_size = "100m"
    end
  end

  def init_default_class_path()
    @class_path = [ "#{@project_root}/classes", "#{@salma_hayek}/classes" ]

    if target_os() != "Darwin"
      @class_path << "#{salma_hayek}/MRJ141Stubs.jar"

      require "#{@salma_hayek}/bin/find-jdk-root.rb"
      jdk_root=find_jdk_root()
  
      # This doesn't exist on Mac OS X but the classes are on the boot class
      # path anyway.
      # There's a bug against Java 6 to add these classes to its boot class
      # path too.
      tools_jar="#{jdk_root}/lib/tools.jar"
      if Pathname.new(tools_jar).exist?
        @class_path << tools_jar
      end
    end

    # Until Java 6, we need the back-ported SwingWorker.
    @class_path << "#{@salma_hayek}/swing-worker.jar"
  end

  def invoke(extra_app_arguments = [])
    args = [ @launcher, "-Xmx#{@heap_size}", "-cp", cygpath(@class_path.uniq().join(":")) ]
    if target_os() == "Darwin"
      args << "-Xdock:name=#{@dock_name}"
      args << "-Xdock:icon=#{@dock_icon}"
    end
    args.concat(@extra_java_arguments)
    args << @class_name
    args.concat(extra_app_arguments)
    args.concat(ARGV)
    #$stderr.puts(args)
    return system(*args)
  end
end

if __FILE__ == $0
  # Just an example.
  invoker = Java.new("e/util/Launcher")
  invoker.invoke()
end
