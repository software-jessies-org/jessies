#!/usr/bin/ruby -w

if defined?(target_os)
    # Allow this library to be multiply included without warning.
else
    require "singleton.rb"
    
    class OsExaminer
        include Singleton
        
        def initialize()
            require "rbconfig.rb"
            # Avoid calling the console-subsystem uname(1) program on Cygwin.
            # We used to do this to prevent a console window from appearing when started from a desktop shortcut.
            # We've also seen Cygwin's uname report both "CYGWIN_NT-5.0" and "CYGWIN_NT-5.1".
            # A dependency on uname would be a dependency on Cygwin.
            ruby_os_name = RbConfig::CONFIG["target_os"]
            # We used to use uname -m (Linux) or arch but that gives the kernel architecture.
            # We're more interested in the architecture of the binaries that we can build and run.
            @arch = RbConfig::CONFIG["target_cpu"]
            if ruby_os_name == "cygwin"
                @os_name = "Cygwin"
            elsif ruby_os_name == "mswin32"
                @os_name = "Windows"
            else
                @os_name = `uname`.chomp()
                if @os_name == "Darwin"
                    @arch = "universal"
                end
            end
            # http://alioth.debian.org/docman/view.php/30192/21/debian-amd64-howto.html#id250846 says amd64 is to i386 as x86_64 is to x86.
            @arch = @arch.sub(/i[456]86/, "i386").sub(/x86_64/, "amd64")
            
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
end

if __FILE__ == $0
    puts(target_os())
    puts(target_architecture())
    puts(target_directory())
end
