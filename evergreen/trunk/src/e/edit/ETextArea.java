package e.edit;

import java.awt.*;
import java.util.regex.*;
import e.ptextarea.*;
import e.util.*;

/**
 * A text-editing component.
 */
public class ETextArea extends PTextArea {
    public ETextArea() {
        // FIXME: PTextArea doesn't handle this; should we, or should we just
        // force people to add a surrounding JPanel? Swing really made insets
        // and borders way more awkward than they should have been.
        setBorder(new javax.swing.border.EmptyBorder(4, 4, 4, 1));
    }
    
    public void setFont(Font font) {
        super.setFont(font);
        
        // FIXME
        //boolean fixedWidth = GuiUtilities.isFontFixedWidth(font);
        //setTabSize(fixedWidth ? 8 : 2);
    }
    
    public String reformatPastedText(String pastedText) {
        return pastedText.replace('\u00a0', ' ');
    }
}
