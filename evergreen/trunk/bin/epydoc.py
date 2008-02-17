#!/usr/bin/python

"""Output Python documentation in HTML.

pydoc(1) contains most of the code we need to grovel around Python's internals
looking for documentation, but it won't provide it in convenient form, nor is
its command-line interface convenient.
"""

__author__ = 'Elliott Hughes <enh@jessies.org>'

import pydoc
import re
import sys

def PrintHtml(thing):
  """Print HTML documentation to stdout."""
  try:
    object, name = pydoc.resolve(thing, forceload=0)
    page = pydoc.html.page(pydoc.describe(object), pydoc.html.document(object, name))
        
    # Note: rewriting the anchors in a form more useful to Evergreen should be in Evergreen, not here.
    # The rewriting here should be generally useful to anyone or anything that needs Python documentation.
    
    # Remove a couple of useless (and seemingly broken) links.
    page = page.replace('<a href=".">index</a><br>', '')
    page = re.sub('<br><a href="[^"]+\.html">Module Docs</a>', '', page)
    
    # There's a bug in pydoc that makes it output the text of the "Modules" section in cyan instead of white.
    page = re.sub('"#fffff"', '"#ffffff"', page)
    # The explicit font specifications are unnecessary manual uglification.
    page = re.sub(' face="[^"]+"', '', page);
    
    sys.stdout.write(page)
    sys.stdout.write('\n')
  except (ImportError, pydoc.ErrorDuringImport), value:
    print value

if __name__ == '__main__':
  for arg in sys.argv[1:]:
    obj = pydoc.locate(arg, forceload=1)
    if obj:
      PrintHtml(obj)
    else:
      # FIXME: assume this is a method name ("split", say), and try to find it.
      sys.exit(1)
