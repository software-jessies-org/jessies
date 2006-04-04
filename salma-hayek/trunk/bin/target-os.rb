#!/usr/bin/ruby -w

require "singleton.rb"

class OsExaminer
    include Singleton
    
    def initialize
        require "rbconfig.rb"
        # Avoid calling the console-subsystem uname(1) program on Cygwin.
        @os_name = Config::CONFIG["target_os"]
        # Conform to uname conventions.
        @os_name.capitalize!()
        
        # Remove version numbers. Our motivating example was "darwin8.0" on Mac OS.
        @os_name.sub!(/[\d.]+/, "")
        # Chris Starling reports that Ruby on CentOS says "Linux-gnu" rather than just "Linux" (which is what we see on other Linuxes).
        @os_name.sub!("Linux-gnu", "Linux")
        # Ruby [blastwave or built from source or both?] on Solaris says "Solaris" rather than "SunOS" (which is what uname(1) would say).
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
