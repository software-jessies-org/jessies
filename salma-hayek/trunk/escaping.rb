#!/usr/bin/ruby -w

require 'cgi'

def escapeTextLineToHtml(line)
    $_ = CGI.escapeHTML(line.chomp())
    
    # Embolden filenames:
    $_.gsub!(/^([^[:space:]\/]\S*): /, "<b>\\1</b>: ")
    
    # Try to detect quoted text (either traditional Unix mail style or
    # wiki-like leading whitespace):
    $_.gsub!(/^(?:&gt;| ) (.*)$/) {
        |text|
        "<tt>  #{text.gsub(' ', '&nbsp;')}</tt>"
    }
    
    # Turn URLs into links:
    $_.gsub!(/(http:\/\/\S+)/, "<a href=\"\\1\">\\1</a>")
    
    # Make slight typographical improvements:
    $_.gsub!(/ -- /, "&nbsp;&ndash; ")
    $_.gsub!(/ --- /, "&nbsp;&mdash; ")
    
    # Each input line gets a line-break in the HTML:
    return "#{$_}<br>\n"
end
