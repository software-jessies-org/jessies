package e.util;

import java.awt.*;
import java.awt.event.*;
import java.awt.image.*;
import java.lang.reflect.*;
import java.util.*;
import javax.swing.*;
import javax.swing.event.*;

public class GuiUtilities {
    static {
        e.debug.EventDispatchThreadHangMonitor.initMonitoring();
    }
    
    /**
     * Prevents instantiation.
     */
    private GuiUtilities() {
    }
    
    private static int defaultKeyStrokeModifier = Toolkit.getDefaultToolkit().getMenuShortcutKeyMask();
    
    /**
     * An invisible cursor, useful if you want to hide the cursor when the
     * user is typing.
     */
    public static final Cursor INVISIBLE_CURSOR = Toolkit.getDefaultToolkit().createCustomCursor(new BufferedImage(1, 1, BufferedImage.TYPE_4BYTE_ABGR), new Point(0, 0), "invisible");
    
    private static final Color MAC_OS_ALTERNATE_ROW_COLOR = new Color(0.92f, 0.95f, 0.99f);
    
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
        for (int i = 0; i < testChars.length; ++i) {
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
        return System.getProperty("os.name").contains("Mac");
    }
    
    /**
     * Tests whether we're running on Windows.
     */
    public static boolean isWindows() {
        return System.getProperty("os.name").contains("Windows");
    }
    
    /**
     * Tests whether we're using the GTK+ LAF (and so are probably on Linux or Solaris).
     */
    public static boolean isGtk() {
        return UIManager.getLookAndFeel().getClass().getName().contains("GTK");
    }
    
    /**
     * Used to override the system's default keyboard equivalent modifier.
     * Useful in a terminal emulator on a platform where the default is Control (and probably nowhere else).
     */
    public static void setDefaultKeyStrokeModifier(int modifier) {
        defaultKeyStrokeModifier = modifier;
    }
    
    public static int getDefaultKeyStrokeModifier() {
        return defaultKeyStrokeModifier;
    }
    
    /**
     * Returns a KeyStroke suitable for passing to putValue(Action.ACCELERATOR_KEY) to
     * set up a keyboard equivalent for an Action.
     */
    public static KeyStroke makeKeyStroke(final String key, final boolean shifted) {
        return makeKeyStrokeForModifier(defaultKeyStrokeModifier, key, shifted);
    }
    
