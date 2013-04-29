#!/usr/bin/ruby -w

require "pathname.rb"
require "set.rb"

# Takes a POSIX pathname and turns it into a Win32 pathname if we're on Win32.
# Returns the original pathname on any other OS.
def convert_to_jvm_compatible_pathname(pathname)
  pathname = pathname.to_s()
  if target_os() != "Cygwin"
    return pathname
  end
  
  # We used to call now-deprecated functions directly from cygwin1.dll.
  # That was in order to prevent cygpath causing a console window to flash up.
  # That was only believed to be an issue when run from a non-console Win32 application.
  # Now our shortcuts run Ruby via ruby-launcher, a Cygwin application.
  # That program ensures that stdout and stderr are open, which I now think is the active ingredient.
  cygpathCommand = "cygpath --windows '#{pathname}'"
  $stderr.puts("Running:")
  $stderr.puts(cygpathCommand)
  cygpathOutput = nil
  IO.popen("-") {
    |io|
    if io == nil
      $stderr.reopen($stdout)
      exec("cygpath", "--windows", pathname)
    end
    cygpathOutput = io.read()
  }
  $stderr.puts("Produced:")
  $stderr.puts(cygpathOutput)
  $stderr.puts("Status was:")
  $stderr.puts($?.inspect())
  if $?.success?() != true
    raise("#{cygpathCommand} failed with #{$?.inspect()}, producing \"#{cygpathOutput}\"")
  end
  return cygpathOutput.chomp()
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
  
  def sendCommandWithoutExceptionHandling(command)
    IO.read(@serverPortPathname) =~ /^(.+):(\d+)$/
    host = @host != nil ? @host : $1
    require "socket"
    # When connected via the BlueArc VPN, this laptop always thinks its name is
    # lt-tseuchter.terastack.bluearc.com, which no DNS server will resolve.
    # Fortunately, Java and Ruby agree on the name.
    if host == Socket::gethostname()
      host = "localhost"
    end
    port = $2.to_i()
    secret = IO.read(@secretPathname)
    socket = TCPSocket.new(host, port)
    socket.puts(secret)
    socket.puts(command)
    serverOutput = socket.readlines()
    authentication_response = serverOutput.shift().chomp()
    if authentication_response != "Authentication OK"
      raise authentication_response
    end
    print(serverOutput.join(""))
    socket.close()
  end
  
  def trySendCommand(command)
    sendCommandWithoutExceptionHandling(command)
    return true
  rescue Exception
    return false
  end
  
  def sendCommand(command)
    sendCommandWithoutExceptionHandling(command)
    return true
  rescue Exception => ex
    begin
      $stderr.puts(ex)
    rescue Errno::EPIPE
      raise(ex)
    end
    return false
  end
end

