#!/usr/bin/env ruby -w

# Looks for the first occurrence of program within path.
# Returns nil if not found.
# (From Ruby Facets' FileUtils, and hopefully part of Ruby at some point.)
def which(program, path = ENV["PATH"])
  path.split(File::PATH_SEPARATOR).each {
    |directory|
    file = File.join(directory, program)
    # Avoid /usr/lib/ruby, for example
    if File.executable?(file) && File.directory?(file) == false
      return file
    end
  }
  # This differs from the behavior of `which `, but it makes it easier to find errors, and it's the behavior of the Ruby Facets' FileUtils extension.
  nil
end

if $0 == __FILE__
  puts(which(ARGV.shift()))
end
