package e.edit;

import java.util.*;
import e.util.*;

public class RubyDocumentationResearcher implements WorkspaceResearcher {
    /**
     * Look for something in a JTextComponent. Returns an HTML string
     * containing information about what it found. Should return
     * the empty string (not null) if it has nothing to say.
     */
    public String research(javax.swing.text.JTextComponent text, String string) {
        String ri = getRi();
        if (ri == null) {
            return "";
        }
        ArrayList lines = new ArrayList();
        ArrayList errors = new ArrayList();
        int status = ProcessUtilities.backQuote(null, new String[] { "ruby", ri, "-T", "-f", "html", string }, lines, errors);
        String result = StringUtilities.join(lines, "\n");
        return (result.indexOf("<error>") != -1) ? "" : result.toString();
    }
    
    private String getRi() {
        ArrayList availableRis = new ArrayList();
        ArrayList errors = new ArrayList();
        int status = ProcessUtilities.backQuote(null, new String[] { "which", "ri" }, availableRis, errors);
        if (status != 0 || availableRis.size() == 0) {
            return null;
        }
        return (String) availableRis.get(0);
    }
    
    /** Returns true for Ruby files. */
    public boolean isSuitable(ETextWindow textWindow) {
        return textWindow.isRuby();
    }
}
