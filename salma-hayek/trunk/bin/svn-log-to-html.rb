#!/usr/bin/ruby -w

# Reads the output of "svn log" and produces a reasonable HTML rendition.

require 'cgi'
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
	    $_ = CGI.escapeHTML(gets())
            # Embolden filenames:
            $_.gsub!(/^(\S+): /, "<b>\\1</b>: ")
            # Preserve formatting:
            $_.gsub!(/^(\s+)/) {|spaces| "&nbsp;" * spaces.length()}
            # Make slight typographical improvements:
            $_.gsub!(/ -- /, "&nbsp;&ndash; ")
            $_.gsub!(/ --- /, "&nbsp;&mdash; ")
            puts("#{$_}<br>\n")
	}
        puts("</blockquote>")
        puts("<hr noshade>")
    end
end
exit(0)
