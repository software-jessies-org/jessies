#!/usr/bin/ruby -w

# Reads the output of "svn log" and produces a reasonable HTML rendition.

$: << "#{File.dirname(__FILE__)}/.."

require 'escaping.rb'
puts("<html>")
while gets()
    if $_ =~ /^r(\d+) \| (\S+) \| (\d\d\d\d-\d\d-\d\d .*) \(.*\) \| (\d+) lines?$/
        revision = $1
        author = $2
        date = $3
        line_count = $4.to_i()
        puts("<p><font size=\"+1\">#{date} / #{author} / revision #{revision}</font></p>")
        puts("<blockquote>")
	gets()
        (1..line_count).each() {
            puts(escapeTextLineToHtml(gets()))
        }
        puts("</blockquote>")
        puts("<hr noshade>")
    end
end
puts("</html>")
exit(0)
