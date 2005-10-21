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
#   older version.
# * Using aliases has the same problem, and is also specific to your particular
#   shell, so isn't usefully inherited by subprocesses.
# * Using $JAVA_HOME -- as was common practice in Java 1.1 days -- requires
#   everyone to abide by the (deprecated) convention.
#
# * Using $PATH is slightly awkward to switch versions (unless you're happy to
#   just keep prepending), but doesn't require you to know about every utility,
#   is understood by subprocesses, and is the default convention that everyone
#   uses anyway. This is Sun's recommended way of working and until they make
#   Java 5 the default, this is Apple's recommended way of using the Java 5
#   previews.

if `uname` == "Darwin"
    # The current version of Java has a well-known home on Darwin. (If you
    # read the comment above, you'll realize that this means we'll be using the
    # Java 1.4.2 installation on Mac OS, and this script is actually broken
    # right now. Oops. But it's no more broken than the make rules used to be.)
    puts("/System/Library/Frameworks/JavaVM.framework/Versions/CurrentJDK/Home")
    exit(0)
end

require 'pathname.rb'

# Find java(1) on the path.
java_on_path=`which java`.chomp()

# Neophyte users are likely to be using whatever's in /usr/bin, and that's
# likely to be a link to where there's a JDK or JRE installation. So we need
# to follow the links to the actual installation.
java_in_actual_location=Pathname.new(java_on_path).realpath()

# Assume we're in the JDK/JRE bin/ directory.
java_bin=java_in_actual_location.dirname()

# Assume the directory above the bin/ directory is the "home" directory; the
# directory that contains bin/ and include/ and so on.
java_home=java_bin.dirname()

puts(java_home)
exit(0)
