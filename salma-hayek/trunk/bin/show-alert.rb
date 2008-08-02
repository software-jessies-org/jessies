#!/usr/bin/ruby -w

if defined?(show_alert)
    # Allow this library to be multiply included without warning.
else
    def show_alert(title, message)
        require "pathname.rb"
        salma_hayek = Pathname.new(__FILE__).realpath().dirname().dirname()
        
        require "#{salma_hayek}/bin/target-os.rb"
        if target_os() == "Darwin"
            # FIXME: Mac OS 10.5 should give us RubyCocoa. Until then, we need a separate helper executable.
            ENV["PATH"] = "#{salma_hayek}/bin:#{salma_hayek}/.generated/#{target_directory()}/bin"
            command = [ "NSRunAlertPanel", title, message ]
            system(*command)
        elsif target_os() == "Linux"
            text = "#{title}\n\n#{message}"
            reported_okay = false
            
            # Pango will fail to parse something like "undefined local variable or method `logging' for #<Java:0xb7c827bc>":
            # (zenity:31203): Gtk-WARNING **: Failed to set text from markup due to error parsing markup: Unknown tag 'Java:0xb7c827bc' on line 6 char 1
            # HTML-escaping the #<> part seems to fix it.
            require "cgi"
            text = CGI.escapeHTML(text)
            
            # FIXME: this assumes that a KDE user doesn't have the GNOME zenity(1) installed. Which is probably true.
            if File.exist?("/usr/bin/zenity")
                command = [ "zenity" ]
                # The GNOME HIG suggests that dialog titles should be empty, but zenity(1) doesn't currently ensure this.
                command << "--title="
                command << "--info"
                command << "--text"
                command << text
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
            platformMessageLines = []
            platformMessageLines << message
            platformMessageLines << ""
            platformMessageLines << "Please copy this message to the clipboard with Ctrl-C and mail it to software@jessies.org.";
            platformMessageLines << "(Windows won't let you select the text but Ctrl-C works anyway.)";
            require "Win32API"
            Win32API.new('user32', 'MessageBox', %w(p p p i), 'i').call(0, platformMessageLines.join("\n"), title, 0)
        end
    end
    
    # Shows a dialog reporting that the exception "ex" was caught while starting application "app_name".
    def show_uncaught_exception(app_name, ex)
        # exit() throws a SystemExit exception.
        # We're only interested in unintentional exceptions here.
        if ex.class() == SystemExit
            raise()
        end
        # Based on the idea in http://blade.nagaokaut.ac.jp/cgi-bin/scat.rb/ruby/ruby-talk/21530
        prefix = "\tat "
        message = "An error occurred while starting #{app_name}:\n"
        message << "\n"
        message << "Exception #{ex.class}: #{ex.message}\n"
        message << "#{prefix}" << ex.backtrace.join("\n#{prefix}")
        show_alert("Uncaught exception", message)
    end
    
    # Use this to report exceptions thrown by the given block.
    def report_exceptions(app_name, &block)
        begin
            # Our Windows desktop shortcuts get started from Cygwin's /bin directory.
            # We want to behave as if started from the invoking user's home directory.
            if ENV["RUBY_LAUNCHER_INVOKING"]
                ENV["RUBY_LAUNCHER_INVOKING"] = nil
                Dir.chdir() {
                    block.call()
                }
            else
                block.call()
            end
        rescue Exception => e
            show_uncaught_exception(app_name, e)
            exit(1)
        end
    end
end

if __FILE__ == $0
    show_alert("This is a test of show_alert.", "The show_alert function should be used by other programs, but you appear to be a human.\n\nPlease run something else instead.")
end
