#!/usr/bin/ruby -w

if defined?(show_alert)
    # Allow this library to be multiply included without warning.
else
    def show_alert(title, message, support_address = nil)
        require "pathname.rb"
        salma_hayek = Pathname.new(__FILE__).realpath().dirname().dirname()
        
        require "#{salma_hayek}/bin/target-os.rb"
        instructionLines = []
        begin
            presumedProjectRoot = Pathname.new($0).realpath().dirname().dirname()
            buildRevisionLines = IO.read("#{presumedProjectRoot}/.generated/build-revision.txt").split("\n")
            buildDate = buildRevisionLines.shift()
            projectVersion = buildRevisionLines.shift()
            salmaHayekVersion = buildRevisionLines.shift()
            packageVersion = buildRevisionLines.shift()
            instructionLines << ""
            instructionLines << "Package #{packageVersion}"
            instructionLines << "Revision #{projectVersion} (#{salmaHayekVersion})"
            instructionLines << "Built #{buildDate}"
        rescue
        end
        # Some alerts are self-explanatory.
        if support_address != nil
            instructionLines << ""
            instructionLines << "Please mail this error message to #{support_address}.";
            if target_os() == "Cygwin" || target_os() == "Windows"
                instructionLines << "You can copy it to the clipboard with Ctrl-C.";
                instructionLines << "Windows won't let you select the text but Ctrl-C works anyway.";
            end
        end
        # Some of our error messages end in a newline, some don't.
        # No-one would notice these instructions without a blank line separating them from the morass of stack or arguments.
        # Two blank lines would look amateur.
        if message[-1] != "\n" && instructionLines.empty?() == false
            instructionLines.unshift("")
        end
        instructions = instructionLines.join("\n")
        if target_os() == "Darwin"
            text = "#{message}#{instructions}"
            # FIXME: Mac OS 10.5 should give us RubyCocoa. Until then, we need a separate helper executable.
            command = [ "#{salma_hayek}/.generated/#{target_directory()}/bin/NSRunAlertPanel", title, text ]
            system(*command)
        elsif target_os() == "Linux"
            text = "#{title}\n\n#{message}#{instructions}"
            reported_okay = false
            
            # FIXME: this assumes that a KDE user doesn't have the GNOME zenity(1) installed. Which is probably true.
            if File.exist?("/usr/bin/zenity")
                # Pango will fail to parse something like "undefined local variable or method `logging' for #<Java:0xb7c827bc>":
                # (zenity:31203): Gtk-WARNING **: Failed to set text from markup due to error parsing markup: Unknown tag 'Java:0xb7c827bc' on line 6 char 1
                # HTML-escaping the #<> part seems to fix it.
                require "cgi"
                html = CGI.escapeHTML(text)
                
                command = [ "zenity" ]
                # The GNOME HIG suggests that dialog titles should be empty, but zenity(1) doesn't currently ensure this.
                command << "--title="
                command << "--info"
                command << "--text"
                command << html
                reported_okay = system(*command)
            end
            if reported_okay == false && File.exist?("/usr/bin/kdialog")
                command = [ "kdialog" ]
                command << "--msgbox"
                command << text
                reported_okay = system(*command)
            end
            if reported_okay == false
                $stderr.puts(text)
            end
        elsif target_os() == "Cygwin" || target_os() == "Windows"
            text = "#{message}#{instructions}"
            require "#{salma_hayek}/lib/User32"
            User32.MessageBox(0, text, title, 0)
        end
    end
    
    class ExceptionReporter
        # Shows a dialog reporting that the exception "ex" was caught.
        def show_uncaught_exception(ex)
            # exit() throws a SystemExit exception.
            # We're only interested in unintentional exceptions here.
            if ex.class() == SystemExit
                raise()
            end
            # I like seeing the Ruby backtrace on the console but throwing a GUI alert confuses end users.
            if ex.class() == Interrupt
                raise()
            end
            # Ron Pagani managed to get the Dock to kill Terminator with SIGTERM.
            if ex.class() == SignalException && ex.message() == "SIGTERM"
                raise()
            end
            # Based on the idea in http://blade.nagaokaut.ac.jp/cgi-bin/scat.rb/ruby/ruby-talk/21530
            prefix = "\tat "
            message = "An error occurred in #{@app_name}:\n"
            message << "\n"
            message << "Exception #{ex.class}: #{ex.message}\n"
            message << "#{prefix}" << ex.backtrace.join("\n#{prefix}")
            show_alert("Uncaught exception", message, @support_address)
        end
        
        def run_in_home_directory(block)
            # This happens more often on Cygwin than users of other operating systems would think.
            # Perhaps often enough to warrant all this code to detect and report on the situation in a friendly way.
            chdirSucceeded = false
            begin
                Dir.chdir() {
                    chdirSucceeded = true
                    block.call()
                }
            rescue Exception => ex
                if chdirSucceeded
                    raise()
                end
                messageLines = []
                messageLines << "#{@app_name} failed to change to your home directory."
                messageLines << ""
                messageLines << "Exception #{ex.class()}: #{ex.message()}"
                messageLines << ""
                messageLines << "Perhaps you need to double click on the Cygwin shortcut again?"
                messageLines << "See https://code.google.com/p/jessies/wiki/CygwinSetup"
                show_alert("Home directory problem", messageLines.join("\n"), @support_address)
                # If the user has no home directory, then they might be happy starting in /bin,
                # but Terminator will fail when it can't create ~/.terminator,
                # so perhaps we're best off predictably exiting here.
                exit(1)
            end
        end
        
        # Use this to report exceptions thrown by the given block.
        def initialize(app_name, support_address, &block)
            @app_name = app_name
            @support_address = support_address
            begin
                # Our Windows desktop shortcuts get started from Cygwin's /bin directory.
                # We want to behave as if started from the invoking user's home directory.
                if ENV["RUBY_LAUNCHER_INVOKING"]
                    ENV.delete("RUBY_LAUNCHER_INVOKING")
                    run_in_home_directory(block)
                else
                    block.call()
                end
            rescue Exception => ex
                show_uncaught_exception(ex)
                exit(1)
            end
        end
    end
    
    # There are two callers that don't specify the address.
    # One would be hard to eliminate.
    def report_exceptions(app_name, support_address = "jessies-software@googlegroups.com", &block)
        ExceptionReporter.new(app_name, support_address, &block)
    end
end

if __FILE__ == $0
    show_alert("This is a test of show_alert.", "The show_alert function should be used by other programs, but you appear to be a human.\n\nPlease run something else instead.")
end
