package e.edit;

import e.ptextarea.FileType;
import java.util.Set;

public interface WorkspaceResearcher {
    /**
     * Returns an HTML string containing information about "string".
     * Returns the empty string (not null) if it has nothing to say.
     * wordAtCaret is the selection, if there is one, or the word at the caret otherwise.
     */
    public String research(String wordAtCaret);
    
    /**
     * Invoked before research to give the Researcher an opportunity to decline.
     * A Java researcher need not tell you about Java's Array class when you're working on Ruby.
     */
    public boolean isSuitable(FileType fileType);
    
    /**
     * Some researchers need to invent their own URI schemes such as "man:" or
     * "ri:"; this method is invoked when such a link is clicked on to see if
     * this researcher knows how to handle such links. Return true if you've
     * handled the link, and it won't be passed to further researchers.
     * 
     * This method is not invoked from the event dispatch thread.
     */
    public boolean handleLink(String link);
}
