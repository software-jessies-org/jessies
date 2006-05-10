#!/usr/bin/ruby -w

# ----------------------------------------------------------------------------
# Parse command line.
# ----------------------------------------------------------------------------
targets = ARGV

# ----------------------------------------------------------------------------
# Find Subversion projects.
# ----------------------------------------------------------------------------
svn_projects = []
# FIXME: I want to pass in the root directory, ~/Projects here, as the first argument, so that I can
# avoid duplicating it and so I can hide the nightly builds somewhere I won't trip over them.
Dir.glob("#{ENV['HOME']}/Projects/*/.svn").each() {
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
failed_builds = []
svn_projects.each() {
  |svn_project|
  svn_project =~ /.*\/([^\/]+)\/$/
  project_name = $1
  print("-- Updating and Building \"#{project_name}\"\n")
  # FIXME: This all-on-one line style is unmaintainable.
  # You can't always foresee at the time you write a script that it will be run from cron.
  # So it should be the cron job's responsibility to set up the environment.
  # "echo script-name | bash --login" achieves that.
  Dir.chdir(svn_project)
  system("source ~/.bashrc ; svn status ; svn diff ; svn update && make #{targets.join(" ")}")
  if $? != 0
    failed_builds << project_name
  end
}

# ----------------------------------------------------------------------------
# Output a quick summary of how things went.
# ----------------------------------------------------------------------------
puts()
if failed_builds.length() > 0
  puts("Failed builds: #{failed_builds.join(' ')}")
else
  puts("Everything built OK")
end
exit(0)
