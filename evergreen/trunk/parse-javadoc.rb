#!/usr/bin/ruby -w

# Represents a class, field or method.
class Item
    def initialize(name)
        @name = name
        @doc = ""
        @summary = ""
    end
    
    def append_doc_line(line)
        @doc += line
    end
    
    def extract_summary
        @doc =~ /<\/H3>\n<PRE>(.+)<\/PRE>/m
        summary = $1
        @doc = nil
        
        # Convert all entities to spaces, all whitespace runs to
        # a single space, and join everything onto a single line.
        summary.gsub!(/\&[^;]+;/, " ")
        summary.gsub!(/\s+/, " ")
        summary.gsub!(/\n/, "")
        
        # Remove HTML formatting.
        summary.gsub!(/<\/?B>/, "")
        summary.gsub!(/<\/?A[^>]*>/, "")

        @summary = summary
    end
    
    def to_s
        if @summary.length() > 0
            return "#{@name}\t#{@summary}"
        else
            return @name
        end
    end
end

#####################

# Parses a single file of JavaDoc HTML.
class JavaDocParser
    def initialize
        @class = nil
        @fields = []
        @methods = []
        @constructors = []
        @annotationTypes = []
        @enumConstants = []
    end

    def parse_file(filename)
        File.open(filename) {
            |file|
            parse_contents(file)
        }
        if @class == nil
            raise "no class"
        end
        puts("File:#{File.expand_path(filename)}")
        puts("Class:#{@class}")
        show_items("C", @constructors)
        show_items("M", @methods)
        show_items("F", @fields)
        show_items("A", @annotationTypes)
        show_items("E", @enumConstants)
        puts("")
    end

    def parse_contents(file)
        items = nil
        item = nil
        
        file.each_line() {
            |line|
            #puts(">>>>> #{line}")
            
            if line =~ /^<META NAME="keywords" CONTENT="(.+) .+">$/
                @class = Item.new($1)
            elsif line =~ /^<!-- =+ START OF CLASS DATA =+ -->$/
                item = @class
            elsif line =~ /^<!-- =+ .+ SUMMARY =+ -->$/
                item = nil
            elsif line =~ /^<A NAME=\"(.*)\">.*<\/A><H3>$/
                if item != nil
                    raise "haven't finished #{item}, so can't start #$1!"
                end
                item = Item.new($1)
                item.append_doc_line(line)
            elsif item != nil && item != @class && (line =~ /^<HR>$/ || line =~ /^<!-- =+ .* =+ -->/)
                items << item
                item.extract_summary()
                item = nil
            elsif item != nil
                item.append_doc_line(line)
            end
            
            # Keep track of what kind of item we're looking at.
            if line =~ /^<!-- =+ FIELD DETAIL =+ -->$/
                items = @fields
            elsif line =~ /^<!-- =+ METHOD DETAIL =+ -->$/
                items = @methods
            elsif line =~ /^<!-- =+ CONSTRUCTOR DETAIL =+ -->$/
                items = @constructors
            elsif line =~ /^<!-- =+ ANNOTATION TYPE MEMBER DETAIL =+ -->$/
                items = @annotationTypes
            elsif line =~ /^<!-- =+ ENUM CONSTANT DETAIL =+ -->$/
                items = @enumConstants
            end
        }
    end
    
    def show_items(name, items)
        items.each() {
          |item|
          puts("#{name}:#{item}")
        }
        #puts("#{name}:")
        #puts("  #{items.join("\n  ")}")
    end
end

#####################

if ARGV.length() == 0
    puts("Usage: parse-javadoc.rb <java-doc-directories>... > javadoc-summary.txt")
    puts("")
    puts("Probable JavaDoc locations on this machine:")
    locations = `locate allclasses-noframe.html | grep -v "^/Previous Systems/"`
    puts(locations.gsub(/allclasses-noframe\.html/, ""))
    exit(1)
end

# Find all the .html files under the JavaDoc directory.
files = []
ARGV.each() {
    |java_doc_directory|
    dir = java_doc_directory.gsub(/\/$/, "")
    files += Dir["#{dir}/**/*.html"]
}

# Exclude the files that don't correspond to classes.
# Conveniently, apart from "index.html" and "packages.html",
# their names all contain a hyphen, which
# isn't a valid character in a class or package name. The
# only trouble we're likely to have here is if someone names
# their class 'index' or 'packages', but that would be a
# violation of the naming conventions that insist on an
# initial capital for a class name, so "hard luck".
files.delete_if { |file| /-/ === file }
files.delete_if { |file| /\/(index|packages)\.html/ === file }

#puts("Files to process: #{files.length()}\n#{files.join("\n")}")

# Parse each .html file in turn.
files.each() {
    |filename|
    begin
        parser = JavaDocParser.new
        parser.parse_file(filename)
    rescue Exception => ex
        puts("Failure parsing #{filename}: " + ex)
        puts(ex.backtrace().join("\n"))
        exit(1)
    end
}
