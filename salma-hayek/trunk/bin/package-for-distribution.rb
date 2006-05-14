#!/usr/bin/ruby -w

require "fileutils.rb"
require "pathname.rb"

def die(message)
    $stderr.puts(message)
    exit(1)
end

def usage()
    die("usage: #{$0} <project_name> <salma-hayek-path> (with filenames one per line on stdin)")
end

def make_info_plist(app_dir, project_name, human_project_name)
    # Create a minimal "Info.plist".
    File.open("#{app_dir}/Info.plist", "w") {
        |file|
        # http://developer.apple.com/documentation/MacOSX/Conceptual/BPRuntimeConfig/index.html
        # Contrary to the documentation, CFBundleIconFile must end ".icns".
        file.puts <<EOS
<?xml version="1.0" encoding="UTF-8"?>
<!DOCTYPE plist PUBLIC "-//Apple Computer//DTD PLIST 1.0//EN" "http://www.apple.com/DTDs/PropertyList-1.0.dtd">
<plist version="1.0">
 <dict>
  <key>CFBundleIconFile</key>
  <string>#{project_name}.icns</string>
  <key>CFBundleIdentifier</key>
  <string>org.jessies.#{human_project_name}</string>
  <key>CFBundleName</key>
  <string>#{human_project_name}</string>
  <key>CFBundlePackageType</key>
  <string>APPL</string>
  <key>CFBundleSignature</key>
  <string>????</string>
 </dict>
</plist>
EOS
    }
end

def linux_link_sources(glob, unwanted_prefix)
    return Dir.glob(glob).map() {
        |current_pathname|
        # Remove tmp_dir from the front so we create currently dangling links to where the files will end up at install-time.
        current_pathname.slice(unwanted_prefix.length(), current_pathname.length() - unwanted_prefix.length())
    }
end

def extract_package_description_from_html(human_project_name)
    # If you're using apt-get(1), the description isn't very important because you won't get to see it.
    # Anyone using gdebi(1), though, will see this at the same time as the "Install" button, so it's worth a little effort.
    
    # First the generic:
    description = "software.jessies.org's #{human_project_name}"
    
    # Then the first paragraph pulled from our HTML:
    html_filename = "./www/index.html"
    if File.exist?(html_filename)
        html = IO.readlines(html_filename).join("").gsub("\n", " ")
        if html =~ /<p><strong>(.*?)<\/strong>/
            description << "\n " << $1.gsub("&nbsp;", " ").gsub("&mdash;", "--")
        end
    end
    
    return description
end

if ARGV.length() != 2
    usage()
end

# Get our command line arguments.
project_name = ARGV.shift()
salma_hayek = ARGV.shift()

require "#{salma_hayek}/bin/humanize.rb"
require "#{salma_hayek}/bin/target-os.rb"

if target_os() != "Linux" && target_os() != "Darwin"
    die("#{$0}: this script will only work on Linux or Mac OS X; you're running on '#{target_os()}'.")
end

# Then read stdin (otherwise Ruby will treat ARGV as a list of filenames to read from).
make_installer_file_list = readlines().map() { |line| line.chomp() }

if make_installer_file_list.empty?()
    usage()
end

human_project_name = humanize(project_name)

puts("Building #{target_os() == 'Darwin' ? '.app bundle' : '.deb package'} for #{human_project_name}...")

# Make a temporary directory to work in.
tmp_dir = ".generated/native/#{target_os()}/#{project_name}"
FileUtils.rm_rf(tmp_dir)
FileUtils.mkdir_p(tmp_dir)

if target_os() == "Linux"
    app_dir = "#{tmp_dir}/usr/share/software.jessies.org/#{project_name}"
    FileUtils.mkdir_p(app_dir)
else
    # Make a skeleton .app bundle.
    app_dir = "#{tmp_dir}/#{human_project_name}.app/Contents"
    FileUtils.mkdir_p("#{app_dir}/MacOS")
    system("echo -n 'APPL????' > #{app_dir}/PkgInfo")
    make_info_plist(app_dir, project_name, human_project_name)
end
resources_dir = "#{app_dir}/Resources"
FileUtils.mkdir_p(resources_dir)

# Copy this project's individual files.
project_resource_directory = "#{resources_dir}/#{project_name}"
FileUtils.mkdir_p(project_resource_directory)
make_installer_file_list.each() {
    |src|
    src_pathname = Pathname.new(src)
    dst_dirname = "#{resources_dir}/#{src_pathname.dirname()}"
    FileUtils.mkdir_p(dst_dirname)
    FileUtils.cp("../" + src_pathname, dst_dirname)
}

# Generate a single JAR file containing both the project's unique classes and all the classes from the salma-hayek library.
# Unscientific experiments suggest that uncompressed (-0) JAR files give us faster start-up times, and improving start-up time is why we're using a JAR file.
# We have to do this in two stages to avoid a "java.util.zip.ZipException: duplicate entry:" error from jar(1) for cases where both trees share a package prefix.
jar_filename = "#{project_resource_directory}/classes.jar"
system("jar", "c0f", jar_filename, "-C", "classes/", ".")
system("jar", "u0f", jar_filename, "-C", "#{salma_hayek}/classes/", ".")

