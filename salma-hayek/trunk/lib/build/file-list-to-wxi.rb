#!/usr/bin/ruby -w

# Cope with symbolic links to this script.
require "pathname.rb"
salma_hayek = Pathname.new(__FILE__).realpath().dirname().dirname().dirname()

require "#{salma_hayek}/lib/build/uuid.rb"

xmlToInjectAfterCygwinLauncher = <<EOT
<Shortcut Id='ShortcutShortcut' Advertise='yes' Directory='DesktopFolder' Name='$(env.HUMAN_PROJECT_NAME)' Arguments='"[projectResources]bin\\$(env.MACHINE_PROJECT_NAME)"' WorkingDirectory='DesktopFolder' Icon='$(env.MACHINE_PROJECT_NAME).ico' />
EOT
# While an advertised shortcut seems to be the approach we're expected to take, it can't be made optional.
xmlToInjectAfterCygwinLauncher = ""

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
componentNumber = 0
fileNumber = 0
directoryPathToFileNames.each_pair() {
  |directoryPath, fileNames|
  pathComponents = directoryPath.split("/")
  pathComponents.each() {
    |directoryName|
    puts("<Directory Id='directory#{directoryNumber}' Name='#{directoryName}'>")
    directoryNumber += 1
  }
  guid = uuid()
  puts("<Component Id='component#{componentNumber}' Guid='#{guid}'>")
  componentNumber += 1
  fileNames.each() {
    |fileName|
    filePathComponents = pathComponents + [fileName]
    filePath = filePathComponents.join("/")
    puts("<File Id='file#{fileNumber}' Name='#{fileName}' DiskId='1' Source='#{filePath}' />")
    if fileName == "cygwin-launcher.exe"
      puts(xmlToInjectAfterCygwinLauncher)
    end
    fileNumber += 1
  }
  puts("</Component>")
  pathComponents.reverse().each() {
    |directoryName|
    puts("</Directory>")
  }
}
puts("</Include>")
