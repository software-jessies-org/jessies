package e.edit;

import java.awt.*;
import java.io.*;
import javax.swing.text.html.*;

import e.util.*;

public class RubyDocumentationResearcher implements WorkspaceResearcher {
    /**
     * Look for something in a JTextComponent. Returns an HTML string
     * containing information about what it found. Should return
     * the empty string (not null) if it has nothing to say.
     */
    public String research(javax.swing.text.JTextComponent text, String string) {
        String[] availableRis = ProcessUtilities.backQuote(new String[] { "which", "ri" });
        if (availableRis == null) {
            return "";
        }
        
        String ri = availableRis[0];
        String[] output = ProcessUtilities.backQuote(new String[] { "ruby", ri, "-T", "-f", "html", string });
        String result = StringUtilities.join(output, "\n");
        return (result.indexOf("<error>") != -1) ? "" : result.toString();
    }
    
    /** Returns true for Ruby files. */
    public boolean isSuitable(ETextWindow textWindow) {
        return textWindow.isRuby();
    }
}
