#!/usr/bin/ruby -w
lines = []
sawError = false
while line = gets()
  line = line.chomp()
  lines << line
  # When nested, we get the make level in square brackets.
  if line.match(/^g?make(\[\d+\])?: \*\*\*/)
    $stderr.puts(lines)
    while line = gets()
      $stderr.puts(line)
    end
    break
  end
  progressLine = nil
  # Match Compiling, Generating etc.
  if line.match(/^[A-Z][a-z]+ing .*\.\.\./)
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
