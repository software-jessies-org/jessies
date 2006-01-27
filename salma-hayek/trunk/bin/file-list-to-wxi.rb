#!/usr/bin/ruby -w
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
# "sudo apt-get install uuid" gets you a suitable program on Debian
guids = `uuid -n #{numberOfDirectories}`.split("\n")
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
  guid = guids.pop()
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
