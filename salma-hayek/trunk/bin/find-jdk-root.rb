#!/usr/bin/ruby -w

# What this script does
# ---------------------
#
# This script is used by the build process to find a JDK.
# If javah(1) provided a way to ask for a list of the directories that need to
# be on the native compiler's include path, we might not have written this
# script. But it doesn't, so we needed a way to find the current JDK's
# include/ directory.
#
# We use a completely different method (in "invoke-java.rb") to find the
# java(1) launcher, and yet another method (in "java-launcher.cpp") to find a
# JVM DLL on Windows.
#
# The idea is that the build process should be fairly controlled and you
# should know what you're building with, but if you're just trying to run
# one of our applications, we should do our darnedest to run the first best
# JVM we find. This script takes care of the building side of things. See
# "invoke-java.rb" for the running side of things.
#
# "invoke-java.rb" does make passing use of this script to find "tools.jar".
# That's why, rather than make this script specific to the include/ directory,
# we output the path to the top-level directory of the JDK installation.
#
# How we choose a Java installation
# ---------------------------------
#
# There are many popular ways of choosing a particular Java installation, and
# they all have problems. The bad:
#
# * Using symbolic links requires a link for each program and makes it
#   unnecessarily difficult to switch if you need to test with a newer or
#   older version. Also, since the OS probably installed the symbolic
#   links in /usr/bin/, they're prone to being overwritten without your
#   direct consent. Other applications may (reasonably) assume that
#   the links haven't been meddled with. It makes it more likely that you're
#   testing with an unusual (and unsupported) configuration; if you rely on
#   the modified symbolic link to function, you won't necessarily recognize
#   this until someone tries to run your program on their standard system.
# * Using aliases requires an alias for each program, and is also specific to
#   your particular shell, so isn't usefully inherited by subprocesses which
#   can make testing more confusing.
# * Using $JAVA_HOME -- as was common practice in Java 1.1 days -- requires
#   everyone to abide by the (deprecated) convention. Because you're probably
#   still running the tools from $PATH regardless of any $JAVA_HOME setting,
#   it's easy to miss when this is set wrong. Using out-of-date header files,
#   for example, isn't always going to be easy to spot.
#
# The best:
#
# * Using $PATH is slightly awkward to switch versions (unless you're happy to
#   just keep prepending), but doesn't require you to know about every utility,
#   is understood by subprocesses, and is the default convention that everyone
#   uses anyway. This is Sun's (and Apple's) recommended way of working with
#   multiple versions.
#
# This is complicated somewhat when we're actually just looking for a JRE to
# run our stuff. While it's perfectly reasonable to expect developers to
# fiddle with $PATH to explicitly choose a JDK, it's less reasonable to expect
# users to know or care about any of this.
#
# There are several other common methods for end-users:
#
# * If you use JNLP, you can specify exactly your requirements, including
#   such things as "1.5 or later". This isn't an option for us, and I don't
#   think anyone's ever implemented it to choose between different vendors'
#   JVMs anyway; just between different versions of a single vendor's JVM.
# * On Debian-based systems, update-java-alternatives(1) fiddles the symbolic
#   links in /usr/bin/ to point to the various components of the selected Java
#   implementation. This has the disadvantage of applying to all applications
#   on the system (and most of the disadvantages of symbolic links).
# * On Debian-based systems, there's a seemingly independent scheme using a
#   configuration file /etc/jvm and shell scripts in /usr/share/java-common/.
#   This is flawed because although it lets you override the default for
#   your application, it doesn't let you do so in terms of version
#   requirements; you have to say exactly which JVM you want, making it no
#   better than just looking for your desired JVM in the right place. (The
#   configuration file doesn't even necessarily tell you which JVMs are
#   installed: it just lists some JVMs any of which may or may not be
#   available.)
#

if defined?(find_jdk_root)
  # Allow this library to be multiply included without warning.
else
  # Looks for the first occurrence of program within path.
  # Returns nil if not found.
  # (From Ruby Facets' FileUtils, and hopefully part of Ruby at some point.)
  def which(program, path = ENV["PATH"])
    path.split(File::PATH_SEPARATOR).each() {
      |directory|
      file = File.join(directory, program)
      # Avoid /usr/lib/ruby, for example
      if File.executable?(file) && File.directory?(file) == false
        return file
      end
    }
    # This differs from the behavior of `which `, but it makes it easier to find errors, and it's the behavior of the Ruby Facets' FileUtils extension.
    nil
  end
  
  def convertWindowsFilenameToUnix(windowsFilename)
    return `cygpath --unix '#{windowsFilename}'`.chomp()
  end
  
  def find_jdk_root()
    require "pathname.rb"
    
    # Load helper libraries, coping with symbolic links to this script.
    bin = Pathname.new(__FILE__).realpath().dirname()
    require "#{bin}/target-os.rb"
    
    # Kees Jongenburger says this is the way things are done on Gentoo.
    # It sounds like a per-user equivalent of /etc/alternatives.
    is_gentoo = (which("java-config") != nil)
    if is_gentoo
      return `java-config --jdk-home`
    end
    
    # On Windows, it's likely that the only Java-related executables on the user's path are %WINDIR%\SYSTEM32\{java,javaw,javaws}.exe.
    # This is unfortunately true even if the user has a properly installed JDK.
    if target_os() == "Cygwin"
      choices = []
      Dir.glob("/proc/registry/HKEY_LOCAL_MACHINE/SOFTWARE/JavaSoft/Java Development Kit/1.*/JavaHome").each() {
        |registryKeyFile|
        if registryKeyFile.match(/\.(\d+)/) == nil
          next
        end
        version = $1.to_i()
        # See -target in universal.make.
        if version < 6
          next
        end
        choices << [version, registryKeyFile]
      }
      choices.sort_by() {
        |choice|
        choice[0]
      }.reverse().each() {
        |version, registryKeyFile|
        contents = IO.read(registryKeyFile);
        # Cygwin's representation of REG_SZ keys contains the null terminator.
        return convertWindowsFilenameToUnix(contents.chomp("\0"))
      }
    end
    
    # Find javac(1) on the path.
    # We used to look for java(1) instead, but that fails in the case where the user has both JRE and JDK installed, with the former coming first on their path.
    javac_on_path = which("javac")
    
    # Our Windows "java-launcher.exe" uses the registry to find a JRE.
    # Returning nil or an unsuitable Java here prevents you from building, but doesn't affect your ability to *run* our applications.
    if javac_on_path == nil
      return nil
    end
    
    # Neophyte users are likely to be using whatever's in /usr/bin, and that's likely to be a link to where there's a JDK or JRE installation.
    # Systems using /etc/alternatives are especially likely to be set up this way.
    # We need to follow the links to the actual installation, because it's the support files we're really looking for.
    javac_in_actual_location = Pathname.new(javac_on_path).realpath()
    
    # Assume we're in the JDK/JRE bin/ directory.
    jdk_bin = javac_in_actual_location.dirname()
    
    # Assume the directory above the bin/ directory is the "home" directory; the directory that contains bin/ and include/ and so on.
    jdk_root = jdk_bin.dirname()
    
    # We must not return Pathname in some circumstances and String in others.
    return jdk_root.to_s()
  end
end

if __FILE__ == $0
  puts(find_jdk_root())
  exit(0)
end
