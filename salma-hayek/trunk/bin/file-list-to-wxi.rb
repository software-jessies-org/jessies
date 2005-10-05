#!/usr/bin/ruby -w
puts("<Include>")
directoryNumber = 0
fileNumber = 0
$stdin.each() {
  |filePath|
  filePath.chomp!()
  pathComponents = filePath.split("/")
  fileName = pathComponents.pop()
  pathComponents.each() {
    |directoryName|
    puts("<Directory Id='directory#{directoryNumber}' Name='name#{directoryNumber}' LongName='#{directoryName}'>")
    directoryNumber += 1
  }
  puts("<Component Id='component#{fileNumber}'>")
  puts("<File Id='file#{fileNumber}' Name='name#{fileNumber}' LongName='#{fileName}' DiskId='1' src='#{filePath}' />")
  puts("</Component>")
  fileNumber += 1
  pathComponents.reverse().each() {
    |directoryName|
    puts("</Directory>")
  }
}
puts("</Include>")
