package e.edit;

import java.util.*;
import e.ptextarea.*;
import e.util.*;

public class RubyDocumentationResearcher implements WorkspaceResearcher {
    public String research(String string) {
        String ri = Advisor.findToolOnPath("ri");
        if (ri == null) {
            return "";
        }
        ArrayList<String> lines = new ArrayList<String>();
        ArrayList<String> errors = new ArrayList<String>();
        int status = ProcessUtilities.backQuote(null, new String[] { "ruby", ri, "-T", "-f", "html", string }, lines, errors);
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
        
        // FIXME: we should link up stuff like "Includes:", "Class methods:", and "Instance methods:". See Array for an example of all three.
        
        return (result.contains("<error>") ? "" : result);
    }
    
    /** Returns true for Ruby files. */
    public boolean isSuitable(ETextWindow textWindow) {
        return textWindow.isRuby();
    }
    
    /** Handles our non-standard "ri:" scheme. */
    public boolean handleLink(String link) {
        if (link.startsWith("ri:")) {
            Advisor.getInstance().showDocumentation(research(link.substring(3)));
            return true;
        }
        return false;
    }
}
