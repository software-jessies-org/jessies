#!/usr/bin/env ruby -w

require "singleton.rb"

class OsExaminer
    include Singleton
    
    def initialize
        require "rbconfig.rb"
        # Avoid calling the console-subsystem uname(1) program on Cygwin.
        @os_name = Config::CONFIG["target_os"]
        # Remove the version number from darwin8.0.
        @os_name.sub!(/[\d.]+/, "")
        # Conform to uname conventions.
        @os_name.capitalize!()
        @os_name.sub!("Solaris", "SunOS")
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
