#!/usr/bin/ruby -w

# Cope with symbolic links to this script.
require "pathname.rb"
salma_hayek = Pathname.new(__FILE__).realpath().dirname().dirname()
require "#{salma_hayek}/bin/invoke-java.rb"

invoker = Java.new("JavaHpp", "e/tools/JavaHpp")
invoker.invoke()
