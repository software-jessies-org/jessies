#!/usr/bin/ruby -w
def filterBuildOutput(inputIo)
  lines = []
  sawError = false
  while line = inputIo.gets()
    line = line.chomp()
    lines << line
    # We need to match:
    # universal.make:378: *** Unable to find /bin/javac --- do you only have a JRE installed?.  Stop.
    # make: *** [recurse] Error 2
    # gmake[2]: *** [recurse] Error 2
    
    # We also want to see javac(1) warnings:
    # src/Test.java:174: warning: [deprecation] toURL() in java.io.File has been deprecated
    #     URL url = f.toURL();
    #                ^
    if line.match(/\*\*\*/) || line.match(/warning: /)
      $stderr.puts(lines)
      while line = gets()
        $stderr.puts(line)
      end
      break
    end
    progressLine = nil
    # Match Compiling, Generating etc.
    if line.match(/^[A-Z][a-z]+ing\b.*\.\.\./)
      progressLine = line
    elsif line.match(/^(?:cc|g\+\+) .*?\/([^\/ ]+)$/)
      # I don't want to override the built-in rules for compilation and it's hard to hook them to do extra echoing.
      # The regular expression above might be ugly but at least it's small, isolated and won't cause a build failure if it breaks.
      sourceFile = $1
      progressLine = "Compiling #{sourceFile}..."
    else
      next
    end
    # TODO: Stamping each line with the time since invocation would be a neat way of quantifying which is the most expensive part of the build.
    puts(progressLine)
    $stdout.flush()
    lines = []
  end
end

IO.popen("-") {
  |buildOutputIo|
  if buildOutputIo == nil
    exec(*ARGV)
  end
  filterBuildOutput(buildOutputIo)
}
