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
            text = "#{caption}\n\n#{message}"
            reported_okay = false
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
                require "Win32API"
                Win32API.new('user32', 'MessageBox', %w(p p p i), 'i').call(0, message, caption, 0)
        end
    end
end

if __FILE__ == $0
    show_alert("This is a test of show_alert.", "The show_alert function should be used by other programs, but you appear to be a human.\n\nPlease run something else instead.")
end
