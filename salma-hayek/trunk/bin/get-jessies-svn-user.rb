#!/usr/bin/ruby -w

require 'fileutils.rb'
require 'pathname.rb'

# Cope with symbolic links to this script.
salma_hayek = Pathname.new("#{__FILE__}/..").realpath().dirname()

`cd #{salma_hayek} && svn info`.each() {
  |line|
  if line.match(/svn\+ssh:\/\/(\w+)@/)
    puts($1)
  end
}
