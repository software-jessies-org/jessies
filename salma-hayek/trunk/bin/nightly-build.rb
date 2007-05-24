#!/usr/bin/ruby -w

# Typical usage (in the builder's crontab):
# echo nightly-build.rb ~/Projects clean native-dist | bash --login
# echo nightly-build.rb ~/Projects | bash --login
# echo nightly-build.rb | bash --login

require "pathname.rb"

# ----------------------------------------------------------------------------
# Parse command line.
# ----------------------------------------------------------------------------
projects_root = Pathname.new(__FILE__).realpath().dirname().dirname().dirname()
if ARGV.empty?() == false
  projects_root = ARGV.shift()
end
targets = ARGV
if targets.empty?()
  # Elliott suggests cleaning by default in a nightly build.
  targets << "clean"
  # There is no well-known name for the default target.  lwm doesn't support "build".
  targets << ""
end

# ----------------------------------------------------------------------------
# Find all Subversion projects under "projects_root".
# ----------------------------------------------------------------------------
svn_projects = []
Dir.glob("#{projects_root}/*/.svn").each() {
  |svn_directory|
  svn_directory =~ /^(.*\/)\.svn$/
  svn_projects << $1
}
svn_projects.uniq!()

# ----------------------------------------------------------------------------
# Of the jessies.org projects, salma-hayek must come first. Performance anxiety.
# ----------------------------------------------------------------------------
salma_hayek = svn_projects.find() { |item| item.include?("/salma-hayek/") }
svn_projects.delete(salma_hayek)
svn_projects.insert(0, salma_hayek)

# ----------------------------------------------------------------------------
# Update and build the Subversion projects.
# ----------------------------------------------------------------------------
failed_updates = []
failed_builds = []
svn_projects.each() {
  |svn_project|
  svn_project =~ /.*\/([^\/]+)\/$/
  project_name = $1
  print("-- Updating \"#{project_name}\"\n")
  Dir.chdir(svn_project)
  system("svn status ; svn diff ; svn update")
  if $? != 0
    failed_updates << project_name
  else
    if (Pathname.new(svn_project) + "Makefile").exist?()
      print("-- Building \"#{project_name}\"\n")
      commands = targets.map() {
        |target|
        "make #{target}"
      }
      system(commands.join(" && "))
      if $? != 0
        failed_builds << project_name
      end
    end
  end
}

# ----------------------------------------------------------------------------
# Output a quick summary of how things went.
# ----------------------------------------------------------------------------
puts()
if failed_updates.length() > 0
  puts("Failed updates: #{failed_updates.join(' ')}")
end
if failed_builds.length() > 0
  puts("Failed builds: #{failed_builds.join(' ')}")
end
if failed_updates.length() + failed_builds.length() == 0
  puts("Everything built OK")
end
exit(0)
