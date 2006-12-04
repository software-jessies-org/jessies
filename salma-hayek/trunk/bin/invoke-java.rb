#!/usr/bin/ruby -w

require "pathname.rb"

# Takes a POSIX pathname and turns it into a Win32 pathname if we're on Win32.
# Returns the original pathname on any other OS.
def convert_to_jvm_compatible_pathname(pathname)
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
  native_pathnames = pathnames.uniq().map() { |pathname| convert_to_jvm_compatible_pathname(pathname) }
  jvm_path_separator = File::PATH_SEPARATOR
  # Cygwin's ruby's File::PATH_SEPARATOR is ':', but the Win32 JVM wants ';'.
  if target_os() == "Cygwin"
    jvm_path_separator = ";"
  end
  return native_pathnames.join(jvm_path_separator)
end

class InAppClient
  def initialize(serverPortPathname)
    @serverPortPathname = serverPortPathname
    @secretPathname = Pathname.new(serverPortPathname.to_s() + ".secret")
    if @secretPathname.exist?() == false
      @secretPathname.open("w")
    end
    @secretPathname.chmod(0600)
    @host = nil
  end
  
  def overrideHost(host)
    @host = host
  end
  
  def trySendCommand(command)
    File.open(@serverPortPathname) { |f| f.read() =~ /^(.+):(\d+)$/ }
    host = @host != nil ? @host : $1
    port = $2.to_i()
    secret = @secretPathname.open() { |file| file.read() }
    require "net/telnet"
    telnet = Net::Telnet.new('Host' => host, 'Port' => port, 'Telnetmode' => false)
    telnet.puts(secret)
    telnet.puts(command)
    serverOutput = telnet.readlines()
    success = true
    if serverOutput.length() > 0 && serverOutput[0].match(/^Authentication (OK|failed)$/)
      if $1 == "OK"
        serverOutput.shift()
      else
        success = false
      end
    end
    print(serverOutput.join(""))
    telnet.close()
    return success
  rescue
    return false
  end
end

