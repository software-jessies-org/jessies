#!/usr/bin/ruby -w
def which(program, path = ENV["PATH"])
  path.split(File::PATH_SEPARATOR).each {
    |directory|
    file = File.join(directory, program)
    # Avoid /usr/lib/ruby, for example
    if File.executable?(file) && File.directory?(file) == false
      return file
    end
  }
  nil
end

if $0 == __FILE__
  puts(which(ARGV.shift()))
end
