#!/usr/bin/ruby -w

# humanize("terminator") => "Terminator"
def humanize(name)
  return name.sub(/^(.)/) { |s| s.upcase() }
end

if __FILE__ == $0
  ARGV.each() {
    |arg|
    puts(humanize(arg))
  }
end
