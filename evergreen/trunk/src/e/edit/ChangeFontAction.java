package e.edit;

import java.awt.*;
import java.awt.event.*;

/**
The ETextArea action to change to a proportional/fixed-width font.
*/
public class ChangeFontAction extends ETextAction {
    private boolean proportional;

    public ChangeFontAction(boolean proportional) {
        super("Use " + (proportional ? "Proportional" : "Fixed") + " Font");
        this.proportional = proportional;
    }

    public void actionPerformed(ActionEvent e) {
        ETextArea text = getTextArea();
        Font font = proportional ? ETextArea.getConfiguredFont() : ETextArea.getConfiguredFixedFont();
        text.setFont(font);
    }
}
