package e.gui;

import e.util.*;
import javax.swing.*;

/**
 * A simple status bar.
 * Doesn't suffer from JLabel's broken empty-string behavior.
 * Chooses a font suitable for a status bar, which may not be the same as the regular label font.
 */
public class EStatusBar extends ELabel {
    /**
     * Constructs a status bar with no initial message.
     */
    public EStatusBar() {
        // The default label fonts are a little large for status bars.
        if (System.getProperty("os.name").contains("Linux")) {
            setFont(UIManager.getFont("Menu.font"));
        } else if (GuiUtilities.isMacOs()) {
            setFont(UIManager.getFont("ToolTip.font"));
        }
    }
}
