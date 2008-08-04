#!/usr/bin/ruby -w

# Possibly useful extensions:

# --lang c++|java
#  don't match tags with a "language:" extension field whose value doesn't match; remember that most tags files won't have this information.

if ARGV.length() != 1
    $stderr.puts("usage: find-tags.rb <tag>")
    exit(1)
end

tag_name = ARGV.shift() + "\t"
# FIXME: do the binary search of the tags file ourselves.
# FIXME: read the tags header to see if they're sorted?
# FIXME: as long as we're calling out, should we |head? we don't want more than a handful of results anyway.
matching_tag_lines = `look -b '#{tag_name}' tags`
matching_tag_lines.each() {
    |matching_tag_line|
    if matching_tag_line =~ /^([^\t]+)\t([^\t]+)\t(\d+);"\t/
        file_name = $2
        line_number = $3
        # Output matches in a form similar to http://code.google.com/p/google-gtags/ so this can easily be swapped out for that.
        puts("* #{file_name}:#{line_number}")
    end
}