    private static KeyStroke makeKeyStrokeForModifier(final int modifier, final String key, final boolean shifted) {
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
    public synchronized static String getMonospacedFontName() {
        if (monospacedFontName == null) {
            monospacedFontName = findMonospacedFontName();
        }
        return monospacedFontName;
    }
    
    private static String monospacedFontName;
    
    private static String findMonospacedFontName() {
        if (GuiUtilities.isMacOs()) {
            // "Monaco" is the traditional monospaced font on Mac OS.
            // "Lucida Sans Typewriter" will be available too.
            // Either will cover as much of Unicode as "Monospaced" would.
            // "Monospaced" would use "Courier New", which is unacceptable.
            return "Monaco";
        } else if (GuiUtilities.isWindows()) {
            // If we're using a JDK, we'll have "Lucida Sans Typewriter" available.
            // Not so if we've got a default JRE installation: http://java.sun.com/javase/6/docs/technotes/guides/intl/font.html
            String lucidaSansTypewriter = "Lucida Sans Typewriter";
            Font[] fonts = GraphicsEnvironment.getLocalGraphicsEnvironment().getAllFonts();
            for (Font font : fonts) {
                if (font.getFamily().equals(lucidaSansTypewriter)) {
                    return lucidaSansTypewriter;
                }
            }
            // On Windows, "Monospaced" uses "Courier New" for the latin range, so it's a choice of last resort.
            // Far-east Asian users might disagree, but they seem to be a minority of our users.
            return "Monospaced";
        } else {
            // We're on Linux or Solaris, where the logical font "Monospaced" uses "Lucida Sans Typewriter" for the latin range.
            // Specifying "Monospaced" has the advantage of additionally getting us fallbacks for the other ranges.
            return "Monospaced";
        }
    }
    
    /**
     * Returns the index into 'chars' of the character at offset 'x' when the
     * characters are rendered from 'startX' with font metrics 'metrics'. Note
     * that there's no font rendering context, so this won't account for
     * anti-aliasing et cetera.
     * 
     * Useful for hit-testing.
     * 
     * Note: this is broken for combining characters. See the Thai example
     * in Markus Kuhn's "UTF-8-demo.txt".
     */
    public static int getCharOffset(FontMetrics metrics, int startX, int x, char[] chars) {
        int min = 0;
        int max = chars.length;
        while (max - min > 1) {
            int mid = (min + max) / 2;
            int width = metrics.charsWidth(chars, 0, mid);
            if (width > x - startX) {
                max = mid;
            } else {
                min = mid;
            }
        }
        int charPixelOffset = x - startX - metrics.charsWidth(chars, 0, min);
        if (charPixelOffset > metrics.charWidth(chars[min]) / 2) {
            ++min;
        }
        return min;
    }
    
    /**
     * Returns the appropriate background color for the given row index.
     */
    public static Color backgroundColorForRow(int row) {
        if (isGtk()) {
            return (row % 2 == 0) ? Color.WHITE : UIManager.getColor("Table.background");
        } else if (isMacOs()) {
            return (row % 2 == 0) ? Color.WHITE : MAC_OS_ALTERNATE_ROW_COLOR;
        }
        return UIManager.getColor("Table.background");
    }
    
    public static void finishGnomeStartup() {
        String DESKTOP_STARTUP_ID = System.getProperty("gnome.DESKTOP_STARTUP_ID");
        if (DESKTOP_STARTUP_ID != null) {
            System.clearProperty("gnome.DESKTOP_STARTUP_ID");
            ProcessUtilities.spawn(null, new String[] { "gnome-startup", "stop", DESKTOP_STARTUP_ID });
        }
    }
    
    public static void selectFileInFileViewer(String fullPathname) {
        // FIXME: add GNOME support, if possible. See https://launchpad.net/distros/ubuntu/+source/nautilus/+bug/57537
        // FIXME: make into an action that also supplies an appropriate name for the action on the current platform.
        if (GuiUtilities.isMacOs()) {
            ProcessUtilities.spawn(null, new String[] { "/usr/bin/osascript", "-e", "tell application \"Finder\" to select \"" + fullPathname + "\" as POSIX file", "-e", "tell application \"Finder\" to activate" });
        } else if (GuiUtilities.isWindows()) {
            // See "Windows Explorer Command-Line Options", http://support.microsoft.com/default.aspx?scid=kb;EN-US;q152457
            ProcessUtilities.spawn(null, new String[] { "Explorer", "/select," + fullPathname });
        }
    }
    
    public static void initLookAndFeel() {
        try {
            // Work around Sun bug 6389282 which prevents Java 6 applications that would use the GTK LAF from displaying on remote X11 displays.
            UIManager.getInstalledLookAndFeels();
            
            String lafClassName = UIManager.getSystemLookAndFeelClassName();
            
            // FIXME: when we move to 1.6, remove this completely. The GTK LAF is okay there.
            if (lafClassName.contains("GTK") && System.getProperty("java.vm.version").startsWith("1.5.")) {
                lafClassName = UIManager.getCrossPlatformLookAndFeelClassName();
            }
            
            UIManager.setLookAndFeel(lafClassName);
            
            // Tweak Sun's "Metal" cross-platform LAF.
            if (lafClassName.contains("Metal")) {
                Object font = UIManager.get("Table.font");
                UIManager.put("Menu.font", font);
                UIManager.put("MenuItem.font", font);
            }
            
            // Tweak Apple's "Aqua" Mac OS LAF.
            if (lafClassName.contains("Aqua")) {
                // Apple's UI delegate has over-tight borders. (Apple 4417784.) Work-around by Werner Randelshofer.
                UIManager.put("OptionPane.border", makeEmptyBorderResource(15-3, 24-3, 20-3, 24-3));
                UIManager.put("OptionPane.messageAreaBorder", makeEmptyBorderResource(0, 0, 0, 0));
                UIManager.put("OptionPane.buttonAreaBorder", makeEmptyBorderResource(16-3, 0, 0, 0));
                // On Mac OS, standard tabbed panes use way too much space. This makes them slightly less greedy.
                UIManager.put("TabbedPane.useSmallLayout", Boolean.TRUE);
                // Apple's LAF uses the wrong background color for selected rows in lists and tables.
                Color MAC_OS_SELECTED_ROW_COLOR = new Color(0.24f, 0.50f, 0.87f);
                UIManager.put("List.selectionBackground", MAC_OS_SELECTED_ROW_COLOR);
                UIManager.put("List.selectionForeground", Color.WHITE);
                UIManager.put("Table.selectionBackground", MAC_OS_SELECTED_ROW_COLOR);
                UIManager.put("Table.selectionForeground", Color.WHITE);
            }
            
            if (lafClassName.contains("GTK")) {
                // We try to imitate the modern alternating table background effect, but get no help from the GTK LAF as yet.
                // In the meantime, this actually corresponds to Ubuntu's "Human" theme.
                // If we get complaints from users of other themes, we might want to try to find the closest color already known to UIManager.
                UIManager.put("Table.background", new Color(0xf5f2ed));
                fixWmClass();
            }
        } catch (Exception ex) {
            Log.warn("Problem setting up GUI defaults.", ex);
        }
    }
    
    /**
     * Overrides AWT's default guess of what to use as our windows' WM_CLASS.
     * 
     * AWT's XToolkit guesses a WM_CLASS for us based on the bottom-most class name in the stack trace of the thread that causes its construction.
     * For those of our application that launch from e.util.Launcher, that means they all get the WM_CLASS "e-util-Launcher".
     * Even those that don't, get a fully-qualified name such as "e-tools-FatBits" or "terminator-Terminator".
     * These names aren't usually very important unless you're doing some ugly application-specific hacking in your window manager.
     * Sadly, though, they show through in certain cases:
     * 1. When space gets too tight for GNOME's panel to have an icon for each window, it starts collapsing them by application, and uses WM_CLASS as the application name.
     * 2.If you use the GNOME/the Java Desktop System's Alt-PrtScr screenshot tool, its default filename is "Screenshot-<WM_CLASS>".
     * There are probably more examples, but these are enough to warrant a solution.
     * Given that we know what our application calls itself, we can use reflection to override AWT's default guess.
     */
    private static void fixWmClass() {
        try {
            Toolkit xToolkit = Toolkit.getDefaultToolkit();
            java.lang.reflect.Field awtAppClassNameField = xToolkit.getClass().getDeclaredField("awtAppClassName");
            awtAppClassNameField.setAccessible(true);
            awtAppClassNameField.set(xToolkit, Log.getApplicationName());
        } catch (Throwable th) {
            Log.warn("Failed to fix WM_CLASS for " + Log.getApplicationName() + " windows.", th);
        }
    }
    
    /**
     * Sets the opacity (1.0 => fully opaque, 0.0 => fully transparent) of the given Frame.
     * http://elliotth.blogspot.com/2007/08/transparent-java-windows-on-x11.html
     */
    public static void setFrameAlpha(Frame frame, double alpha) {
        try {
            Field peerField = Component.class.getDeclaredField("peer");
            peerField.setAccessible(true);
            Object peer = peerField.get(frame);
            if (peer == null) {
                return;
            }
            
            if (isMacOs()) {
                Class<?> cWindowClass = Class.forName("apple.awt.CWindow");
                if (cWindowClass.isInstance(peer)) {
                    // ((apple.awt.CWindow) peer).setAlpha(alpha);
                    Method setAlphaMethod = cWindowClass.getMethod("setAlpha", float.class);
                    setAlphaMethod.invoke(peer, (float) alpha);
                }
            } else if (isWindows()) {
                // FIXME: can we do this on Windows?
            } else {
                // long windowId = peer.getWindow();
                Class<?> xWindowPeerClass = Class.forName("sun.awt.X11.XWindowPeer");
                Method getWindowMethod = xWindowPeerClass.getMethod("getWindow", new Class[0]);
                long windowId = ((Long) getWindowMethod.invoke(peer, new Object[0])).longValue();
                
                long value = (int) (0xff * alpha) << 24;
                // sun.awt.X11.XAtom.get("_NET_WM_WINDOW_OPACITY").setCard32Property(windowId, value);
                Class<?> xAtomClass = Class.forName("sun.awt.X11.XAtom");
                Method getMethod = xAtomClass.getMethod("get", String.class);
                Method setCard32PropertyMethod = xAtomClass.getMethod("setCard32Property", long.class, long.class);
                setCard32PropertyMethod.invoke(getMethod.invoke(null, "_NET_WM_WINDOW_OPACITY"), windowId, value);
            }
        } catch (Exception ex) {
            // Boo hoo! No transparency for you!
            ex.printStackTrace();
            return;
        }
    }
    
    private static javax.swing.border.Border makeEmptyBorderResource(int top, int left, int bottom, int right) {
        return new javax.swing.plaf.BorderUIResource.EmptyBorderUIResource(top, left, bottom, right);
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
