#!/usr/bin/ruby -w

require 'cgi'

def escapeTextLineToHtml(line)
    $_ = line.chomp
    $_ = CGI.escapeHTML($_)
    # Embolden filenames:
    $_.gsub!(/^([^[:space:]\/]\S*): /, "<b>\\1</b>: ")
    # Preserve formatting:
    $_.gsub!(/^(\s+)/) {|spaces| "&nbsp;" * spaces.length()}
    # Turn URLs into links:
    $_.gsub!(/(http:\/\/\S+)/, "<a href=\"\\1\">\\1</a>")
    # Make slight typographical improvements:
    $_.gsub!(/ -- /, "&nbsp;&ndash; ")
    $_.gsub!(/ --- /, "&nbsp;&mdash; ")
    return "#{$_}<br>\n"
end
