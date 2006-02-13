#!/usr/bin/ruby -w

require "fileutils.rb"
require "pathname.rb"

# Takes a POSIX pathname and turns it into a Win32 pathname if we're on Win32.
# Returns the original pathname on any other OS.
def cygpath(pathname)
  if target_os() != "Cygwin"
    return pathname
  end
  
  # We know we're on Win32, and we're assuming we have Cygwin.
  # If we run cygpath(1) from a non-console Win32 application, we'll cause
  # console windows to flash up, which is distracting and ugly. So we invoke
  # the Cygwin functions directly from the DLL.
  require "Win32API"
  path_list_buf_size = Win32API.new("cygwin1.dll", "cygwin_posix_to_win32_path_list_buf_size", [ 'p' ], 'i')
  to_win32_path = Win32API.new("cygwin1.dll", "cygwin_conv_to_full_win32_path", [ 'p', 'p' ], 'v')
  
  # Create a large enough buffer for the conversion.
  buf_size = path_list_buf_size.Call(pathname)
  buf = "\0" * buf_size
  
  # Do the conversion and tidy up the result (in case the suggested buffer
  # size was too large).
  to_win32_path.Call(pathname, buf)
  buf.delete!("\0")
  return buf
end

# Takes a list of POSIX pathnames and returns a string suitable for use
# as the OS' PATH environment variable.
def pathnames_to_path(pathnames)
  native_pathnames = pathnames.uniq().map() { |pathname| cygpath(pathname) }
  # Cygwin's ruby's File::PATH_SEPARATOR is ':', but the Win32 JVM wants ';'.
  path_separator = (target_os() == "Cygwin") ? ";" : ":"
  return native_pathnames.join(path_separator)
end

class Java
  attr_accessor(:dock_name)
  attr_accessor(:launcher)
  attr_accessor(:log_filename)

  def initialize(name, class_name)
    @dock_name = name
    @dock_icon = ""
    @png_icon = ""
    @class_name = class_name
    @launcher = "java"
    @log_filename = ""

    # Cope with symbolic links to this script.
    @project_root = Pathname.new("#{$0}/..").realpath().dirname()
    @salma_hayek = Pathname.new("#{@project_root}/../salma-hayek").realpath()
    require "#{@salma_hayek}/bin/target-os.rb"

    @extra_java_arguments = []

    require "#{@salma_hayek}/bin/find-jdk-root.rb"
    @jdk_root = find_jdk_root()
    check_java_version()

    init_default_heap_size()
    init_default_class_path()
    @library_path = [ "#{@project_root}/.generated/#{target_os()}/lib" ]
    
    set_icons(name)
  end

  def check_java_version()
    # Do we have a good enough version of Java?
    # FIXME: there's similar but different (taking the audience into account) code in "ensure-suitable-mac-os-version.rb" that should be merged with this.
    actual_java_version = `java -fullversion 2>&1`.chomp()
    actual_java_version.match(/java full version "(.*)"/)
    actual_java_version = $1
    if actual_java_version.match(/^1\.[5-9]\.0/) == nil
      informational_alert("#{@dock_name} requires a newer version of Java.", "This application requires at least Java 5, but your #{`which java`.chomp()} claims to be #{actual_java_version} instead.\n\nPlease upgrade.")
      exit(1)
    end
  end

  def informational_alert(caption, message)
    # FIXME: there's very similar code for Mac OS in "ensure-suitable-mac-os-version.rb" that should be merged with this, perhaps by putting a fake "zenity" script for Mac OS on the path and making sure the path is set up before we get here.
    command = [ "zenity" ]
    command << "--title=#{@dock_name}"
    command << "--window-icon=#{@png_icon}"
    command << "--error"
    command << "--text"
    command << "#{caption}\n\n#{message}"
    system(*command)
  end

  def add_class_path_entries(new_entries)
    @class_path.concat(new_entries)
  end
  
  def add_extra_java_arguments(new_java_arguments)
    @extra_java_arguments.concat(new_java_arguments)
  end
  
  def set_icons(name)
    dock_icon = "#{@project_root}/lib/#{name}.icns"
    if Pathname.new(dock_icon).exist?
      @dock_icon = dock_icon
    end
    png_icon = "#{@project_root}/lib/#{name.downcase()}-128.png"
    if Pathname.new(png_icon).exist?
      @png_icon = png_icon
    end
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

      # "tools.jar" doesn't exist on Mac OS X but the classes are on the boot
      # class path anyway.
      # There's a bug against Java 6 to add these classes to its boot class
      # path too.
      # On Win32 it's likely that users don't have a JDK on their path, in
      # which case they probably aren't interested in running anything that
      # wouldn't work without "tools.jar", so we cope with not having found
      # a JDK.
      if @jdk_root != nil
        tools_jar = "#{@jdk_root}/lib/tools.jar"
        if Pathname.new(tools_jar).exist?
          @class_path << tools_jar
        end
      end
    end

    # Until Java 6, we need the back-ported SwingWorker.
    @class_path << "#{@salma_hayek}/swing-worker.jar"
  end
  
  def getExtraPathComponents()
    subProjectRoots = [ @project_root, @salma_hayek ]
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
    # Put our setsid(1) ahead of any pre-installed one, for the potential benefit of edit.
    # Experience suggests that various startup files are likely to reset the PATH in terminator shells.
    newPathComponents.concat(getExtraPathComponents())
    newPathComponents.concat(originalPathComponents)
    # Find cygwin1.dll.
    newPathComponents << "/bin"
    # uniq() seems to do The Right Thing with unsorted duplicates:
    # removing the later ones, preserving order.
    # @salma_hayek may be the same as @project_root, particular with installed versions.
    ENV["PATH"] = newPathComponents.uniq().join(":")
  end
  
  def invoke(extra_app_arguments = [])
    args = [ @launcher ]

    # Set the class path directly with a system property rather than -cp so
    # that our custom Win32 launcher doesn't have to convert between the two
    # forms. (Sun's Win32 JVM expects ';'-separated paths.)
    args << "-Djava.class.path=#{pathnames_to_path(@class_path)}"
    args << "-Djava.library.path=#{pathnames_to_path(@library_path)}"

    # Pass any GNOME startup notification id through as a system property.
    # That way it isn't accidentally inherited by the JVM's children.
    # We test for the empty string because GDK doesn't unset the variable, it sets it to the empty string, presumably for portability reasons.
    desktop_startup_id = ENV['DESKTOP_STARTUP_ID']
    if desktop_startup_id != nil && desktop_startup_id.length() > 0
      ENV['DESKTOP_STARTUP_ID'] = nil
      args << "-Dgnome.DESKTOP_STARTUP_ID=#{desktop_startup_id}"
    end

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

    args << "-Dorg.jessies.frameIcon=#{cygpath(@png_icon)}"

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
