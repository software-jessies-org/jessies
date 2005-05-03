package e.util;

import java.awt.*;
import java.awt.event.*;
import java.awt.image.*;
import javax.swing.*;
import javax.swing.event.*;

public class GuiUtilities {
    static {
        System.setProperty("apple.laf.useScreenMenuBar", "true");
        System.setProperty("swing.aatext", "true");
        System.setProperty("swing.boldMetal", "false");
    }
    
    private GuiUtilities() { /* Not instantiable. */ }
    
    /**
     * An invisible cursor, useful if you want to hide the cursor when the
     * user is typing.
     */
    public static final Cursor INVISIBLE_CURSOR = Toolkit.getDefaultToolkit().createCustomCursor(new BufferedImage(1, 1, BufferedImage.TYPE_4BYTE_ABGR), new Point(0, 0), "invisible");
    
    /**
     * The background color for alternate rows in lists and tables.
     */
    public static final Color ALTERNATE_ROW_COLOR = new Color(0.92f, 0.95f, 0.99f);
    
    /**
     * The background color for selected rows in lists and tables.
     */
    public static final Color SELECTED_ROW_COLOR = new Color(0.24f, 0.50f, 0.87f);
    
    static {
        UIManager.put("List.selectionBackground", SELECTED_ROW_COLOR);
        UIManager.put("List.selectionForeground", Color.WHITE);
        UIManager.put("Table.selectionBackground", SELECTED_ROW_COLOR);
        UIManager.put("Table.selectionForeground", Color.WHITE);
    }
    
    // Used by isFontFixedWidth.
    private static final java.awt.font.FontRenderContext DEFAULT_FONT_RENDER_CONTEXT = new java.awt.font.FontRenderContext(null, false, false);
    
    /**
     * Guesses whether a font is fixed-width by comparing the widths of
     * various characters known for having wildly different sizes in
     * proportional fonts. As far as I know, there's no proper way to
     * query this font property in Java, despite how fundamental it
     * appears.
     */
    public static boolean isFontFixedWidth(Font font) {
        int maxWidth = 0;
        char[] testChars = "ILMWilmw01".toCharArray();
        for (int i = 0; i < testChars.length; i++) {
            java.awt.geom.Rectangle2D stringBounds = font.getStringBounds(testChars, i, i + 1, DEFAULT_FONT_RENDER_CONTEXT);
            int width = (int) Math.ceil(stringBounds.getWidth());
            if (maxWidth == 0) {
                maxWidth = width;
            } else {
                if (width != maxWidth) {
                    return false;
                }
            }
        }
        return true;
    }
    
    /**
     * Tests whether we're running on Mac OS. The Mac is quite
     * different from Linux and Windows, and it's sometimes
     * necessary to put in special-case behavior if you're running
     * on the Mac.
     */
    public static boolean isMacOs() {
        return (System.getProperty("os.name").indexOf("Mac") != -1);
    }
    
    /**
     * Tests whether we're running on Windows.
     */
    public static boolean isWindows() {
        return (System.getProperty("os.name").indexOf("Windows") != -1);
    }
    
    /**
     * Returns a KeyStroke suitable for passing to putValue(Action.ACCELERATOR_KEY) to
     * set up a keyboard equivalent for an Action.
     */
    public static KeyStroke makeKeyStroke(final String key, final boolean shifted) {
        return makeKeyStrokeForModifier(Toolkit.getDefaultToolkit().getMenuShortcutKeyMask(), key, shifted);
    }
    
    /**
     * A more general form of makeKeyStroke needed by Terminator on Linux to use Alt instead of Ctrl.
     */
    public static KeyStroke makeKeyStrokeForModifier(final int modifier, final String key, final boolean shifted) {
        int modifiers = modifier;
        if (shifted) modifiers |= InputEvent.SHIFT_MASK;
        String keycodeName = "VK_" + key;
        int keycode;
        try {
            keycode = KeyEvent.class.getField(keycodeName).getInt(KeyEvent.class);
            return KeyStroke.getKeyStroke(keycode, modifiers);
        } catch (Exception ex) {
            Log.warn("Couldn't find virtual keycode for '" + key + "'.", ex);
        }
        return null;
    }
    
    /**
     * Returns the name of the system's best monospaced font.
     */
    public static String getMonospacedFontName() {
        return isMacOs() ? "Monaco" : "Monospaced";
    }
    
    public static void initLookAndFeel() {
        try {
            String lafClassName = Parameters.getParameter("laf.className");
            if (lafClassName == null) {
                lafClassName = UIManager.getSystemLookAndFeelClassName();
            }
            if (lafClassName.indexOf("GTK") != -1) {
                lafClassName = UIManager.getCrossPlatformLookAndFeelClassName();
            }
            UIManager.setLookAndFeel(lafClassName);
            if (lafClassName.indexOf("Metal") != -1) {
                Object font = UIManager.get("Table.font");
                UIManager.put("Menu.font", font);
                UIManager.put("MenuItem.font", font);
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }
    
    /**
     * Scrolls so that the maximum value of the the scroll bar is visible,
     * even as the scroll bar's range changes, unless the user has manually
     * grabbed the thumb to look at some part of the history, in which case
     * we leave the scroll bar alone until it's next at the maximum value.
     */
    public static final void keepMaximumShowing(final JScrollBar scrollBar) {
        scrollBar.getModel().addChangeListener(new ChangeListener() {
            private BoundedRangeModel model = scrollBar.getModel();
            private boolean wasAtMaximum;
            private int maximum;
            private int extent;
            private int value;
            
            // If we had a decent name for this class, it would be in its own
            // file!
            {
                updateValues();
                wasAtMaximum = isAtMaximum();
            }
            
            private void updateValues() {
                maximum = model.getMaximum();
                extent = model.getExtent();
                value = model.getValue();
            }
            
            public void stateChanged(ChangeEvent event) {
                if (model.getMaximum() != maximum || model.getExtent() != extent) {
                    updateValues();
                    if (wasAtMaximum) {
                        scrollToBottom();
                    }
                    wasAtMaximum = isAtMaximum();
                } else if (model.getValue() != value) {
                    updateValues();
                    wasAtMaximum = isAtMaximum();
                } else {
                    updateValues();
                }
            }
            
            private void scrollToBottom() {
                model.setValue(maximum - extent);
            }
            
            private boolean isAtMaximum() {
                return (value + extent == maximum);
            }
        });
    }
}
