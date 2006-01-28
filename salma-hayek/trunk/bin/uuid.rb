#!/usr/bin/ruby -w

require "Win32API"

# Contrary to what the MS documentation says, UuidCreateSequential fills
# a buffer you give it.
uuid = "\0" * 16
uuid_create = Win32API.new("rpcrt4.dll", "UuidCreateSequential", ['p'], 'i')
rc = uuid_create.Call(uuid)
if rc != 0
  $stderr.puts("UuidCreateSequential returned #{rc}.")
  exit(1)
end

# Output the UUID in the RFC4122 format.
i = 0
uuid.each_byte() {
  |byte|
  printf("%02x", byte)
  i += 1
  if [4, 6, 8, 10].include?(i)
    print("-")
  end
}
print("\n")
exit(0)
