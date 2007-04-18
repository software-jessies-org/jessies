package e.edit;

import java.util.*;
import e.ptextarea.*;
import e.util.*;

public class RubyDocumentationResearcher implements WorkspaceResearcher {
    public String research(String string, ETextWindow textWindow) {
        String ri = Advisor.findToolOnPath("ri");
        if (ri == null) {
            return "";
        }
        ArrayList<String> lines = new ArrayList<String>();
        ArrayList<String> errors = new ArrayList<String>();
        int status = ProcessUtilities.backQuote(null, new String[] { "ruby", ri, "-T", "-f", "html", string }, lines, errors);
        
        String className = string;
        String lastLine = "";
        for (int i = 0; i < lines.size(); ++i) {
            String line = lines.get(i);
            if (lastLine.equals("<h2>Includes:</h2>")) {
                lines.set(i, rewriteIncludeListAsLinks(line));
            } else if (lastLine.equals("<h2>Class methods:</h2>")) {
                lines.set(i, rewriteMethodListAsLinks(line, className + "::"));
            } else if (lastLine.equals("<h2>Instance methods:</h2>")) {
                lines.set(i, rewriteMethodListAsLinks(line, className + "#"));
            }
            lastLine = line;
        }
        
        String result = StringUtilities.join(lines, "\n");
        
        // Rewrite references such as IO#puts as links to ri:IO#puts, not forgetting more complicated examples such as Zlib::GzipWriter#puts.
        
        // On a normal ri page, any class or method is surrounded by <tt></tt>.
        result = result.replaceAll("<tt>(([A-Za-z0-9_?]+(#|::|\\.))?([A-Za-z0-9_?]+)+)</tt>", "<a href=\"ri:$1\">$1</a>");
        // On an error page, we get a comma-separated list of non-delimited classes or methods.
        if (result.startsWith("More than one ")) {
            result = result.replaceAll("\\b(([A-Za-z0-9_?]+(#|::|\\.))?([A-Za-z0-9_?]+)+)(, |$)", "<a href=\"ri:$1\">$1</a>$5");
        }
        
        // At the top of an individual method's page, link to the defining class.
        result = result.replaceAll("<b>(\\w+)(::|#)(.+?)</b>", "<b><a href=\"ri:$1\">$1</a>$2$3</b>");
        
        return (result.contains("<error>") ? "" : result);
    }
    
    private String rewriteMethodListAsLinks(String line, String prefix) {
        // Example: "[], new<p>"
        String[] methods = line.replaceAll("<p>$", "").split(", ");
        for (int i = 0; i < methods.length; ++i) {
            methods[i] = ("<a href=\"ri:" + prefix + methods[i] + "\">" + methods[i] + "</a>");
        }
        return StringUtilities.join(methods, ", ");
    }
    
    private String rewriteIncludeListAsLinks(String line) {
        // Example: "Comparable(&lt;, &lt;=, ==, &gt;, &gt;=, between?), Enumerable(all?, any?, collect, detect, each_cons, each_slice, each_with_index, entries, enum_cons, enum_slice, enum_with_index, find, find_all, grep, include?, inject, map, max, member?, min, partition, reject, select, sort, sort_by, to_a, to_set, zip)<p>"
        String result = "";
        String[] modules = line.replaceAll("<p>$", "").split("\\), ");
        for (String module : modules) {
            int openParenthesisIndex = module.indexOf('(');
            String moduleName = module.substring(0, openParenthesisIndex);
            String[] methods = module.substring(openParenthesisIndex + 1).split(", ");
            for (int i = 0; i < methods.length; ++i) {
                methods[i] = ("<a href=\"ri:" + moduleName + "#" + methods[i] + "\">" + methods[i] + "</a>");
            }
            if (result.length() > 0) {
                result += "\n<p>";
            }
            result += "<a href=\"ri:" + moduleName + "\">" + moduleName + "</a> methods:<blockquote>" + StringUtilities.join(methods, ", ") + "</blockquote>";
        }
        return result;
    }
    
    /** Returns true for Ruby files. */
    public boolean isSuitable(ETextWindow textWindow) {
        return textWindow.getFileType() == FileType.RUBY;
    }
    
    /** Handles our non-standard "ri:" scheme. */
    public boolean handleLink(String link) {
        if (link.startsWith("ri:")) {
            Advisor.getInstance().showDocumentation(research(link.substring(3), null));
            return true;
        }
        return false;
    }
}
