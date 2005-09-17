#!/usr/bin/ruby -w
UNAME=`uname`.chomp()
TARGET_OS = UNAME.sub(/CYGWIN.*/, "Cygwin")
puts(TARGET_OS)
