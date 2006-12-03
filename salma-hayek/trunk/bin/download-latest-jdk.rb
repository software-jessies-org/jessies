#!/usr/bin/ruby -w

# FIXME: support official builds. (hard because you have to accept a license on the web page.)
# FIXME: support source releases. (obsoleted?)

require "open-uri"

class JdkInstaller
 def initialize()
  # Where to look locally for already-installed JDKs.
  @local_jdks_directory = "/usr/java/"

  # Where to look on the internet for available JDKs.
  @sun_site = "http://download.java.net"
  @sun_binaries_url = "#{@sun_site}/jdk7/binaries/"
  @sun_changes_url = "https://mustang.dev.java.net/servlets/ProjectDocumentList?folderID=2855"
  
  @jdk_version = "jdk1.7.0"

  @should_be_quiet = false
 end

 def should_be_quiet()
  @should_be_quiet = true
 end

 # Get the OS in Sun's nomenclature.
 def desired_os()
  # FIXME: this works for "Linux" -> "linux", but not for the other OSes Sun supports.
  return `uname`.chomp().downcase()
 end

 # Get the architecture in Sun's nomenclature.
 def desired_arch()
  desired_arch = `arch`.chomp()
  desired_arch.gsub!(/^i\d86$/, "i586")
  if desired_arch == "x86_64"
   desired_arch = "amd64"
  end
  return desired_arch
 end

 def install_jdk(jdk_url, jdk_filename, jdk_build_number)
  if FileTest.directory?(@local_jdks_directory) == false
   die("You need to create the #{@local_jdks_directory} directory before you can use this script.")
  end
  if FileTest.directory?("#{@local_jdks_directory}/#{@jdk_version}#{jdk_build_number}")
   if @should_be_quiet == false
    $stderr.puts("You already have the latest JDK build installed (1.7.0#{jdk_build_number})")
   end
  else
   $stderr.puts("Downloading JDK 1.7.0#{jdk_build_number}...")
   system("wget --no-verbose --output-document=/tmp/#{jdk_filename} #{jdk_url}")

   # This lets us run the installer from cron, by accepting the license for us.
   original_installer = "/tmp/#{jdk_filename}"
   modified_installer = "/tmp/#{jdk_filename}-auto.bin"
   system("sed 's/^more <<\"EOF\"$/cat <<\"EOF\"/;s/^ *read reply leftover$/reply=YES/' < #{original_installer} > #{modified_installer}")

   # Run the installer.
   system("cd #{@local_jdks_directory} && bash #{modified_installer} && rm #{modified_installer} && rm #{original_installer}")

   # Give this build a unique name, so we can install as many as we like.
   system("cd #{@local_jdks_directory} && mv #{@jdk_version} #{@jdk_version}#{jdk_build_number}")

   # Extract the supplied class library source.
   system("cd #{@local_jdks_directory}/#{@jdk_version}#{jdk_build_number} && mkdir src && cd src && ../bin/jar xf ../src.zip && rm ../src.zip")

   install_changes_html(jdk_build_number)
  end
 end

 def install_changes_html(jdk_build_number)
  url = "http://download.java.net/jdk7/changes/jdk7#{jdk_build_number}.html"
  if url != nil
   $stderr.puts("Downloading #{url}...")
   system("wget --no-verbose --output-document=#{@local_jdks_directory}/#{@jdk_version}#{jdk_build_number}/changes.html --no-check-certificate #{url}")
  end
 end

 def check_web_site()
  open(@sun_binaries_url) {
   |f|
   f.each_line() {
    |line|
    if line =~ /href="(.*(jdk-7.*(-b\d+).*\.(?:bin|exe|sh)).*)"/
     jdk_url = $1
     jdk_filename = $2
     jdk_build_number = $3

     if jdk_url =~ /^\//
      jdk_url = "#{@sun_site}#{jdk_url}"
     end

     # Ignore JDKs for other OSes/architectures, and ignore RPMs.
     next if jdk_url.include?("-rpm.bin")
     next if jdk_url.include?(desired_os()) == false
     next if jdk_url.include?(desired_arch()) == false

     install_jdk(jdk_url, jdk_filename, jdk_build_number)
    end
   }
  }
 end
end

def die(message)
 $stderr.puts(message)
 exit(1)
end

accept_sun_license = false
jdk_installer = JdkInstaller.new()
ARGV.each() {
 |arg|
 if arg == "-q"
  jdk_installer.should_be_quiet()
 elsif arg == "--accept-sun-license"
  accept_sun_license = true
 else
  die("usage: #{`basename #$0`.chomp()} [-q] --accept-sun-license")
 end
}
if accept_sun_license == false
 die("You must accept Sun's license with --accept-sun-license to run this script.")
end
jdk_installer.check_web_site()
exit(0)
