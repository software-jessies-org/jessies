#!/usr/bin/ruby -w

# Usage: ssi.rb www/index.html > /tmp/test.html
# View /tmp/test.html in your web browser.

# You'll need to add a BASE HREF tag to your document's HEAD if you want to point your browser at the local file system and still have CSS and images work.
# If you're using the software.jessies.org "header.html", this is already done for you.
# Note that links on the local page will actually link to the currently live website.

class SsiProcessor
    def initialize()
        @settings = Hash.new()
        @variables = Hash.new()
    end
    
    def process(filename)
        File.open(filename) {
            |file|
            text = file.read()
            text = text.gsub(/<!--#(.+?)\s*-->/) {
                |comment|
                directive = $1
                evaluate_directive(directive)
            }
            return text
        }
    end
    
    def evaluate_directive(directive)
        #$stderr.puts(directive)
        if directive =~ /echo \s+ var \s* = \s* "(.+)"/x
            var = $1
            value = @variables[var]
            if value == nil
                value = magic_variable(var)
            end
            if value == nil
                value = "(none)" # According to http://httpd.apache.org/docs/1.3/mod/mod_include.html
            end
            return value
        elsif directive =~ /set \s+ var \s* = \s* "(.+)" \s+ value \s* = "(.*)"/x
            var = $1
            value = $2
            @variables[var] = value
            return ""
        elsif directive =~ /config \s+ (\S+) \s* = \s* "(.*)"/x
            setting = $1
            value = $2
            if setting == "errmsg" || setting == "sizefmt" || setting == "timefmt"
                @settings[setting] = value
            else
                $stderr.puts("unknown config attribute \"#{setting}\"")
                exit(1)
            end
            return ""
        elsif directive =~ /include \s+ virtual \s* = \s* "(.*)"/x
            include_file = $1
            return process(virt_to_phys(include_file))
        else
            $stderr.puts("unknown directive: #{directive}")
            exit(1)
        end
    end
    
    def virt_to_phys(virtual)
        return virtual.gsub(/^\/([^\/]+)\/(.+)$/) { "../#$1/www/#$2" }
    end
    
    def magic_variable(name)
        if name == "LAST_MODIFIED"
            return "[[FIXME:LAST_MODIFIED]]"
        end
        return nil
    end
end

ssiProcessor = SsiProcessor.new()
ARGV.each() {
    |filename|
    content = ssiProcessor.process(filename)
    $stdout.print(content)
    
    # Let tidy(1) have a look at what we've produced.
    # The "drop-empty-paras" advice is bogus (according to the w3c validator), and "fixing" it breaks our layout.
    # The adjacent hyphens advice is similarly bogus, and doesn't appear to be something we can turn off.
    IO.popen("tidy -e -q -utf8 --drop-empty-paras false 2>&1 | grep -v 'adjacent hyphens' > /dev/stderr", "w") { |p| p.puts(content) }
}
exit(0)
