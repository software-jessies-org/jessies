#
# This is the 'ri' output class for Edit HTML advice output.
#

# Array
# join
# Array#join
# File::join

require 'rdoc/ri/ri_display.rb'

class  DefaultDisplay
    
    include RiDisplay
    
    def initialize(options)
        @options = options
        @formatter = @options.formatter.new(@options, "     ")
    end
    
    def display_usage
        RI::Options::OptionList.usage(short_form=true)
    end

    def display_method_info(method)
        tag("b") { method.full_name }
        display_params(method)
        tag("p") { @formatter.display_flow(method.comment) }
        if method.aliases && !method.aliases.empty?
            aka = "(also known as "
            aka << method.aliases.map {|a| a.name }.join(", ")
            aka << ")"
            tag("p") { aka }
        end
    end

    def display_class_info(klass, ri_reader)
        superclass = klass.superclass_string
        
        if superclass
            superclass = " < " + superclass
        else
            superclass = ""
        end
        
        @formatter.draw_line(klass.display_name + ": " + klass.full_name + superclass)
        
        display_flow(klass.comment)
        @formatter.draw_line
        
        unless klass.includes.empty?
            @formatter.blankline
            @formatter.display_heading("Includes:", 2, "")
            incs = []
            klass.includes.each {
                |inc|
                inc_desc = ri_reader.find_class_by_name(inc.name)
                if inc_desc
                    str = inc.name + "("
                    str << inc_desc.instance_methods.map{|m| m.name}.join(", ")
                    str << ")"
                    incs << str
                else
                    incs << inc.name
                end
            }
            @formatter.wrap(incs.sort.join(', '))
        end
        
        unless klass.constants.empty?
            @formatter.blankline
            @formatter.display_heading("Constants:", 2, "")
            len = 0
            klass.constants.each { |c| len = c.name.length if c.name.length > len }
            len += 2
            klass.constants.each {
                |c|
                @formatter.wrap(c.value,
                @formatter.indent+((c.name+":").ljust(len)))
            }
        end
        
        unless klass.class_methods.empty?
            @formatter.blankline
            @formatter.display_heading("Class methods:", 2, "")
            @formatter.wrap(klass.class_methods.map{|m| m.name}.sort.join(', '))
        end
        
        unless klass.instance_methods.empty?
            @formatter.blankline
            @formatter.display_heading("Instance methods:", 2, "")
            @formatter.wrap(klass.instance_methods.map{|m| m.name}.sort.join(', '))
        end
        
        unless klass.attributes.empty?
            @formatter.blankline
            @formatter.wrap("Attributes:", "")
            @formatter.wrap(klass.attributes.map{|a| a.name}.sort.join(', '))
        end
    end
    
    # Display a list of method names
    
    def display_method_list(methods)
        puts "More than one method matched your request. You can refine"
        puts "your search by asking for information on one of:\n\n"
        @formatter.wrap(methods.map {|m| m.full_name} .join(", "))
    end
    
    def display_class_list(namespaces)
        puts "More than one class or module matched your request. You can refine"
        puts "your search by asking for information on one of:\n\n"
        @formatter.wrap(namespaces.map {|m| m.full_name}.join(", "))
    end
    
    def list_known_classes(classes)
        if classes.empty?
            puts "Before using ri, you need to generate documentation"
            puts "using 'rdoc' with the --ri option"
        else
            @formatter.draw_line("Known classes and modules")
            @formatter.blankline
            @formatter.wrap(classes.sort.join(", "))
        end
    end

private

    def display_params(method)
        
        params = method.params
        
        if params[0,1] == "("
            if method.is_singleton
                params = method.full_name + params
            else
                params = method.name + params
            end
        end
        params.split(/\n/).each {|p| @formatter.wrap(p) }
    end
    
    def display_flow(flow)
        if !flow || flow.empty?
            @formatter.wrap("(no description...)")
        else
            @formatter.display_flow(flow)
        end
    end
    
    def tag(code)
        puts "<#{code}>"
        puts yield
        puts "</#{code}>"
    end
    
    def entag(code, string)
        return "<#{code}>#{string}</#{code}>"
    end
 
    def puts_tag(code)
        puts "<#{code}>"
        yield
        puts "</#{code}>"
    end
 
    def putMethodList(names)
        puts names.sort.join("<br>")
    end

    def putVerbatim(txt)
        tag("pre") { txt }
    end

    def putParagraph(txt)
        tag("p") { stripFormatting(txt) }
    end

    def putMessage(txt)
        tag("message") { stripFormatting(txt) }
    end

    def newline
    end

    def putMethodHeader(cname, type, mname, callseq)
        tag("b") { "#{cname}#{type}#{mname}" }
        tag("p") { stripFormatting(callseq) }
    end

    def putClassHeader(type, name, superClass, subclasses)
        puts entag("b", "#{type} #{name}") + "<br>"
        puts entag("b", "Superclass: #{superClass}") if superClass and superClass != "Object"
        puts entag("p", "Known subclasses: #{subclasses.join(',')}") if subclasses
    end

    def putClassMethods(names)
        puts entag("b", "Methods<br>")
        putMethodList(names)
    end
end
