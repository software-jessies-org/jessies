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
  attr_accessor(:dock_name)
  attr_accessor(:dock_icon)
  attr_accessor(:launcher)
  attr_accessor(:log_filename)

  def initialize(name, class_name)
    @dock_name = name
    @dock_icon = ""
    @class_name = class_name
    @launcher = "java"
    @log_filename = ""

    # Cope with symbolic links to this script.
    @project_root = Pathname.new("#{$0}/..").realpath().dirname()
    @salma_hayek = Pathname.new("#{@project_root}/../salma-hayek").realpath()
    require "#{@salma_hayek}/bin/target-os.rb"

    @extra_java_arguments = []

    init_default_heap_size()
    init_default_class_path()
    @library_path = []
  end

  def add_class_path_entries(new_entries)
    @class_path.concat(new_entries)
  end
  
  def add_library_path_entries(new_entries)
    @library_path.concat(new_entries)
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
      @class_path << "#{@salma_hayek}/AppleJavaExtensions.jar"

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
  
  def getExtraPathComponents()
    subProjectRoots = [ @project_root, @salma_hayek ]
    # Premature: the make code to build the per-target common bin directory doesn't exist yet.
    executableSubDirectories = [ "bin", ".generated/#{target_os()}/bin" ]
    extraPathComponents = []
    subProjectRoots.each() {
      |subProjectRoot|
      executableSubDirectories.each() {
        |executableSubDirectory|
        directory = "#{subProjectRoot}/#{executableSubDirectory}"
        if FileTest.directory?(directory)
          extraPathComponents << directory
        end
      }
    }
    return extraPathComponents
  end
  
  def subvertPath()
    # When run from Cygwin, we need to use colon as the PATH separator, rather than the native semi-colon.
    originalPathComponents = ENV["PATH"].split(":")
    newPathComponents = []
    # Putting ourselves on the end of the PATH means that the user can override
    # our default echo-local-non-source-directory-pattern and avoids hubristically increasing the length
    # of successful PATH searches in, for example, terminator shells.
    # Yes, perhaps I am protesting too much.
    newPathComponents.concat(originalPathComponents)
    newPathComponents.concat(getExtraPathComponents())
    # uniq() seems to do The Right Thing with unsorted duplicates:
    # removing the later ones, preserving order.
    # @salma_hayek may be the same as @project_root, particular with installed versions.
    ENV["PATH"] = newPathComponents.uniq().join(":")
  end
  
  def invoke(extra_app_arguments = [])
    args = [ @launcher ]
    # Set the class path directly with a system property rather than -cp so
    # that our custom Win32 launcher doesn't have to convert between the two
    # forms.
    # cygpath's argument is a cygwin filename or path, so it should have a cygwin
    # path separator - which is colon, not the Windows native semicolon.
    # cygpath returns a native path - for the benefit of the non-cygwin JVM - but it
    # doesn't take one.
    # Cygwin's ruby's File::PATH_SEPARATOR is colon.
    # This is (now, if it hasn't always been) the only caller which takes advantage of
    # the --path conditional in cygpath.
    args << "-Djava.class.path=#{cygpath(@class_path.uniq().join(":"))}"
    args << "-Djava.library.path=#{cygpath(@library_path.uniq().join(":"))}"
    applicationEnvironmentName = @dock_name.upcase()
    logging = ENV["DEBUGGING_#{applicationEnvironmentName}"] == nil && @log_filename != ""
    if logging
      File.new(@log_filename, "w").close() # Like touch(1).
      args << "-De.util.Log.filename=#{cygpath(@log_filename)}"
    end
    args << "-Xmx#{@heap_size}"
    if target_os() == "Darwin"
      args << "-Xdock:name=#{@dock_name}"
      args << "-Xdock:icon=#{@dock_icon}"
    end
    args.concat(@extra_java_arguments)
    args << @class_name
    args.concat(extra_app_arguments)
    args.concat(ARGV)
    #$stderr.puts(args)
    subvertPath()
    failed = system(*args) == false
    if failed && logging
      puts(File.new(@log_filename).readlines())
    end
  end
end

if __FILE__ == $0
  # Just an example.
  invoker = Java.new("e/util/Launcher")
  invoker.invoke()
end
