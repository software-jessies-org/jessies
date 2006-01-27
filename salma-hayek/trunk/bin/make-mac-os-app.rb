#!/usr/bin/ruby -w

require "fileutils.rb"
require "pathname.rb"

def die(message)
    $stderr.puts(message)
    exit(1)
end

def usage()
    die("usage: make-mac-os-app.rb <project_name> <salma-hayek-path> <version_string> (with filenames one per line on stdin)")
end

if ARGV.length() != 3
    usage()
end

if `uname`.chomp() != "Darwin"
    die("#{$0}: this script will only work on Mac OS X")
end

# Get our command line arguments.
project_name = ARGV.shift()
salma_hayek = ARGV.shift()
version_string = ARGV.shift()
# Then read stdin (otherwise Ruby will treat ARGV as a list of filenames to read from).
make_installer_file_list = readlines().map() { |line| line.chomp() }

if make_installer_file_list.empty?()
    usage()
end

puts("Building .app bundle for #{project_name}...")

# Make a temporary directory to work in.
tmp_dir = ".generated/native/Darwin/#{project_name}"
FileUtils.rm_rf(tmp_dir)
FileUtils.mkdir_p(tmp_dir)

# Make a skeleton .app bundle.
app_dir = "#{tmp_dir}/#{project_name}.app/Contents"
FileUtils.mkdir_p("#{app_dir}/MacOS")
FileUtils.mkdir_p("#{app_dir}/Resources")
system("echo -n 'APPL????' > #{app_dir}/PkgInfo")

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
  <string>org.jessies.#{project_name}</string>
  <key>CFBundleName</key>
  <string>#{project_name}</string>
  <key>CFBundlePackageType</key>
  <string>APPL</string>
  <key>CFBundleSignature</key>
  <string>????</string>
  <key>CFBundleGetInfoString</key>
  <string>#{project_name} #{version_string}</string>
 </dict>
</plist>
EOS
}

# Copy in the required bin/, classes/ and .generated/ directories.
# Unfortunately, the start-up scripts tend to go looking for salma-hayek, so we can't just have Resources/bin and Resources/classes; we have to keep the multi-directory structure. For now.
def copy_required_directories(src, dst)
    # bin/ is under revision control, so avoid the .svn directories.
    # FIXME: this assumes bin/ contains no subdirectories, which is currently true.
    FileUtils.mkdir_p("#{dst}/bin")
    system("find #{src}/bin -name .svn -prune -or -type f -print0 | xargs -0 -J % cp -r % #{dst}/bin")
    
    # classes/ contains only stuff we need, if you ignore the fact that not every application uses every class. There's also the question of whether we should be using JAR files for our classes, which I still have to look into.
    FileUtils.mkdir_p("#{dst}/classes")
    system("cp -r #{src}/classes #{dst}")
    
    # .generated/ contains symbolic links to all the native source (which "-type f" ignores), contains object files, generated JNI header files, and generated make files. None of which we need to distribute, and which add up for some projects.
    FileUtils.mkdir_p("#{dst}/.generated")
    # FIXME: this flattens out the .generated/ subtree, which means we can no longer find the files. martind is going to improve the make rules to separate the generated products from the byproducts.
    #system("find .generated/ -not -name '*.o' -not -name '*.h' -not -name '*.make' -type f -print0 | xargs -0 -J % cp -r % #{dst}/.generated")
    system("cp -r #{src}/.generated #{dst}")
    
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

# FIXME: if we could get the make rules to give us all the salma-hayek MAKE_INSTALLER_FILE_LIST, we could junk this.
copy_required_directories(salma_hayek, "#{app_dir}/Resources/salma-hayek")

# Copy this project's individual files.
project_resource_directory = "#{app_dir}/Resources/#{project_name}"
FileUtils.mkdir_p(project_resource_directory)
make_installer_file_list.each() {
    |src|
    src_pathname = Pathname.new(src)
    dst_dirname = "#{project_resource_directory}/#{src_pathname.dirname()}"
    FileUtils.mkdir_p(dst_dirname)
    system("cp \'#{src_pathname}\' #{dst_dirname}")
}

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

# Make a Mac OS .dmg file.
system("rm -f #{project_name}.dmg")
system("hdiutil create -fs HFS+ -volname #{project_name} -srcfolder #{tmp_dir} #{project_name}.dmg")

exit(0)
