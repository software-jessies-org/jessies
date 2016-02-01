#!/usr/bin/ruby -w

# NAME
#   annotate-patch.rb - add scope names to patch hunks
#
# SYNOPSIS
#   annotate-patch.rb <patch-filename>
#   svn diff | annotate-patch.rb -
#   svn diff | annotate-patch.rb
#
# DESCRIPTION
#   Appends scope names to the end of lines in a patch that introduce hunks,
#   in the style of "diff -p", only applying to any language that Exuberant
#   ctags can parse, rather than just C.
#
# EXAMPLE
#   Given this patch file, and starting in a directory containing src/tuple.h:
# 
example_jikes_patch = <<EOF
Index: src/tuple.h
===================================================================
RCS file: /usr/cvs/jikes/jikes/src/tuple.h,v
retrieving revision 1.19
diff -u -r1.19 tuple.h
--- src/tuple.h 11 Dec 2002 00:55:05 -0000      1.19
+++ src/tuple.h 26 Nov 2004 21:10:19 -0000
@@ -90,8 +90,9 @@
         // instead of having to perform a subtraction for each reference.
         // See operator[] below. Finally, we update size.
         //
-        base[k] = (new T[Blksize()]) - size;
-        size += Blksize();
+        unsigned block_size = Blksize();
+        base[k] = (new T[block_size]) - size;
+        size += block_size;
     }
 
 public:
EOF
# 
# The output is rewritten so the hunk introduction looks like this (the two
# guesses correspond to the best tag in the --- file and the +++ file; I
# don't yet know whether this is going to be useful):
# 
example_result = <<EOF
--- src/tuple.h 11 Dec 2002 00:55:05 -0000      1.19
+++ src/tuple.h 26 Nov 2004 21:10:19 -0000
@@ -90,8 +90,9 @@ Jikes::Tuple.AllocateMoreSpace Jikes::Tuple.AllocateMoreSpace
         // instead of having to perform a subtraction for each reference.
EOF
#
# BUGS
#   Exuberant ctags doesn't tell us the range of a tag, so a new top-level
#   function in C, for example, will be described as being part of the
#   previous top-level symbol.
#
#   It's not obvious that we care about the scope at the start of the context;
#   it seems more likely that we're interested in the scope of the first line
#   starting with a + or - in each hunk.
#

# -----------------------------------------------------------------------------

def tags_for_file(filename)
  tag_lines = `ctags -n -f - #{filename}`
  namespace_separator=(filename =~ /\.java$/) ? "." : "::"
  tags = Hash.new()
  tag_lines.split("\n").each() {
    |line|
    if line =~ /^(\S*)\t\S*\t(\d+);"\t\S+(?:\t(\S*))?/
      line_number = $2.to_i()
      identifier = $1
      if $3 =~ /(?:struct|class|enum|interface|namespace):(\S+)/
        identifier = "#$1#{namespace_separator}#{identifier}"
      end
      tags[line_number] = identifier
    end
  }
  return tags.sort()
end

def find_tag_for_line(tags, line)
 sought_line_number = line.to_i()
 name = "(unknown)"
 tags.each() {
  |tag|
  line_number = tag[0]
  if line_number.to_i() <= sought_line_number
   name = tag[1]
  end
 }
 return name
end

def annotate_patch()
 file = $stdin
 if ARGV.length() > 1
  print("usage: #{$0} patch-file\n(or use as a filter)\n")
  exit(1)
 elsif ARGV.length() == 1 && ARGV[0] != "-"
  file = File.new(ARGV[0])
 end
 
 minus_tags = nil
 plus_tags = nil 
 
 file.each_line() {
  |line|
  if line =~ /^=+$/ || line =~ /^=== / || line =~ /^Index: /
   next
  elsif line =~ /^\=\=\=\=\= (\S+) / || line =~ /^\=\=\=\= \S+ - (\S+) \=\=\=\=/
   if test(?r, $1)
    plus_tags = tags_for_file($1)
    minus_tags = tags_for_file($1)
   end
   next # SCM displays nicer-looking patches if we elide these lines.
  elsif line =~ /^\+\+\+ (\S+)\s/
   if test(?r, $1)
    plus_tags = tags_for_file($1)
   end
  elsif line =~ /^--- (\S+)\s/
   if test(?r, $1)
    minus_tags = tags_for_file($1)
   end
  elsif line =~ /^@@ -(\d+),\d+ \+(\d+),\d+ @@/ && plus_tags != nil && minus_tags != nil
   plus_tag = find_tag_for_line(plus_tags, $2)
   minus_tag = find_tag_for_line(minus_tags, $1)
   tag = "#{minus_tag}"
   if plus_tag != minus_tag
    tag << " #{plus_tag}"
   end
   line = "#{line.chomp()} #{tag}\n"
  end
  print(line)
 }
end

annotate_patch()
exit(0)
