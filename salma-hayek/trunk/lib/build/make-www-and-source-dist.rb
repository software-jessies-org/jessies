#!/usr/bin/ruby -w
require "pathname"
salma_hayek = Pathname.new(__FILE__).realpath().dirname().dirname().dirname()
failed = false
Dir.glob("#{salma_hayek}/../*/.svn").each() {
    |svnDirectory|
    repository = Pathname.new(svnDirectory).realpath().dirname()
    Dir.chdir(repository) {
        # "At revision 2962" is uninteresting.
        updateResult = `{ svn update && make www-dist source-dist; } 2>&1`
        if $? != 0
            $stderr.puts("Failed in #{repository}:")
            $stderr.puts(updateResult)
            failed = true
        end
    }
}
if failed
    exit(1)
end
