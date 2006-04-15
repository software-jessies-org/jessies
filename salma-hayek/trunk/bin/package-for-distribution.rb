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

# Copy in the required bin/, classes/ and .generated/ directories.
# Unfortunately, the start-up scripts tend to go looking for salma-hayek, so we can't just have Resources/bin and Resources/classes; we have to keep the multi-directory structure, at least for now.
def copy_required_directories(src, dst)
    # bin/ is under revision control, so avoid the .svn directories.
    # FIXME: this assumes bin/ contains no subdirectories, which is currently true.
    FileUtils.mkdir_p("#{dst}/bin")
    replace_option = target_os() == "Linux" ? "--replace=%" : "-J %"
    system("find #{src}/bin -name .svn -prune -or -type f -print0 | xargs -0 #{replace_option} cp -r % #{dst}/bin")
    
    # classes/ contains only stuff we need, if you ignore the fact that not every application uses every class.
    # FIXME: there's the question of whether we should be using JAR files for our classes, which I still have to look into.
    FileUtils.mkdir_p("#{dst}/classes")
    system("cp -r #{src}/classes #{dst}")
    
    # .generated/`uname` contains any native stuff we need.
    FileUtils.mkdir_p("#{dst}/.generated/#{target_os()}")
    system("cp -r #{src}/.generated/#{target_os()} #{dst}/.generated")
    
    # lib/ contains support files that we have to assume we need.
    if FileTest.directory?("#{src}/lib")
        FileUtils.mkdir_p("#{dst}/lib")
        system("cp -r #{src}/lib #{dst}")
    end
    
    # Copy JAR files, if there are any.
    # FIXME: we should move these into a subdirectory of the project root. lib/? or a separate jars/?
    if Dir.glob("#{src}/*.jar").length() > 0
        # Let the shell worry about quoting.
        system("cp #{src}/*.jar #{dst}")
    end
end

def linux_link_sources(glob, unwanted_prefix)
    return Dir.glob(glob).map() {
        |current_pathname|
        # Remove tmp_dir from the front so we create currently dangling links to where the files will end up at install-time.
        current_pathname.slice(unwanted_prefix.length(), current_pathname.length() - unwanted_prefix.length())
    }
end

# FIXME: if we could get the make rules to give us all the salma-hayek MAKE_INSTALLER_FILE_LIST, we could junk this.
copy_required_directories(salma_hayek, "#{resources_dir}/salma-hayek")

# Copy this project's individual files.
project_resource_directory = "#{resources_dir}/#{project_name}"
FileUtils.mkdir_p(project_resource_directory)
make_installer_file_list.each() {
    |src|
    src_pathname = Pathname.new(src)
    dst_dirname = "#{project_resource_directory}/#{src_pathname.dirname()}"
    FileUtils.mkdir_p(dst_dirname)
    system("cp \'#{src_pathname}\' #{dst_dirname}")
}

if target_os() == "Darwin"
    # Apple doesn't let you give a path to a .icns file, so we have to move it into position.
    system("ln -s #{project_name}/lib/#{project_name}.icns #{app_dir}/Resources/")

    # Make a bogus start-up script.
    script_name = "#{app_dir}/MacOS/#{project_name}"
    File.open(script_name, "w") {
        |file|
        file.puts("#!/bin/bash")
        file.puts("cd # So user.dir is ~/ rather than the directory containing this script.")

        file.puts("# Apple hasn't yet released a 1.5.0 that makes itself the default. When they do, we can remove this.")
        file.puts("export PATH=/System/Library/Frameworks/JavaVM.framework/Versions/1.5.0/Commands/:$PATH")

        file.puts("resources=`dirname $0`/../Resources")
        file.puts("$resources/salma-hayek/bin/ensure-suitable-mac-os-version.rb && exec $resources/#{project_name}/bin/#{project_name}")
    }
    system("chmod a+x #{script_name}")
else
    # Make sure we have the tools we require.
    # FIXME: it would be nice if we could reliably test whether we need to do this.
    system("sudo apt-get install build-essential")

    Dir.mkdir("#{tmp_dir}/DEBIAN")
    # What to put in DEBIAN/control: http://www.debian.org/doc/debian-policy/ch-controlfields.html
    # The DEBIAN/control file contains the most vital (and version-dependent) information about a binary package.
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

        control.puts("Package: #{project_name}")

        # FIXME: get our version number.
        # http://www.debian.org/doc/debian-policy/ch-binary.html suggests YYYYMMDD
        control.puts("Version: 777")

        # Our use of the architecture field is a bit bogus.
        # For one thing, we don't necessarily have any native code. (Though in practice only amd64 and i386 Linux will have suitable JVMs available.)
        # Also, what matters is not the target platform's architecture but the target JVM's architecture.
        # If you're on i386, that's going to be an i386 JVM, but if you're on amd64 you may well be running the i386 JVM because it has a client compiler.
        # It's unfortunate that we can't build amd64 binaries on i386 or vice versa.
        deb_arch = `dpkg-architecture -qDEB_HOST_ARCH`.chomp()
        control.puts("Architecture: #{deb_arch}")

        control.puts("Maintainer: software.jessies.org <software@jessies.org>")
        control.puts("Description: software.jessies.org's #{project_name}")
    }

    # Make symbolic links in /usr/share/applications/ for any lib/*.desktop files.
    usr_share_applications = "#{tmp_dir}/usr/share/applications"
    FileUtils.mkdir_p(usr_share_applications)
    FileUtils.ln_s(linux_link_sources("#{project_resource_directory}/lib/*.desktop", tmp_dir), usr_share_applications)
    
    # 3. copy any compiled files corresponding to lib/*.tic files into the correct location (which is where?)
    #    helium:~$ cat /etc/terminfo/README
    #    This directory is for system-local terminfo descriptions.  By default,
    #    ncurses will search this directory first, then /lib/terminfo, then
    #    /usr/share/terminfo.

    # Install the start-up script(s) from the project's bin/ to /usr/bin/.
    usr_bin = "#{tmp_dir}/usr/bin"
    FileUtils.mkdir_p(usr_bin)
    FileUtils.ln_s(linux_link_sources("#{project_resource_directory}/bin/*", tmp_dir), usr_bin)
    
    # The files will be installed with the permissions they had when packaged.
    # You're not allowed to create packages with setuid or setgid files.
    # Guard against the case of setgid directories.
    # Ignore setuid because it's likely to be a real mistake that needs investigating.
    system("chmod -R g-s #{tmp_dir}")

    # The files will be installed with the uid and gid values they had when packaged.
    # It's traditional to install as root, so everything should be owned by root when packaging.
    # FIXME: uncommenting the next line makes life inconvenient when we come to rebuild, and gets in the way of automated rebuilds on machines where you need a password to sudo. a postinst script would avoid the disadvantages. Google suggests there's some precedent for this.
    #system("sudo chown -R root:root #{tmp_dir}")
end

exit(0)
