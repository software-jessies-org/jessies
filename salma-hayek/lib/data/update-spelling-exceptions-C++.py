#!/usr/bin/python

import os
import re
import subprocess
import urllib2

# We used to parse the cppreference.com index HTML directly, but that only
# included top-level names. https://github.com/jeaye/stdman has already done
# the work of parsing the cppreference source fully, so we can just clone
# that and then just look at the names of the generated man pages.

clone_location = '/tmp/github.com-jeaye-stdman'

os.system('git clone https://github.com/jeaye/stdman.git %s' % clone_location)

identifiers = set()
for man_page in os.listdir('%s/man/' % clone_location):
  identifiers.add(man_page)

(matches, _) = subprocess.Popen(["grep", "-hr", "#include <",
                                 "%s/man" % clone_location],
                                stdout=subprocess.PIPE).communicate()
for line in matches.splitlines():
  if 'boost' not in line:
    r = re.compile('#include <([^>]*)>')
    m = r.search(line)
    if m:
      identifier = m.group(1)
      identifiers.add(identifier)

# ...except that some things don't currently (2018-09) get a man page.
# std::atto/std::femto and friends are mentioned in std::ratio, but don't get
# their own pages. So for now at least, let's use the cppreference index too.

urls = [
        'https://en.cppreference.com/w/c/symbol_index',
        'https://en.cppreference.com/w/cpp/symbol_index'
       ]
for url in urls:
  lines = urllib2.urlopen(url).readlines()
  for line in lines:
    r = re.compile('<tt>(.*)</tt>.*<br')
    m = r.search(line)
    if m:
      identifier = m.group(1)
      identifier = re.sub('&lt;&gt;', '', identifier)
      identifier = re.sub('\(\)', '', identifier)
      identifiers.add(identifier)

# There's no great source for the preprocessor, but it hasn't changed in my
# lifetime, and there are only a few identifiers anyway. Hard-code them:
identifiers.update(['elif', 'endif', 'ifdef', 'ifndef', 'pragma', 'undef'])
# Our spelling checker splits words at punctuation (which is perhaps a bad
# default given some of the weird identifiers in C++ and POSIX), but that
# means we need to accept "func" everywhere to accept "__func__".
identifiers.add('func')

# For some reason, <iosfwd> is mentioned nowhere in the man pages.
identifiers.add('iosfwd')

# Python equivalent of Advisor.extractUniqueWords.
words = set()
for identifier in sorted(identifiers):
  for ch in '~:,.<>()[]*+-_=&!"/':
    identifier = identifier.replace(ch, ' ')
  identifier = re.sub(r'([a-z])([A-Z])', '\g<1> \g<2>', identifier)
  identifier = identifier.lower()
  words.update(identifier.split(' '))
for word in sorted(words):
  if len(word) > 3:
    print word
