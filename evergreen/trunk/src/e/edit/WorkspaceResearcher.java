package e.edit;

public interface WorkspaceResearcher {
    /**
    * Returns an HTML string containing information about "string". Returns
    * the empty string (not null) if it has nothing to say. The string passed
    * to the researcher is the selection, if there is one, or the line up to the
    * caret otherwise.
    */
    public String research(String string);
    
    /**
    * Invoked before research -- if we're researching an ETextWindow -- to
    * give the Researcher an opportunity to decline. This lets a Java researcher,
    * for example, not tell you about Java's Array class when you're working on
    * a Ruby program.
    */
    public boolean isSuitable(ETextWindow textWindow);
    
    /**
     * Some researchers need to invent their own URI schemes such as "man:" or
     * "ri:"; this method is invoked when such a link is clicked on to see if
     * this researcher knows how to handle such links. Return true if you've
     * handled the link, and it won't be passed to further researchers.
     */
    public boolean handleLink(String link);
}
