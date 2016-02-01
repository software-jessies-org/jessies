#!/usr/bin/python

"""Output Python documentation in HTML.

pydoc(1) contains most of the code we need to grovel around Python's internals
looking for documentation, but it won't provide it in convenient form, nor is
its command-line interface convenient.
"""

import keyword
import inspect
import pydoc
import re
import sys

def PrintHtml(name, things):
  """Print HTML documentation to stdout."""
  ambiguous = len(things) > 1
  
  content = ""
  for thing in things:
    obj, name = pydoc.resolve(thing, forceload=0)
    title = pydoc.describe(obj)
    if ambiguous:
      if inspect.ismethoddescriptor(obj):
        content += '\n\n<h2>method %s in class %s</h2>\n' % (obj.__name__, obj.__dict__)
      else:
        content += '<h2>%s in module <a href="py:%s">%s</a></h2>\n' % (title, obj.__module__, obj.__module__)
    content += pydoc.html.document(obj, name)
  
  if ambiguous:
    title = 'Matches for "%s"' % name
    content = '<h1>%s</h1>\n\n%s' % (title, content)
  
  page = pydoc.html.page(title, content)
  
  # Note: rewriting the anchors in a form more useful to Evergreen should be in Evergreen, not here.
  # The rewriting here should be generally useful to anyone or anything that needs Python documentation.
  
  # Remove a couple of useless (and seemingly broken) links.
  page = page.replace('<a href=".">index</a><br>', '')
  page = re.sub('<br><a href="[^"]+\.html">Module Docs</a>', '', page)
  
  # There's a bug in pydoc that makes it output the text of the "Modules" section in cyan instead of white.
  page = re.sub('"#fffff"', '"#ffffff"', page)
  # The explicit font specifications are unnecessary manual uglification.
  page = re.sub(' face="[^"]+"', '', page);
  
  sys.stdout.write(page + '\n')


def FindBuiltIn(name):
  matches = set()
  
  if keyword.iskeyword(name):
    # FIXME: do something clever!
    pass
  
  # Import all the standard modules.
  for module_name in sys.modules.keys():
    try:
      __import__(module_name)
    except ImportError:
      # FIXME: why does this happen?
      pass
  
  # FIXME: unfortunately, this seems to give us some kind of second-class "methoddescriptor" rather than a "method".
  # Look for matches in the classes contained in modules we've imported.
  #for module in sys.modules.values():
  #  for klass_name, klass in inspect.getmembers(module, inspect.isclass):
  #    obj = klass.__dict__.get(name)
  #    if obj:
  #      matches.add(obj)
  
  # Look for matches in the modules we've imported.
  for module in sys.modules.values():
    if module:
      obj = module.__dict__.get(name)
      if obj:
        matches.add(obj)
  
  return matches


def PrintAmbiguousMatches(name, matches):
  sys.stdout.write('<p>%s is ambiguous. Did you mean:\n' % name)
  for match in matches:
    dotted_name = match.__module__ + '.' + match.__name__
    sys.stdout.write('<p><a href="py:%s">%s</a>\n' % (dotted_name, dotted_name))


def PrintDocumentation(name):
  obj = pydoc.locate(name, forceload=1)
  if obj:
    PrintHtml(name, [obj])
    sys.exit(0)
  
  matches = FindBuiltIn(name)
  if matches:
    #PrintAmbiguousMatches(name, matches)
    PrintHtml(name, matches)
    sys.exit(0)
  
  sys.exit(1)


def main():
  for name in sys.argv[1:]:
    PrintDocumentation(name)


if __name__ == '__main__':
  main()
