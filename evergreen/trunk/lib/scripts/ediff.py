#!/usr/bin/python

# A "diff" utility that also includes information about changes within lines.

# Each line begins with a single-character code, similar to "diff -u" output:
#   "-"  line unique to file1.
#   "+"  line unique to file2.
#   " "  line common to both files.
#   "?"  non-whitespace characters on this line mark the changed characters on
#        the previous line (which will have been a "-" or "+" line).

# The exit status is POSIX compliant:
#   0  no differences were found.
#   1  differences were found.
#  >1  an error occurred.

# More on POSIX diff(1) (but note that POSIX doesn't include -u):
# http://www.opengroup.org/onlinepubs/009695399/utilities/diff.html

import difflib
import sys

def main(args):
  if len(args) != 4:
    sys.stderr.write("usage: %s LABEL1 FILENAME1 LABEL2 FILENAME2\n" % sys.argv[0])
    sys.exit(2)
  label1, filename1, label2, filename2 = args
  print '--- %s' % label1
  print '+++ %s' % label2
  file1_lines = open(filename1).readlines()
  file2_lines = open(filename2).readlines()
  difference_count = 0
  for line in difflib.ndiff(file1_lines, file2_lines):
    # diff(1) uses " ", "+", and "-" at the start of lines.
    # Python's difflib uses "  ", "+ ", and "- ", so we need to remove line[1].
    sys.stdout.write("%s%s" % (line[0], line[2:]))
    if line[0] != ' ':
      difference_count += 1
  sys.exit(min(difference_count, 1))

if __name__ == '__main__':
  main(sys.argv[1:])
