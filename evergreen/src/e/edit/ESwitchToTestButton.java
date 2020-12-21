package e.edit;

import e.util.*;
import java.awt.*;
import java.awt.event.*;

public class ESwitchToTestButton extends EButton implements ActionListener {
    private ETextWindow window;
    
    public ESwitchToTestButton(ETextWindow window) {
        this.window = window;
        
        final String keyboardEquivalent = GuiUtilities.keyStrokeToString(ShowTestFileAction.KEYSTROKE);
        setToolTipText("Switch to \"" + window.getTestFilename() + "\" (" + keyboardEquivalent + ")");
    }
    
    public void paintComponent(Graphics g) {
        g.setColor(getGlyphColor());
        paintGlyph(g, 5, 5);
    }
    
    public void paintGlyph(Graphics g, int x, int y) {
        final int d = GuiUtilities.scaleSizeForText(2);
        
        g.fillRect(x, y, 5*d, d);
        g.fillRect(x + 2*d, y + d, d, 4 * d);
    }
    
    public void actionPerformed(ActionEvent e) {
        window.switchToTestFile();
    }
}
