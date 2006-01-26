#!/usr/bin/ruby -w

# If javah(1) provided a way to ask for a list of the directories that need to
# be on the native compiler's include path, we wouldn't need this script. But
# it doesn't, so we need a way to find the current JDK's include/ directory.
#
# Rather than make this script overly-specific to that particular directory,
# we output the path to the top-level directory of the JDK installation.
#
# There are many popular ways of choosing a particular Java installation, and
# they all have problems:
#
# * Using symbolic links requires a link for each program and makes it
#   unnecessarily difficult to switch if you need to test with a newer or
#   older version. Also, since the OS probably installed the symbolic
#   links in /usr/bin, they're prone to being overwritten without your
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
# * Using $PATH is slightly awkward to switch versions (unless you're happy to
#   just keep prepending), but doesn't require you to know about every utility,
#   is understood by subprocesses, and is the default convention that everyone
#   uses anyway. This is Sun's recommended way of working and until they make
#   Java 5 the default, this is Apple's recommended way of using the Java 5
#   previews.

def find_jdk_root()
  require "pathname.rb"
  
  # Cope with symbolic links to this script.
  @project_root = Pathname.new("#{$0}/..").realpath().dirname()
  @salma_hayek = Pathname.new("#{@project_root}/../salma-hayek").realpath()
  require "#{@salma_hayek}/bin/target-os.rb"
  
  # Find java(1) on the path.
  # `/bin/bash -c "type -p java"` would be an alternative.
  java_on_path=`/usr/bin/which java`.chomp()
  
  # Neophyte users are likely to be using whatever's in /usr/bin, and that's
  # likely to be a link to where there's a JDK or JRE installation. So we need
  # to follow the links to the actual installation.
  java_in_actual_location=Pathname.new(java_on_path).realpath()
  
  # Assume we're in the JDK/JRE bin/ directory.
  java_bin=java_in_actual_location.dirname()
  
  # Assume the directory above the bin/ directory is the "home" directory; the
  # directory that contains bin/ and include/ and so on.
  jdk_root=java_bin.dirname()
  
  if target_os() == "Darwin"
    # On Mac OS, Apple use their own layout but provide a Home/ subdirectory
    # that contains a JDK-like directory structure of links to the files in
    # the Apple tree.
    jdk_root="#{jdk_root}/Home"
  end
  
  return jdk_root
end

if __FILE__ == $0
  puts(find_jdk_root())
  exit(0)
end
