#!/usr/bin/python

import re
import urllib2

identifiers = set()

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

# Python equivalent of Advisor.extractUniqueWords.
words = set()
for identifier in sorted(identifiers):
  identifier = identifier.replace('_', ' ')
  identifier = re.sub(r'([a-z])([A-Z])', '\g<1> \g<2>', identifier)
  identifier = identifier.lower()
  words.update(identifier.split(' '))
for word in sorted(words):
  if len(word) > 3:
    print word
