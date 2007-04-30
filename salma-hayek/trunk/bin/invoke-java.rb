#!/usr/bin/ruby -w

require "pathname.rb"
require "set.rb"

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
  
  # Create a mutable copy, which seems to be required by the DLL calls.
  pathname = pathname.dup()
  
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
  attr_accessor(:log_filename)
  attr_accessor(:initiate_startup_notification)
  
  #
  # Command-line tools are probably best off with this trivial interface.
  # It certainly removes boilerplate from your start-up script.
  #
  def Java.runCommandLineTool(className)
    invoker = Java.new(className, className)
    invoker.initiate_startup_notification = false
    invoker.invoke()
  end
  
  def initialize(name, class_name)
    @dock_name = name
    @dock_icon = ""
    @png_icon = ""
    @class_name = class_name
    @log_filename = ""
    @initiate_startup_notification = true

    # Cope with symbolic links to this script.
    @project_root = Pathname.new("#{$0}/..").realpath().dirname()
    @salma_hayek = Pathname.new(__FILE__).realpath().dirname().dirname()
    
    # Load salma-hayek libraries.
    require "#{@salma_hayek}/bin/target-os.rb"
    require "#{@salma_hayek}/bin/show-alert.rb"
    
    @extra_java_arguments = []

    init_default_heap_size()
    init_default_class_path()
    # We don't know the JVM's architecture at this point.
    # We've seen a number of systems which run an i386 JVM on an amd64 kernel.
    add_pathnames_property("org.jessies.libraryDirectories", Dir.glob("#{@project_root}/.generated/*_#{target_os()}/lib"))
    
    set_icons(name)
    
    @launcher = "java"
    if target_os() == "Cygwin"
      # We need to load jvm.dll from a Cygwin executable to get a reliable Cygwin JNI experience.
      # The console subsystem version of this executable buys us nothing with Cygwin 1.5.21.
      # This launcher doesn't use the same algorithm as Sun's for picking a jvm.dll.
      @launcher = "#{@salma_hayek}/.generated/#{target_directory()}/bin/launcherw"
    end
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
      
      if target_os() == "Darwin"
        # At the moment, only Apple's Java 5 is suitable for running our applications.
        globs = [ "/System/Library/Frameworks/JavaVM.framework/Versions/1.5/Home" ]
      else
        # This works for Linux distributions using Sun's RPM, and for Solaris.
        globs = [  "/usr/java/jdk1.7.0*", "/usr/java/jre1.7.0*", "/usr/java/jdk1.6.0*", "/usr/java/jre1.6.0*", "/usr/java/jdk1.5.0*", "/usr/java/jre1.5.0*" ]
      end
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
      message_lines = []
      launcher_path = `which #{@launcher}`.chomp()
      # http://www.java.com/en/download/ looks like a better choice if we want to keep it simple.
      # The suggestion below offers a variety of downloads of JDKs and Java EE stuff which would
      # be bewildering to the uninitiated.
      suggestion = "http://java.sun.com/javase/downloads/ may link to a suitable JRE, if you can't use one provided by your OS vendor"
      if launcher_path != ""
        message_lines << "Your #{launcher_path} claims to be #{actual_java_version}."
        suggestion = "Please upgrade."
      end
      if File.exist?("/usr/bin/gnome-app-install")
        suggestion = 'To install a suitable JRE, choose "Add/Remove..." from the GNOME "Applications" menu, show "All available applications", type "sun java" in the search field, and install "Sun Java 5.0 Runtime".'
      end
      message_lines << suggestion
      show_alert("#{@dock_name} requires Java 5 or newer.", message_lines.join("\n\n"))
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
    @class_path = [ "#{@project_root}/.generated/classes.jar", "#{@project_root}/.generated/classes", "#{@salma_hayek}/.generated/classes" ]
    
    jars = Set.new()
    jars.merge(Dir.glob("#{@salma_hayek}/lib/jars/*.jar"))
    jars.merge(Dir.glob("#{@project_root}/lib/jars/*.jar"))
    
    if target_os() != "Darwin"
      # It's sometimes useful to have classes from "tools.jar".
      # It doesn't exist on Mac OS X but the classes are on the boot class path anyway.
      # On Win32 it's likely that users don't have a JDK on their path, in
      # which case they probably aren't interested in running anything that
      # wouldn't work without "tools.jar", so we cope with not having found
      # a JDK.
      require "#{@salma_hayek}/bin/find-jdk-root.rb"
      jdk_root = find_jdk_root()
      if jdk_root != nil
        tools_jar = "#{jdk_root}/lib/tools.jar"
        if Pathname.new(tools_jar).exist?()
          @class_path << tools_jar
        end
      end
    end
    
    @class_path.concat(jars.to_a())
  end
  
  def getExtraPathComponents()
    subProjectRoots = [ @project_root, @salma_hayek ]
    executableSubDirectories = [ "bin", ".generated/#{target_directory()}/bin" ]
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
    # Put our setsid(1) ahead of any pre-installed one, for the potential benefit of Evergreen.
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
    @extra_app_arguments = extra_app_arguments
    launch(@dock_name)
  end
  
  def launch(app_name)
    report_exceptions(app_name) { launch0() }
  end
  
  def launch0()
    # Back-quoting any console subsystem application causes a flickering window on startup for Terminator on Windows.
    # (But we could use javaw -version.)
    # The salma-hayek Java launcher already contains a version check.
    # The version check often used to "get stuck" on Cygwin when running javahpp.
    # Process Explorer says there are just two Ruby processes left running: the child we're back-quoting has already quit.
    if target_os() != "Cygwin"
      check_java_version()
    end

    # check_java_version may alter @launcher.
    args = [ @launcher ]

    add_pathnames_property("java.class.path", @class_path)
    
    # Fix the path so that our support binaries are on it.
    subvertPath()
    
    # Since we're often started from the command line or from other programs, set up startup notification ourselves if it looks like we should.
    if @initiate_startup_notification && ENV['DISPLAY'] != nil && ENV['DESKTOP_STARTUP_ID'] == nil && target_os() == "Linux"
      id=`gnome-startup start #{@png_icon} Starting #{@dock_name}`.chomp()
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
    args.concat(@extra_app_arguments)
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
