require 'cgi'

def escapeTextLineToHtml(line)
    $_ = CGI.escapeHTML(line.chomp())
    
    # Embolden filenames:
    $_.gsub!(/^([^[:space:]\/]\S*): /, "<b>\\1</b>: ")
    
    # Try to detect where martind has quoted a whole paragraph
    # with a single "> " at the start.
    # For example, in revision 445 of terminator.
    $_.gsub!(/^&gt; (.{80,})$/, "<blockquote>\\1</blockquote>")

    # Try to detect quoted text (either traditional Unix mail style or
    # wiki-like leading whitespace):
    $_.gsub!(/^(?:&gt;| ) (.*)$/) {
        |text|
        "<tt>  #{text.gsub(' ', '&nbsp;')} </tt>"
    }
    
    # Turn URLs into links.  This regex is very loose, but other implementation I've seen have been too tight, or incorrect.
    # We could do to revisit this, perhaps using the BNF here as a source:
    # http://www.w3.org/Addressing/URL/url-spec.txt
    $_.gsub!(/(http:\/\/\S+)/, "<a href=\"\\1\">\\1</a>")
    
    # Make slight typographical improvements:
    $_.gsub!(/ -- /, "&nbsp;&ndash; ")
    $_.gsub!(/ --- /, "&nbsp;&mdash; ")
    
    # Each input line gets a line-break in the HTML:
    return "#{$_}<br/>\n"
end
