#!/usr/bin/ruby -w

def getSubversionVersion(directory)
  IO.popen("svnversion #{directory}") {
    |pipe|
    while pipe.gets()
      line = $_
      # The second number, if there are two, is the more up-to-date.
      # (In Subversion's model, commit doesn't necessarily require update.)
      if line.match(/^(?:\d+:)?(\d+)/)
        return $1.to_i()
      end
    end
  }
  return nil
end

project_root = ARGV.shift()
salma_hayek = ARGV.shift()

fields = []
# The third field is called the build version or the update version and has a maximum value of 65,535.
fieldSize = 64 * 1024
# We try to separate the project_root and salma_hayek versions into different fields.
unifiedVersion = getSubversionVersion(project_root) * fieldSize + getSubversionVersion(salma_hayek)
remainder = unifiedVersion
field = remainder % fieldSize
remainder /= fieldSize
fields.push(field)
# The second field is the minor version and has a maximum value of 255.
fieldSize = 256
field = remainder % fieldSize
remainder /= fieldSize
fields.push(field)
# The first field is the major version and has a maximum value of 255.
fieldSize = 256
field = remainder % fieldSize
remainder /= fieldSize
fields.push(field)

puts(fields.reverse.join("."))