class Java
  attr_accessor(:dock_name)
  attr_accessor(:launcher)
  attr_accessor(:log_filename)
  attr_accessor(:initiate_startup_notification)
  
  def initialize(name, class_name)
    @dock_name = name
    @dock_icon = ""
    @png_icon = ""
    @class_name = class_name
    @launcher = "java"
    @log_filename = ""
    @initiate_startup_notification = true

    # Cope with symbolic links to this script.
    @project_root = Pathname.new("#{$0}/..").realpath().dirname()
    @salma_hayek = Pathname.new(__FILE__).realpath().dirname().dirname()
    
    # Load salma-hayek libraries.
    require "#{@salma_hayek}/bin/target-os.rb"
    require "#{@salma_hayek}/bin/show-alert.rb"
    require "#{@salma_hayek}/bin/find-jdk-root.rb"
    
    @extra_java_arguments = []

    @jdk_root = find_jdk_root()

    init_default_heap_size()
    init_default_class_path()
    @library_path = [ "#{@project_root}/.generated/#{target_os()}/lib" ]
    
    set_icons(name)
  end

  def get_java_version(java_executable)
    # We might like to use -fullversion, but Debian's gij-wrapper Perl script only understands -version. I think for our purposes here, "-version" is accurate enough.
    java_version = `#{java_executable} -version 2>&1`.chomp()
    # If we appear to have found a JVM, extract just the version number.
    # Otherwise, If we return exactly what the executable said, that might give the user some clue as to what went wrong.
    if java_version.match(/java version "(.*)"/) != nil
      java_version = $1
    end
    return java_version
  end

  def is_java_new_enough(java_version)
    return (java_version.match(/^1\.[5-9]\.0/) != nil)
  end

  def check_java_version()
    actual_java_version = get_java_version(@launcher)
    if is_java_new_enough(actual_java_version) == false
      # The "java" on the path was no good.
      # Can we salvage the situation by finding a suitable JVM?

      # This works for Linux distributions using Sun's RPM, and for Solaris.
      globs = [  "/usr/java/jdk1.7.0*", "/usr/java/jre1.7.0*", "/usr/java/jdk1.6.0*", "/usr/java/jre1.6.0*", "/usr/java/jdk1.5.0*", "/usr/java/jre1.5.0*" ]
      globs.each() {
        |glob|
        java_directories = Dir.glob(glob).sort().reverse()
        java_directories.each() {
          |java_directory|
          bin_java = File.join(java_directory, "bin", "java")
          if File.exist?(bin_java)
            @launcher = bin_java
            return
          end
        }
      }

      # This works for Linux distributions using the Debian package for Sun's JVM where the user hasn't run update-java-alternatives(1).
      # (If they have configured Sun's JVM as their default, we won't have had to grovel about for a suitable JVM.)
      sun_java = "/usr/lib/jvm/java-1.5.0-sun/bin/java"
      if File.exist?(sun_java)
        @launcher = sun_java
        return
      end

      # We didn't find a suitable JVM, so we'll just have to tell the user.
      suggestion = "Please upgrade."
      if File.exist?("/usr/bin/gnome-app-install")
        suggestion = 'To install a suitable JRE, choose "Add/Remove..." from the GNOME "Applications" menu, show "All available applications", type "sun java" in the search field, and install "Sun Java 5.0 Runtime".'
      end
      show_alert("#{@dock_name} requires a newer version of Java.", "This application requires at least Java 5, but your #{`which java`.chomp()} claims to be #{actual_java_version} instead.\n\n#{suggestion}")
      exit(1)
    end
  end

  def add_class_path_entries(new_entries)
    @class_path.concat(new_entries)
  end
  
  def add_property(name, value)
    @extra_java_arguments.push("-D#{name}=#{value}")
  end
  
  def add_pathname_property(propertyName, pathname)
    add_property(propertyName, convert_to_jvm_compatible_pathname(pathname))
  end
  
  def add_pathnames_property(propertyName, pathnames)
    add_property(propertyName, pathnames_to_path(pathnames))
  end
  
  def set_icons(name)
    # FIXME: Is this really human_project_name?
    dock_icon = "#{@project_root}/lib/#{name}.icns"
    if Pathname.new(dock_icon).exist?
      @dock_icon = dock_icon
    end
    # FIXME: Is this really machine_project_name?
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
    # Users need the classes.jar, but developers need the two directories.
    # This speeds things up for the users at the expense to the developers of the time it takes the JVM to check for the non-existent classes.jar, which shouldn't be a problem.
    @class_path = [ "#{@project_root}/classes.jar", "#{@project_root}/classes", "#{@salma_hayek}/classes" ]

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
    # File::PATH_SEPARATOR is the right choice here for Cygwin (":") and native Windows Ruby (";").
    originalPathComponents = ENV["PATH"].split(File::PATH_SEPARATOR)
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
    ENV["PATH"] = newPathComponents.uniq().join(File::PATH_SEPARATOR)
  end
  
  def invoke(extra_app_arguments = [])
    # Back-quoting anything causes a flickering window on startup for Terminator on Windows.
    # The salma-hayek Java launcher already contains a version check.
    # The version check often "gets stuck" on Cygwin when running javahpp.
    # Process Explorer says there are just two Ruby processes left running: the child we're back-quoting has already quit.
    if target_os() != "Cygwin"
      check_java_version()
    end

    # check_java_version may alter @launcher.
    args = [ @launcher ]

    add_pathnames_property("java.class.path", @class_path)
    add_pathnames_property("java.library.path", @library_path)
    
    # Fix the path so that our support binaries are on it.
    subvertPath()
    
    # Since we're often started from the command line or from other programs, set up startup notification ourselves if it looks like we should.
    if @initiate_startup_notification && ENV['DISPLAY'] != nil && ENV['DESKTOP_STARTUP_ID'] == nil
      id=`gnome-startup start Starting #{@dock_name}`.chomp()
      ENV['DESKTOP_STARTUP_ID'] = id
    end
    
    # Pass any GNOME startup notification id through as a system property.
    # That way it isn't accidentally inherited by the JVM's children.
    # We test for the empty string because GDK doesn't unset the variable, it sets it to the empty string, presumably for portability reasons.
    desktop_startup_id = ENV['DESKTOP_STARTUP_ID']
    if desktop_startup_id != nil && desktop_startup_id.length() > 0
      ENV['DESKTOP_STARTUP_ID'] = nil
      add_property("gnome.DESKTOP_STARTUP_ID", desktop_startup_id)
    end

    applicationEnvironmentName = @dock_name.upcase()
    logging = ENV["DEBUGGING_#{applicationEnvironmentName}"] == nil && @log_filename != ""
    if logging
      begin
        File.new(@log_filename, "w").close() # Like touch(1).
        add_pathname_property("e.util.Log.filename", @log_filename)
      rescue SystemCallError
        # Inability to create the log file is not fatal.
      end
    end
    
    add_property("e.util.Log.applicationName", @dock_name)

    args << "-Xmx#{@heap_size}"

    if target_os() == "Darwin"
      args << "-Xdock:name=#{@dock_name}"
      args << "-Xdock:icon=#{@dock_icon}"
      add_property("apple.laf.useScreenMenuBar", "true")
    end
    
    # These only-needed-with-Java5 extra arguments are things I've found useful in the past.
    # They'll help me google if I need them again.
    #args << "-Xdebug"
    #args << "-Xrunjdwp:transport=dt_socket,server=y,suspend=n"
    #args << "-Xrunhprof:file=dump.hprof,format=b"

    add_pathname_property("org.jessies.frameIcon", @png_icon)
    
    # Work around Sun bug 6274341.
    add_property("java.awt.Window.locationByPlatform", "true")
    # Work around the Metal LAF's ugliness. Not needed in Java 6?
    add_property("swing.boldMetal", "false")
    
    args.concat(@extra_java_arguments)
    args << @class_name
    args.concat(extra_app_arguments)
    args.concat(ARGV)
    #$stderr.puts(args)
    if logging
      failed = system(*args) == false
      if failed
        puts(File.new(@log_filename).readlines())
      end
    else
      # Using exec() rather than system() works around a Cygwin problem seen most of the time when running javahpp on Cygwin 1.5.21.
      # I may have seen this less frequently on previous versions too.
      # The symptom, as reported by procexp, is that the forked ruby process (the child) doesn't die.
      exec(*args)
    end
  end
end

if __FILE__ == $0
  # Just an example.
  invoker = Java.new("Launcher", "e/util/Launcher")
  invoker.invoke()
end
