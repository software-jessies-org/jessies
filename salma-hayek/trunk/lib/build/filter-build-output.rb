#!/usr/bin/ruby -w

def isWorthyOfOutput(line)
  # We need to match:
  # universal.make:378: *** Unable to find /bin/javac --- do you only have a JRE installed?.  Stop.
  # make: *** [recurse] Error 2
  # gmake[2]: *** [recurse] Error 2
  if line.match(/\*\*\*/)
    return true
  end
  
  # We also want to see javac(1) warnings:
  # src/Test.java:174: warning: [deprecation] toURL() in java.io.File has been deprecated
  #     URL url = f.toURL();
  #                ^
  if line.match(/warning: /)
    return true
  end
  
  # We also want to see findbugs warnings, which:
  # /Users/mad/software.jessies.org/work/terminator/src/terminator/FindDialog.java:80:80 IS: Inconsistent synchronization of terminator.FindDialog.formDialog; locked 77% of time (M)
  # /Users/mad/software.jessies.org/work/terminator/src/terminator/terminal/TerminalControl.java:-1:-1 UuF: Unused field: terminator.terminal.TerminalControl.stepModeReader (M)
  # But, rather lamely, I'll take the simple, low risk approach of matching the final warning counter.
  # Warnings generated: 55
  if line.match(/Warnings generated: /)
    return true
  end
  
  # We also want to see make warnings like:
  # make: Circular .generated/classes/e/debug/EventDispatchThreadHangMonitor$Tests$5$1$1.class <- .generated/java.build-finished dependency dropped.
  # make[1]: Circular /home/martind/software.jessies.org/work/salma-hayek/.generated/classes/e/tools/JarExplorer$3.class <- .generated/java.build-finished dependency dropped.
  # But we don't want to see:
  # make[1]: Entering directory `/Users/mad/software.jessies.org/work/salma-hayek'
  # make[1]: Leaving directory `/Users/mad/software.jessies.org/work/salma-hayek'
  if line.match(/^make(?:\[\d+\])?: ((?:Entering|Leaving) directory )?/)
    if $1 != nil
      return false
    end
    return true
  end
  
  if line.match(/All \d+ tests passed/)
    return true
  end
  
  return false
end

def filterBuildOutput(inputIo)
  lines = []
  sawError = false
  while line = inputIo.gets()
    line = line.chomp()
    lines << line
    if isWorthyOfOutput(line)
      $stderr.puts(lines)
      while line = inputIo.gets()
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
    $stderr.reopen($stdout)
    exec(*ARGV)
  end
  filterBuildOutput(buildOutputIo)
}
if $?.success?() != true
  exit(1)
end
