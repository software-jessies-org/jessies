#!/usr/bin/ruby -w

require "singleton.rb"

class OsExaminer
    include Singleton
    
    def initialize
        require "rbconfig.rb"
        # Avoid calling the console-subsystem uname(1) program on Cygwin.
        # (Calling a console subsystem program from a desktop shortcut causes a console window to appear briefly.)
        # We've also seen Cygwin's uname report both "CYGWIN_NT-5.0" and "CYGWIN_NT-5.1".
        target_os = Config::CONFIG["target_os"]
        if target_os == "cygwin"
          @os_name = "Cygwin"
        elsif target_os == "mswin32"
          @os_name = "Windows"
        else
          @os_name = `uname`.chomp()
        end
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
