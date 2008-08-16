package e.edit;

import e.ptextarea.FileType;
import e.util.*;
import java.util.Set;

public class NumberResearcher implements WorkspaceResearcher {
    public String research(String string, ETextWindow textWindow) {
        try {
            NumberDecoder numberDecoder = new NumberDecoder(string);
            return numberDecoder.toHtml();
        } catch (Exception ex) {
            return "";
        }
    }
    
    /** Returns true, because numbers are everywhere. */
    public boolean isSuitable(FileType fileType) {
        return true;
    }
    
    /** We don't implement any non-standard URI schemes. */
    public boolean handleLink(String link) {
        return false;
    }
    
    /** Does nothing. */
    public void addWordsTo(Set<String> words) {
    }
}
