#!/usr/bin/ruby -w

# Cope with symbolic links to this script.
require "pathname.rb"
salma_hayek = Pathname.new(__FILE__).realpath().dirname().dirname()

require "#{salma_hayek}/bin/uuid.rb"

if ARGV[0] == "--diskId"
  diskIdTag = "DiskId='1'"
else
  diskIdTag = ""
end
directoryPathToFileNames = Hash.new() {
  |hash, directoryPath|
  hash[directoryPath] = []
}
$stdin.each() {
  |filePath|
  filePath.chomp!()
  pathComponents = filePath.split("/")
  fileName = pathComponents.pop()
  directoryPath = pathComponents.join("/")
  directoryPathToFileNames[directoryPath].push(fileName)
}
numberOfDirectories = directoryPathToFileNames.length()
puts("<Include>")
directoryNumber = 0
fileNumber = 0
directoryPathToFileNames.each_pair() {
  |directoryPath, fileNames|
  pathComponents = directoryPath.split("/")
  pathComponents.each() {
    |directoryName|
    puts("<Directory Id='directory#{directoryNumber}' Name='name#{directoryNumber}' LongName='#{directoryName}'>")
    directoryNumber += 1
  }
  guid = uuid()
  puts("<Component Id='component#{directoryNumber}' Guid='#{guid}'>")
  fileNames.each() {
    |fileName|
    filePathComponents = pathComponents + [fileName]
    filePath = filePathComponents.join("/")
    puts("<File Id='file#{fileNumber}' Name='name#{fileNumber}' LongName='#{fileName}' #{diskIdTag} src='#{filePath}' />")
    fileNumber += 1
  }
  puts("</Component>")
  pathComponents.reverse().each() {
    |directoryName|
    puts("</Directory>")
  }
}
puts("</Include>")
