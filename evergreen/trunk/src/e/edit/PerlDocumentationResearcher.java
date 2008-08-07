package e.edit;

import java.util.*;
import e.ptextarea.*;
import e.util.*;

public class PerlDocumentationResearcher implements WorkspaceResearcher {
    public String research(String string, ETextWindow textWindow) {
        String perldoc = Advisor.findToolOnPath("perldoc");
        if (perldoc == null) {
            return "";
        }
        ArrayList<String> lines = new ArrayList<String>();
        ArrayList<String> errors = new ArrayList<String>();
        int status = ProcessUtilities.backQuote(null, ProcessUtilities.makeShellCommandArray(perldoc + " -u -f " + string + " | pod2html"), lines, errors);
        String result = StringUtilities.join(lines, "\n");
        
        // Swing doesn't seem to cope with this, so remove it.
        result = result.replaceAll("^<\\?xml version=\"1.0\" \\?>", "");
        // Asking pod2html(1) to not produce an index doesn't appear to prevent the output of an empty paragraph.
        result = result.replaceAll("<p><a name=\"__index__\"></a></p>", "");
        
        // FIXME: pod2html(1) output is still pretty hopeless: incorrectly-scoped "pre" sections, dodgy links that we don't have proper control over (we'll have to rewrite them here), bad/missing links to man pages (including other Perl man pages), and a great number of dodgy "dd" tags.
        
        return (result.startsWith("No documentation for perl ") ? "" : result);
    }
    
    /** Returns true for Perl files. */
    public boolean isSuitable(ETextWindow textWindow) {
        return textWindow.getFileType() == FileType.PERL;
    }
    
    /** Handles our non-standard "perldoc:" scheme. */
    public boolean handleLink(String link) {
        if (link.startsWith("perldoc:")) {
            Advisor.getInstance().setDocumentationText(research(link.substring(8), null));
            return true;
        }
        return false;
    }
}
