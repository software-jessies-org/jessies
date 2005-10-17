#!/usr/bin/ruby -w

require 'fileutils.rb'
require 'pathname.rb'

# Cope with symbolic links to this script.
salma_hayek = Pathname.new("#{__FILE__}/..").realpath().dirname()

`cd #{salma_hayek} && svn info`.each() {
  |line|
  if line.match(/svn\+ssh:\/\/(\w+)@/)
    puts($1)
    exit(0)
  end
}
$stderr.puts("the copy of get-jessies-svn-user.rb which you run needs to be in a checked-out Subversion work area")
$stderr.puts("this copy was #{__FILE__}")
