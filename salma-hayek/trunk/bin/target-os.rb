#!/usr/bin/ruby -w

require "singleton.rb"

class OsExaminer
    include Singleton
    
    def initialize
        # Cache the result of invoking uname(1) and mangling its output.
        @os_name = `PATH=$PATH:/bin:/usr/bin uname`.chomp().sub(/CYGWIN.*/, "Cygwin")
    end
    
    def os_name
        return @os_name
    end
end

def target_os()
    return OsExaminer.instance().os_name()
end

if __FILE__ == $0
    puts(target_os())
end
