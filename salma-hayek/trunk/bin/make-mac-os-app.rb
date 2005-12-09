#!/usr/bin/ruby -w

def die(message)
    $stderr.puts(message)
    exit(1)
end

if ARGV.length() != 2
    # example: make-mac-os-app.rb Terminator /Users/elliotth/Projects/salma-hayek
    die("usage: make-mac-os-app.rb <project_name> <salma-hayek-path>")
end

if `uname`.chomp() != "Darwin"
    die("#{$0}: this script will only work on Mac OS X")
end

project_name = ARGV.shift()
salma_hayek = ARGV.shift()
puts("Building .app bundle for #{project_name}...")

require "fileutils.rb"

# Make a temporary directory to work in.
tmp_dir = "/tmp/make-mac-os-app.#$$"
FileUtils.mkdir_p(tmp_dir)

# Make a skeleton .app bundle.
app_dir = "#{tmp_dir}/#{project_name}.app/Contents"
FileUtils.mkdir_p("#{app_dir}/MacOS")
FileUtils.mkdir_p("#{app_dir}/Resources")

# Copy in the required bin/, classes/ and .generated/ directories.
# Unfortunately, the start-up scripts tend to go looking for salma-hayek, so we can't just have Resources/bin and Resources/classes; we have to keep the multi-directory structure. For now.
def copy_required_directories(src, dst)
    FileUtils.mkdir_p("#{dst}/bin")
    FileUtils.mkdir_p("#{dst}/classes")
    FileUtils.mkdir_p("#{dst}/.generated")
    
    # bin/ is under revision control, so avoid the .svn directories.
    system("find #{src}/bin -name .svn -prune -or -type f -print0 | xargs -0 -J % cp -r % #{dst}/bin")
    
    system("cp -r #{src}/classes #{dst}")
    system("cp -r #{src}/.generated #{dst}")
    
    # Copy JAR files, if there are any.
    # FIXME: we should move these into a subdirectory of the project root. lib/? or a separate jars/?
    if Dir.glob("#{src}/*.jar").length() > 0
        # Let the shell worry about quoting.
        system("cp #{src}/*.jar #{dst}")
    end
end
copy_required_directories("../#{project_name}", "#{app_dir}/Resources/#{project_name}")
copy_required_directories(salma_hayek, "#{app_dir}/Resources/salma-hayek")

# Make a bogus start-up script.
script_name = "#{app_dir}/MacOS/#{project_name}"
File.open(script_name, "w") {
    |file|
    file.puts("#!/bin/bash")
    file.puts("cd # So user.dir is ~/ rather than the directory containing this script.")
    file.puts("exec `dirname $0`/../Resources/#{project_name}/bin/#{project_name}")
}
system("chmod a+x #{script_name}")

# Make a Mac OS .dmg file.
system("rm -f #{project_name}.dmg")
system("hdiutil create -fs HFS+ -volname #{project_name} -srcfolder #{tmp_dir} #{project_name}.dmg")

exit(0)
