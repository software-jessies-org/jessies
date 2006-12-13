#!/usr/bin/ruby

require 'pathname'

def getDiffTool(dir)
  if (dir + ".bzr").directory?()
    return "bzr diff"
  elsif (dir + "CVS").directory?()
    return "cvs diff -u"
  elsif (dir + ".hg").directory?()
    return "hg diff"
  elsif (dir + ".svn").directory?()
    return "svn diff"
  else
    while dir.root?() == false
      if (dir + "BitKeeper").directory?()
        return "bk diffs -u"
      end
      dir = dir.dirname()
    end
  end
  raise("not under version control")
end

ARGV.each {
  |file|
  dir = Pathname.new(file).realpath().dirname()
  system(getDiffTool(dir) + " " + file)
}
