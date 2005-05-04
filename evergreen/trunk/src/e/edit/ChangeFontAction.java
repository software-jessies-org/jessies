package e.edit;

import java.awt.*;
import java.awt.event.*;

/**
The ETextArea action to change font.
*/
public abstract class ChangeFontAction extends ETextAction {
    public ChangeFontAction(String fontDescription) {
        super("Use " + fontDescription + " Font");
    }
    
    public abstract Font getFont();

    public void actionPerformed(ActionEvent e) {
        ETextArea text = getTextArea();
        text.setFont(getFont());
    }
}
