require "pathname.rb"
salma_hayek = Pathname.new(__FILE__).realpath().dirname().dirname()
require "#{salma_hayek}/bin/target-os.rb"

if defined?(show_alert)
    # Allow this library to be multiply included without warning.
elsif target_os() == "Darwin"
    
    def show_alert(title, message)
        # AppleScript's "display alert" isn't available before 10.4.
        # Before then we need "display dialog".
        # See http://daringfireball.net/2005/10/css_checker_101 for an explanation of this work-around.
        # Running osascript(1) twice is slow, but slightly less unclear and who cares about performance in a failure case anyway?
        has_display_alert = `osascript -e 'property AS_VERSION_1_10 : 17826208' -e '((system attribute \"ascv\") >= AS_VERSION_1_10)'`.chomp() == "true"
        if has_display_alert
            display_command = "display alert \"#{title}\" message \"#{message}\" as informational"
        else
            display_command = "display dialog \"#{title}\" & return & return & \"#{message}\" buttons { \"OK\" } default button 1 with icon note"
        end
        system("osascript -e 'tell application \"Finder\"' -e 'activate' -e '#{display_command}' -e 'end tell' > /dev/null")
    end
    
elsif target_os() == "Linux"
    
    def show_alert(caption, message)
        command = [ "zenity" ]
        # The GNOME HIG suggests that dialog titles should be empty, but zenity(1) doesn't currently ensure this.
        command << "--title="
        command << "--info"
        command << "--text"
        command << "#{caption}\n\n#{message}"
        system(*command)
    end
    
elsif target_os() == "Cygwin" || target_os() == "Windows"
    
    def show_alert(caption, message)
        require "Win32API"
        Win32API.new('user32', 'MessageBox', %w(p p p i), 'i').call(0, message, caption, 0)
    end
    
end
