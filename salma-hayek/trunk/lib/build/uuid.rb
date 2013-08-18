#!/usr/bin/ruby -w

def uuid()
  require "Win32API"
  
  # Contrary to what the MS documentation says, UuidCreateSequential fills
  # a buffer you give it.
  uuid = "\0" * 16
  uuid_create = Win32API.new("rpcrt4.dll", "UuidCreateSequential", ["p"], "i")
  rc = uuid_create.Call(uuid)
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
