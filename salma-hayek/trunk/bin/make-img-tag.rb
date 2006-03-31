#!/usr/bin/env ruby -w
if ARGV.length() == 0
    print("Usage: #$0 <filename...>\n")
    exit(1)
end
ARGV.each() {
    |file|
    `file #{file}` =~ /image data, (\d+) x (\d+), /
    print("<img src=\"#{file}\" width=\"#$1\" height=\"#$2\">\n")
}