if target_os() == "Darwin"
    # Apple doesn't let you give a path to a .icns file, and doesn't seem to always follow symbolic links, so we have to copy it into position.
    FileUtils.cp("#{resources_dir}/#{project_name}/lib/#{project_name}.icns", "#{app_dir}/Resources/")

    # Make a bogus start-up script.
    script_name = "#{app_dir}/MacOS/#{human_project_name}"
    File.open(script_name, "w") {
        |file|
        file.puts("#!/bin/bash")
        file.puts("cd # So user.dir is ~/ rather than the directory containing this script.")

        file.puts("# Apple hasn't yet released a 1.5.0 that makes itself the default. When they do, we can remove this.")
        file.puts("export PATH=/System/Library/Frameworks/JavaVM.framework/Versions/1.5.0/Commands/:$PATH")

        file.puts("resources=`dirname \"$0\"`/../Resources")
        file.puts("\"$resources/salma-hayek/bin/ensure-suitable-mac-os-version.rb\" && exec \"$resources/#{project_name}/bin/#{human_project_name.downcase()}\"")
    }
    system("chmod a+x #{script_name}")
else
    # Make sure we have the tools we require.
    # FIXME: it would be nice if we could reliably test whether we need to do this.
    system("sudo apt-get install build-essential fakeroot")

    # Copy any ".desktop" files into /usr/share/applications/.
    # GNOME ignores symbolic links.
    usr_share_applications = "#{tmp_dir}/usr/share/applications"
    FileUtils.mkdir_p(usr_share_applications)
    FileUtils.cp(Dir.glob("#{project_resource_directory}/lib/*.desktop"), usr_share_applications)
    
    # Copy any compiled terminfo files under /usr/share/terminfo/.
    generated_terminfo_root = ".generated/terminfo/"
    if File.exists?(generated_terminfo_root)
        usr_share_terminfo = "#{tmp_dir}/usr/share/terminfo"
        FileUtils.mkdir_p(usr_share_terminfo)
        terminfo_files = []
        FileUtils.cd(generated_terminfo_root) {
            |dir|
            terminfo_files << Dir.glob("?/*")
        }
        terminfo_files.each() {
            |terminfo_file|
            FileUtils.mkdir_p("#{usr_share_terminfo}/#{terminfo_file}")
            FileUtils.rmdir("#{usr_share_terminfo}/#{terminfo_file}")
            FileUtils.cp(File.join(generated_terminfo_root, terminfo_file), "#{usr_share_terminfo}/#{terminfo_file}")
        }
    end
    
    # Install the start-up script(s) from the project's bin/ to /usr/bin/.
    usr_bin = "#{tmp_dir}/usr/bin"
    FileUtils.mkdir_p(usr_bin)
    FileUtils.ln_s(linux_link_sources("#{project_resource_directory}/bin/*", tmp_dir), usr_bin)
    
    # gdebi(1) understands that if there's no Installed-Size the size is unknown.
    # apt-get(1) thinks it implies that the installed size is zero bytes.
    # Either way, the user's better off if we declare our installed size because nothing tries to guess from the size of the package.
    installed_size = `du -sk #{tmp_dir}`.split()[0]

    Dir.mkdir("#{tmp_dir}/DEBIAN")
    # What to put in DEBIAN/control: http://www.debian.org/doc/debian-policy/ch-controlfields.html
    # The DEBIAN/control file contains the most vital (and version-dependent) information about a binary package.
    debian_package_name = project_name.downcase()
    if debian_package_name !~ /^[a-z][a-z0-9+.-]+$/
        die("Package name \"#{debian_package_name}\" is invalid.")
    end
    File.open("#{tmp_dir}/DEBIAN/control", "w") {
        |control|
        # The fields in this file are:
        #
        # Package (mandatory)
        # Source
        # Version (mandatory)
        # Section (recommended)
        # Priority (recommended)
        # Architecture (mandatory)
        # Essential
        # Depends et al
        # Installed-Size
        # Maintainer (mandatory)
        # Description (mandatory)

        control.puts("Package: #{debian_package_name}")
        
        # Use the same artificial version number as we use for the ".msi" installer.
        require "#{salma_hayek}/bin/make-version-string.rb"
        version = makeVersionString(".", salma_hayek)
        control.puts("Version: #{version}")
        
        control.puts("Priority: optional")
        
        # Our use of the architecture field is a bit bogus.
        # For one thing, we don't necessarily have any native code. (Though in practice only amd64 and i386 Linux will have suitable JVMs available.)
        # Also, what matters is not the target platform's architecture but the target JVM's architecture.
        # If you're on i386, that's going to be an i386 JVM, but if you're on amd64 you may well be running the i386 JVM because it has a client compiler.
        # It's unfortunate that we can't build amd64 binaries on i386 or vice versa.
        deb_arch = `dpkg-architecture -qDEB_HOST_ARCH`.chomp()
        control.puts("Architecture: #{deb_arch}")
        
        control.puts("Depends: ruby (>= 1.8)")
        control.puts("Installed-Size: #{installed_size}")
        control.puts("Maintainer: software.jessies.org <software@jessies.org>")
        control.puts("Description: #{extract_package_description_from_html(human_project_name)}")
    }
end

# Fix permissions.
# The files will be installed with the permissions they had when packaged.
# 1. You're not allowed to create .deb packages with setuid or setgid files or directories.
#    Mac OS won't let you copy such files or directories out of the .dmg.
#    Guard against the case of setgid directories.
#    (Ignore setuid because it's likely to be a real mistake that needs investigating.)
system("chmod -R g-s #{tmp_dir}")
# 2. You're not supposed (it's a warning rather than an error) to create packages with contents writable by users other than root.
system("chmod -R og-w #{tmp_dir}")

# The files in a .deb will be installed with the uid and gid values they had when packaged.
# It's traditional to install as root, so everything should be owned by root when packaging.
# It seems that the right way to do this is to run dpkg-deb(1) from fakeroot(1), which we do in "universal.make".
# On Mac OS, the files get the uid and gid values of the user that installs the application.

exit(0)
