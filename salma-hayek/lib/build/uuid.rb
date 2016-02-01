#!/usr/bin/ruby -w
require "dl"
require "dl/import"

module Rpcrt4
  extend(DL::Importer)
  dlload("rpcrt4")
  extern("int UuidCreateSequential(unsigned char*)")
end

def uuid()
  # Contrary to what the MS documentation says, UuidCreateSequential fills
  # a buffer you give it.
  uuid = "\0" * 16
  rc = Rpcrt4.UuidCreateSequential(uuid)
  if rc != 0
    raise("UuidCreateSequential returned #{rc}.")
  end
  
  # Output the UUID in the RFC4122 format.
  result = ""
  i = 0
  uuid.each_byte() {
    |byte|
    result << (sprintf("%02x", byte))
    i += 1
    if [4, 6, 8, 10].include?(i)
      result << "-"
    end
  }
  return result
end

if __FILE__ == $0
  puts(uuid())
  exit(0)
end
