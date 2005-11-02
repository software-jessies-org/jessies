#!/usr/bin/ruby -w

def target_os()
    return `uname`.chomp().sub(/CYGWIN.*/, "Cygwin")
end

if __FILE__ == $0
    puts(target_os())
end
