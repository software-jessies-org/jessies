package e.edit;

import e.ptextarea.*;
import e.util.*;

public class NumberResearcher implements WorkspaceResearcher {
    public String research(String string) {
        try {
            NumberDecoder numberDecoder = new NumberDecoder(string);
            return numberDecoder.toHtml();
        } catch (Exception ex) {
            return "";
        }
    }
    
    /** Returns true, because numbers are everywhere. */
    public boolean isSuitable(ETextWindow textWindow) {
        return true;
    }
    
    /** We don't implement any non-standard URI schemes. */
    public boolean handleLink(String link) {
        return false;
    }
}
