#!/usr/bin/ruby -w
require 'cgi'
while gets
    puts(CGI.escapeHTML($_))
end
exit(0)
