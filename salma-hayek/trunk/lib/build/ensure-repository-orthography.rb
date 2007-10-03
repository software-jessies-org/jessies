#!/usr/bin/ruby -w

# Cope with symbolic links to this script.
require "pathname.rb"
require "fileutils"
salma_hayek = Pathname.new(__FILE__).realpath().dirname().dirname().dirname()
evergreen = Pathname.new("#{salma_hayek}/../Evergreen").realpath()
if evergreen.exist?() == false
    exit(0)
end
Dir.chdir(evergreen) {
    # The "website" repository has no "/trunk" on the end.
    if `svn info`.match(/^URL: (.*\/Evergreen.*)/)
        oldUrl = $1
        newUrl = oldUrl.gsub("Evergreen", "evergreen")
        system("svn switch --relocate #{oldUrl} #{newUrl}")
    end
}
# Fix the case, even on a case-insensitive file system.
# Will fail if there's already an "evergreen" as well as an "Evergreen".
newPath = evergreen.dirname() + "evergreen"
evergreen.rename(newPath)
