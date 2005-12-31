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
copy_required_directories("../#{project_name}", "#{app_dir}/Resources/#{project_name}")
copy_required_directories(salma_hayek, "#{app_dir}/Resources/salma-hayek")

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