class Java
  attr_accessor(:log_filename)
  attr_accessor(:initiate_startup_notification)
  attr_accessor(:class_name)
  
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
    @app_name = name
    @mac_dock_icon = ""
    @png_icon = ""
    @class_name = class_name
    @log_filename = ""
    @initiate_startup_notification = true

    # The 'realpath' calls are to cope with symbolic links to the launch
    # script and this script.
    # We look at the PROJECT_ROOT environment variable first so that we can
    # run tests with org.jessies.TestRunner in the context of the project under
    # tests (rather than the project containing the TestRunner script itself,
    # which is always salma-hayek).
    @project_root = ENV["PROJECT_ROOT"]
    if @project_root == nil
      @project_root = Pathname.new("#{$0}/..").realpath().dirname()
    end
    @salma_hayek = Pathname.new(__FILE__).realpath().dirname().dirname()
    
    # Load salma-hayek libraries.
    require "#{@salma_hayek}/bin/target-os.rb"
    require "#{@salma_hayek}/bin/show-alert.rb"
    
    @extra_java_arguments = []
    @extra_app_arguments = []

    init_default_heap_size()
    init_default_class_path()
    # We don't know the JVM's architecture at this point.
    # We've seen a number of systems which run an i386 JVM on an amd64 kernel.
    add_pathnames_property("org.jessies.libraryDirectories", Dir.glob("{#{@project_root},#{@salma_hayek}}/.generated/*_#{target_os()}/lib"))
    add_pathname_property("org.jessies.binaryDirectory", chooseSupportBinaryDirectory())
    
    set_icons(name)
    
    @launcher = "java"
    if target_os() == "Cygwin" || ENV["USE_JAVA_LAUNCHER"] != nil
      # We need to load jvm.dll from a Cygwin executable to get a reliable Cygwin JNI experience.
      # This launcher doesn't use the same algorithm as Sun's for picking a jvm.dll.
      @launcher = findSupportBinary("java-launcher")
    end
    if false && target_os() == "Darwin"
      # For Sparkle to work, we need our [NSBundle mainBundle] to point to our .app bundle.
      # For that to work, the executable that starts the JVM must be in the Contents/MacOS/ directory.
      mac_os_launcher = "#{@project_root}/../../MacOS/java-launcher"
      if File.exist?(mac_os_launcher)
        @launcher = mac_os_launcher
      else
        # We're probably running from a developer's working copy.
        # Better to run without Sparkle than not run at all.
        @launcher = findSupportBinary("java-launcher")
      end
    end
  end
  
  def get_java_version(java_executable)
    # We might like to use -fullversion, but Debian's gij-wrapper Perl script only understands -version. I think for our purposes here, "-version" is accurate enough.
    java_version = `#{java_executable} -version 2>&1`.chomp()
    # A Fedora Core 7 user reports that gij has started claiming 1.5.0, but seemingly still isn't up to the job ("Can't start up: not enough memory").
    # Now we can't ignore gij for free, let's explicitly disregard it, on the assumption that OpenJDK will replace it before it's ready.
    if java_version.match(/gij/) != nil
      java_version = "gij! run away!"
    end
    # If we appear to have found a JVM, extract just the version number.
    # Otherwise return everything the executable said, to give the user some clue as to what went wrong.
    if java_version.match(/java version "(.*)"/) != nil
      java_version = $1
    end
    return java_version
  end

  def is_java_new_enough(java_version)
    return (java_version.match(/^1\.[6-9]\.0/) != nil)
  end

  def check_java_version()
    actual_java_version = get_java_version(@launcher)
    if is_java_new_enough(actual_java_version)
      return
    end
    # The "java" on the path was no good.
    # Can we salvage the situation by finding a suitable JVM?
    
    if target_os() == "Darwin"
      globs = [ "/System/Library/Frameworks/JavaVM.framework/Versions/1.6/Home" ]
    else
      # This works for Linux distributions using Sun's RPM, and for Solaris.
      globs = [  "/usr/java/jdk1.7.0*", "/usr/java/jre1.7.0*", "/usr/java/jdk1.6.0*", "/usr/java/jre1.6.0*" ]
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
    sun_java = "/usr/lib/jvm/java-6-sun/bin/java"
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
      # FIXME: it's now just "Ubuntu Software Center" on the main menu for Ubuntu users.
      suggestion = 'To install a suitable JRE, choose "Add/Remove..." from the GNOME "Applications" menu, show "All available applications", type "sun java" in the search field, and install "Sun Java 6 Runtime".'
    end
    message_lines << suggestion
    show_alert("#{@app_name} requires Java 6 or newer.", message_lines.join("\n\n"))
    exit(1)
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
    mac_dock_icon = "#{@project_root}/lib/#{name}.icns"
    if File.exist?(mac_dock_icon)
      @mac_dock_icon = mac_dock_icon
    end
    # FIXME: Is this really machine_project_name?
    # The 128 pixel icon was traditionally just the about box icon.
    # These days we let the system scale it down for all other uses too.
    png_icon = "#{@project_root}/lib/#{name.downcase()}-128.png"
    if File.exist?(png_icon)
      @png_icon = png_icon
    end
  end
  
  def init_default_heap_size()
    # Default heap size.
    # FIXME: I suspect that our users with 1 GiB Linux boxes would have more responsive machines with the "100m" setting.
    # Portably determining how much RAM we have is a game.
    # sysctl hw.usermem on Mac OS
    # cat /proc/meminfo on Linux and Cygwin
    # sysconf(_SC_PHYS_PAGES) * sysconf(_SC_PAGESIZE) on Solaris?
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
        if File.exist?(tools_jar)
          @class_path << tools_jar
        end
      end
    end
    
    @class_path.concat(jars.to_a())
  end
  
  def subvertPath()
    # File::PATH_SEPARATOR is the right choice here for Cygwin (":") and native Windows Ruby (";").
    originalPathComponents = ENV["PATH"].split(File::PATH_SEPARATOR)
    newPathComponents = []
    # Experience suggests that various startup files are likely to reset the PATH in terminator shells.
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
    report_exceptions(@app_name) { launch() }
  end
  
  def chooseSupportBinaryDirectory()
    # An i386 JVM on an amd64 kernel will run amd64 binaries.
    # But only the i386 binaries are likely to be installed (with dpkg --force-architecture).
    pattern = "#{@salma_hayek}/.generated/{#{target_directory()},*_#{target_os()}}/bin"
    plausibleSupportBinaryDirectories = Dir.glob(pattern)
    if plausibleSupportBinaryDirectories.empty?()
      raise("Failed to find any support binary directories with a glob for #{pattern}")
    end
    return plausibleSupportBinaryDirectories[0]
  end
  
  def findSupportBinary(binaryName)
    return "#{chooseSupportBinaryDirectory()}/#{binaryName}"
  end
  
  def launch()
    # If we're using our own launcher, it'll worry about finding an appropriate JVM version and reporting errors.
    # If we're using Sun's java(1), we need to do some pre-flight checks.
    if @launcher =~ /java$/
      check_java_version()
    end

    # check_java_version may alter @launcher to get us something that works.
    args = [ @launcher ]

    add_pathnames_property("java.class.path", @class_path)
    
    # Fix the path so that cygwin1.dll is on it.
    subvertPath()
    
    # Since we're often started from the command line or from other programs, set up startup notification ourselves if it looks like we should.
    if @initiate_startup_notification && ENV["DISPLAY"] != nil && ENV["DESKTOP_STARTUP_ID"] == nil && target_os() == "Linux"
      id=`#{findSupportBinary("gnome-startup")} start #{@png_icon} Starting #{@app_name}`.chomp()
      ENV["DESKTOP_STARTUP_ID"] = id
    end
    
    # Pass any GNOME startup notification id through as a system property.
    # That way it isn't accidentally inherited by the JVM's children.
    # We test for the empty string because GDK doesn't unset the variable, it sets it to the empty string, presumably for portability reasons.
    desktop_startup_id = ENV["DESKTOP_STARTUP_ID"]
    if desktop_startup_id != nil && desktop_startup_id.length() > 0
      ENV.delete("DESKTOP_STARTUP_ID")
      add_property("gnome.DESKTOP_STARTUP_ID", desktop_startup_id)
    end

    applicationEnvironmentName = @app_name.upcase()
    logging = ENV["DEBUGGING_#{applicationEnvironmentName}"] == nil && @log_filename != ""
    if logging
      begin
        File.new(@log_filename, "w").close() # Like touch(1).
        add_pathname_property("e.util.Log.filename", @log_filename)
      rescue SystemCallError
        # Inability to create the log file is not fatal.
      end
    end
    
    add_property("e.util.Log.applicationName", @app_name)

    args << "-Xmx#{@heap_size}"

    if target_os() == "Darwin"
      args << "-Xdock:name=#{@app_name}"
      args << "-Xdock:icon=#{@mac_dock_icon}"
      add_property("apple.laf.useScreenMenuBar", "true")
    end
    
    if target_os() == "Cygwin"
      # This stops Cygwin's /etc/profile from changing the current directory, which breaks building from Evergreen and Terminator's --working-directory feature.
      ENV["CHERE_INVOKING"] = "1"
    end
    
    if @png_icon != ""
      # TODO: switch to a single org.jessies.pngIcon property.
      add_pathname_property("org.jessies.aboutBoxIcon", @png_icon)
      add_pathname_property("org.jessies.frameIcon", @png_icon)
    end
    add_pathname_property("org.jessies.projectRoot", @project_root)
    add_pathname_property("org.jessies.supportRoot", @salma_hayek)
    
    # Work around Sun bug 6274341.
    add_property("java.awt.Window.locationByPlatform", "true")
    # Work around the Metal LAF's ugliness. Not needed in Java 6?
    add_property("swing.boldMetal", "false")
    # Work around Sun bug 6961306, which has inconvenienced at least two of our Windows users.
    # Correctness trumps what I presume is a performance optimization.
    add_property("sun.java2d.d3d", "false")
    
    args.concat(@extra_java_arguments)
    args << @class_name
    args.concat(@extra_app_arguments)
    args.concat(ARGV)
    #$stderr.puts(args)
    # We used to use exec() rather than system() to work around a Cygwin problem seen most of the time when running javahpp on Cygwin 1.5.21.
    # I may have seen this less frequently on previous versions too.
    # The symptom, as reported by procexp, is that the forked ruby process (the child) doesn't die.
    # I haven't seen that symptom again (in 1.5.25) since reverting to system() but it's early days.
    # Even if it does recur, I was never one to worry much about process table clutter.
    # Having Java crashes that look like installation problems, by contrast, wastes my time.
    # We can relegate this comment to the revision history if the symptom has gone away.
    failed = system(*args) == false
    if failed
      # We're only interested in debugging unexpected exiting here.
      # When Java gets a SIGINT (signal number 2), it exits "normally" with status 130, so termsig() will be nil here.
      # Java mimics the behavior of bash in propagating SIGINT like this.
      deliberateSignals = [
      "INT",
      "TERM" # We kill Evergreen's children with SIGTERM
      ]
      deliberateSignals.each() {
        |signal|
        interruptExitStatus = 0x80 + Signal.list()[signal]
        if $?.exitstatus() == interruptExitStatus
          exit(interruptExitStatus)
        end
        # I can quite often catch Java before it's installed its signal handlers.
        if $?.termsig() == Signal.list()[signal]
          exit(interruptExitStatus)
        end
      }
      
      # It's not an error for a command-line program to exit with a non-zero
      # exit status.
      is_gui = @initiate_startup_notification
      if !is_gui && $?.exitstatus() == 1
        exit($?.exitstatus())
      end
      
      messageLines = []
      messageLines << "Java failed with " + $?.inspect()
      if logging
        messageLines << ""
        # FIXME: The path used here will be in Cygwin format, which isn't the most widely understood on Windows.
        messageLines << "Please send us the contents of the application log, from #{@log_filename}."
      end
      messageLines << ""
      messageLines << "An idea of what you were doing when Java exited might be useful."
      messageLines << ""
      # The application log filename is perhaps of particular interest.
      messageLines << "Command line was:"
      messageLines << args.join(" ")
      # A backtrace appears after the message.
      messageLines << ""
      raise messageLines.join("\n")
    end
  end
end

if __FILE__ == $0
  # Just an example.
  invoker = Java.new("Launcher", "e/util/Launcher")
  invoker.invoke()
end
