#!/usr/bin/ruby -w

# FIXME: support official 1.5.0 builds.
# FIXME: support Java 6 source releases.

require "open-uri"

class JdkInstaller
 def initialize()
  # Where to look locally for already-installed JDKs.
  @local_jdks_directory = "/usr/local/jdk/"

  # Where to look on the internet for available JDKs.
  @sun_site = "http://download.java.net"
  @sun_binaries_url = "#{@sun_site}/jdk6/binaries/"
  @sun_changes_url = "https://mustang.dev.java.net/servlets/ProjectDocumentList?folderID=2855"
 end

 # Get the OS in Sun's nomenclature.
 def desired_os()
  # FIXME: this works for "Linux" -> "linux", but not for the other OSes Sun supports.
  return `uname`.chomp().downcase()
 end

 # Get the architecture in Sun's nomenclature.
 def desired_arch()
  desired_arch = `arch`.chomp()
  if desired_arch == "x86_64"
   desired_arch = "amd64"
  end
  return desired_arch
 end

 def install_jdk(jdk_url, jdk_filename, jdk_build_number)
  if FileTest.directory?("#{@local_jdks_directory}/jdk1.6.0#{jdk_build_number}")
   $stderr.puts("You already have the latest JDK build installed (1.6.0#{jdk_build_number})")
  else
   $stderr.puts("Downloading JDK 1.6.0#{jdk_build_number}...")
   system("wget --no-verbose --output-document=/tmp/#{jdk_filename} #{jdk_url}")

   # This lets us run the installer from cron, by accepting the license for us.
   system("sed 's/^more <<\"EOF\"$/cat <<\"EOF\"/;s/^ *read reply leftover$/reply=YES/' < /tmp/#{jdk_filename} > /tmp/#{jdk_filename}-auto.bin")

   # Run the installer.
   system("cd #{@local_jdks_directory} && bash /tmp/#{jdk_filename}-auto.bin")

   # Give this build a unique name, so we can install as many as we like.
   system("cd #{@local_jdks_directory} && mv jdk1.6.0 jdk1.6.0#{jdk_build_number}")

   install_changes_html(jdk_build_number)
  end
 end

 def install_changes_html(jdk_build_number)
  $stderr.puts("Looking for list of changes on web...")
  url = nil
  `wget --no-verbose --output-document=/tmp/jdk-changes-$$.html --no-check-certificate #{@sun_changes_url} && grep -- '#{jdk_build_number}\.html"' /tmp/jdk-changes-$$.html`.split("\n").each() {
   |line|
   if line =~ /href="(.*#{jdk_build_number}\.html)"/
    url = $1
    if url =~ /^\//
     url = "https://mustang.dev.java.net#{url}"
    end
   end
  }
  if url != nil
   $stderr.puts("Downloading #{url}...")
   system("wget --no-verbose --output-document=#{@local_jdks_directory}/jdk1.6.0#{jdk_build_number}/changes.html --no-check-certificate #{url}")
  end
 end

 def check_web_site()
  open(@sun_binaries_url) {
   |f|
   f.each_line() {
    |line|
    if line =~ /href="(.*(jdk-6.*(-b\d+).*\.(?:bin|exe|sh)).*)"/
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

JdkInstaller.new().check_web_site()
exit(0)
