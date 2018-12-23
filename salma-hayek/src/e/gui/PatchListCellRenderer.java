package e.gui;

import java.awt.*;
import javax.swing.*;

public class PatchListCellRenderer extends EListCellRenderer<String> {
    public static final PatchListCellRenderer INSTANCE = new PatchListCellRenderer();
    
    /**
     * Prevents the creation of useless instances.
     */
    private PatchListCellRenderer() {
        super(false);
    }
    
    /**
     * Renders lines from a context diff patch in colors inspired by code2html.
     */
    @Override public void doCustomization(JList<String> list, String line, int index, boolean isSelected, boolean isFocused) {
        if (isSelected) {
            // Leave the colors alone so you can still see when a +++, ---, or @@ line is selected.
        } else if (line.startsWith("+")) {
            setBackground(PatchDialog.LIGHT_GREEN);
            if (line.startsWith("+++ ")) {
                setBackground(PatchDialog.DARK_GREEN);
            }
        } else if (line.startsWith("-")) {
            setBackground(PatchDialog.LIGHT_RED);
            if (line.startsWith("--- ")) {
                setBackground(PatchDialog.DARK_RED);
            }
        } else if (line.startsWith("@@ ")) {
            setBackground(PatchDialog.VERY_LIGHT_GRAY);
        }
    }
}
