#!/usr/bin/ruby -w

require "singleton.rb"

class OsExaminer
    include Singleton
    
    def initialize()
        require "rbconfig.rb"
        # Avoid calling the console-subsystem uname(1) program on Cygwin.
        # (Calling a console subsystem program from a desktop shortcut causes a console window to appear briefly.)
        # We've also seen Cygwin's uname report both "CYGWIN_NT-5.0" and "CYGWIN_NT-5.1".
        ruby_os_name = Config::CONFIG["target_os"]
        if ruby_os_name == "cygwin"
            @os_name = "Cygwin"
            @arch = "i386"
        elsif ruby_os_name == "mswin32"
            @os_name = "Windows"
            @arch = "i386"
        else
            @os_name = `uname`.chomp()
            @arch = `arch`.chomp()
        end
        if os_name == "Darwin"
            @arch = "universal"
        else
            # http://alioth.debian.org/docman/view.php/30192/21/debian-amd64-howto.html#id250846 says amd64 is to i386 as x86_64 is to x86.
            @arch = @arch.sub(/i[456]86/, "i386").sub(/x86_64/, "amd64")
        end
        @target_directory = "#{@arch}_#{@os_name}"
    end
    
    def os_name()
        return @os_name
    end
    
    def arch()
        return @arch
    end
    
    def target_directory()
        return @target_directory
    end
end

# Commonly returns one of "Cygwin", "Darwin", "Linux", or "Windows".
def target_os()
    return OsExaminer.instance().os_name()
end

def target_architecture()
    return OsExaminer.instance().arch()
end

def target_directory()
    return OsExaminer.instance().target_directory()
end

if __FILE__ == $0
    puts(target_os())
    puts(target_architecture())
    puts(target_directory())
end
