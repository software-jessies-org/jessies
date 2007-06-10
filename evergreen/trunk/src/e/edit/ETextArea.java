package e.edit;

import e.ptextarea.*;
import javax.swing.*;

/**
 * A text-editing component.
 */
public class ETextArea extends PTextArea {
    public ETextArea() {
        // Disable the default find action so we can offer our own.
        getActionMap().remove(PActionFactory.makeFindAction().getValue(Action.NAME));
    }
    
    @Override
    public String reformatPastedText(String pastedText) {
        // Turn non-breakable spaces into normal spaces.
        // These are a problem if, for example, you paste a signature from JavaDoc.
        pastedText = pastedText.replace('\u00a0', ' ');
        // Turn old-style Mac line endings into something sensible.
        // These are a problem if, for example, you paste example code from Apple's on-line documentation.
        pastedText = pastedText.replaceAll("\r\n?", "\n");
        return pastedText;
    }
    
    @Override
    protected void updateFindResults() {
        ETextWindow textWindow = (ETextWindow) SwingUtilities.getAncestorOfClass(ETextWindow.class, this);
        FindAction.INSTANCE.repeatLastFind(textWindow);
    }
}
