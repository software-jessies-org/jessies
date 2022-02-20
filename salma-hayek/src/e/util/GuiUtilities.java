package e.util;

import e.gui.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.image.*;
import java.lang.reflect.*;
import java.net.URI;
import java.util.*;
import java.util.concurrent.CountDownLatch;
import javax.swing.*;
import javax.swing.border.*;
import javax.swing.event.*;
import org.jessies.os.*;

public class GuiUtilities {
    /**
     * On FreeBSD and some other systems, Java's Desktop.getDesktop().browse() function doesn't work.
     * We have a work-around for this, by executing a user-provided command if the function fails.
     * The following is the help text we show in the error dialog, for when the user can use this
     * work-around, but has not done so yet.
     */
    private static final String WEB_BROWSER_HELP_TEXT = "Java's ability to open URLs on your platform is broken, sorry.\n\n" +
        "To work around this, please set the environment variable\n" +
        "'JESSIES_WEB_BROWSER' to the command you wish to run to open web URLs.\n" +
        "Then restart this application with the environment variable set, and the\n" +
        "specified command will be run to open URLs, with the URL as argument.\n\n" +
        "If you use bash, adding the following to your .bashrc file should work:\n" +
        "export JESSIES_WEB_BROWSER=firefox\n";
        
    static {
        try {
            invokeLater(() -> {
                e.debug.EventDispatchThreadHangMonitor.initMonitoring();
            });
        } catch (Throwable th) {
            // The above invokeLater call is where awt first gets initialized.
            // GUI invocation failures reported to stderr rather than the log
            // don't make it to the mailing list.
            Log.warn("Failed to install event dispatch thread hang monitor", th);
        }
    }
    
    /**
     * Prevents instantiation.
     */
    private GuiUtilities() {
    }
    
    // getMenuShortcutKeyMaskEx requires Java 10, so for macOS where the
    // difference between -Ex and non-Ex breaks shortcuts like C-left and C-up,
    // and which is the only "unusual" platform anyway, hard-code the -Ex value
    // that we need. (Reported by Tanel Poder.)
    // All of this can be replaced by just getMenuShortcutKeyMaskEx() >= Java 10.
    @SuppressWarnings("deprecation") // getMenuShortcutKeyMaskEx requires Java 10.
    private static final int defaultKeyStrokeModifier = GuiUtilities.isMacOs() ? InputEvent.META_DOWN_MASK : Toolkit.getDefaultToolkit().getMenuShortcutKeyMask();
    
    /**
     * An invisible cursor, useful if you want to hide the cursor when the
     * user is typing.
     */
    public static final Cursor INVISIBLE_CURSOR = Toolkit.getDefaultToolkit().createCustomCursor(new BufferedImage(1, 1, BufferedImage.TYPE_4BYTE_ABGR), new Point(0, 0), "invisible");
    
    private static final Color MAC_OS_ALTERNATE_ROW_COLOR = new Color(0.92f, 0.95f, 0.99f);
    
    // Used by isFontFixedWidth.
    private static final java.awt.font.FontRenderContext DEFAULT_FONT_RENDER_CONTEXT = new java.awt.font.FontRenderContext(null, false, false);
    
    private static final int COMPONENT_SPACING = calculateComponentSpacing();
    
    private static boolean mnemonicsEnabled = true;
    
    public static void nowOrLater(final Runnable runnable) {
        if (EventQueue.isDispatchThread()) {
            runnable.run();
        } else {
            invokeLater(runnable);
        }
    }
    
    public static void invokeLater(final Runnable runnable) {
        java.awt.EventQueue.invokeLater(() -> {
            try {
                runnable.run();
            } catch (Throwable th) {
                Log.warn("Failure in Swing invocation", th);
            }
        });
    }
    
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
        return OS.isMacOs();
    }
    
    /**
     * Tests whether we're running on Windows.
     */
    public static boolean isWindows() {
        return OS.isWindows();
    }
    
    /**
     * Tests whether we're using the GTK+ LAF (and so are probably on Linux or Solaris).
     */
    public static boolean isGtk() {
        return UIManager.getLookAndFeel().getClass().getName().contains("GTK");
    }
    
    /**
     * Returns an EmptyBorder of 'width' pixels on each side.
     */
    public static Border createEmptyBorder(int width) {
        return BorderFactory.createEmptyBorder(width, width, width, width);
    }
    
    public static int getDefaultKeyStrokeModifier() {
        return defaultKeyStrokeModifier;
    }
    
    /**
     * Translates a KeyStroke into the string that would be used in the UI to describe the keystroke.
     * Menu items do this for you, but for tool tips you're on your own.
     */
    public static String keyStrokeToString(KeyStroke keyStroke) {
        final StringBuilder result = new StringBuilder();
        final int modifiers = keyStroke.getModifiers();
        if (modifiers != 0) {
            result.append(KeyEvent.getModifiersExText(modifiers));
            if (!GuiUtilities.isMacOs()) {
                // GTK and Windows both insert a '+' between the modifiers and the key itself.
                result.append("+");
            }
        }
        final int keyCode = keyStroke.getKeyCode();
        if (keyCode != 0) {
            result.append(KeyEvent.getKeyText(keyCode));
        } else {
            result.append(keyStroke.getKeyChar());
        }
        return result.toString();
    }
    
    /**
     * Returns a KeyStroke suitable for passing to putValue(Action.ACCELERATOR_KEY) to set up a keyboard equivalent for an Action.
     * Assumes that you want the platform's default modifier for keyboard equivalents (but see also setDefaultKeyStrokeModifier).
     */
    public static KeyStroke makeKeyStroke(final String key, final boolean shifted) {
        return makeKeyStrokeWithModifiers(defaultKeyStrokeModifier | (shifted ? InputEvent.SHIFT_DOWN_MASK : 0), key);
    }
    
    /**
     * Returns a KeyStroke suitable for passing to putValue(Action.ACCELERATOR_KEY) to set up a keyboard equivalent for an Action.
     * Callers supply their own set of modifiers (see InputEvent).
     */
    public static KeyStroke makeKeyStrokeWithModifiers(final int modifiers, final String key) {
        String keycodeName = "VK_" + key;
        try {
            int keycode = KeyEvent.class.getField(keycodeName).getInt(KeyEvent.class);
            return KeyStroke.getKeyStroke(keycode, modifiers);
        } catch (Exception ex) {
            Log.warn("Couldn't find virtual keycode for \"" + key + "\".", ex);
        }
        return null;
    }
    
    /**
     * Configures 'action' to have the given name and keystroke.
     * To have no keyboard equivalent, use null for 'keystroke'.
     * Any underscores will be removed from 'name' and used to set the action's mnemonic index ("Cu_t", "_Copy", and "_Paste", for example).
     */
    public static void configureAction(AbstractAction action, String name, KeyStroke keystroke) {
        int underscoreIndex = name.indexOf('_');
        if (underscoreIndex != -1) {
            action.putValue(Action.NAME, name.replaceAll("_", ""));
            if (!GuiUtilities.isMacOs()) {
                action.putValue(Action.MNEMONIC_KEY, Integer.valueOf(Character.toUpperCase(name.charAt(underscoreIndex + 1))));
                action.putValue(Action.DISPLAYED_MNEMONIC_INDEX_KEY, underscoreIndex);
            }
        } else {
            action.putValue(Action.NAME, name);
        }
        if (keystroke != null) {
            action.putValue(Action.ACCELERATOR_KEY, keystroke);
        }
    }
    
    /**
     * Always call this instead of new JMenu.
     * FIXME: This doesn't use the same underscore scheme as configureAction.
     * configureAction's scheme can frustrate grep.
     */
    public static JMenu makeMenu(String name, char mnemonic) {
        JMenu menu = new JMenu(name);
        if (!GuiUtilities.isMacOs() && mnemonicsEnabled) {
            menu.setMnemonic(mnemonic);
        }
        return menu;
    }
    
    /**
     * Terminator uses alt as the modifier for its accelerators by default, so it needs a way to disable mnemonics.
     * Normal applications should have no reason to call this.
     */
    public static void setMnemonicsEnabled(boolean newState) {
        mnemonicsEnabled = newState;
    }
    
    public static boolean getMnemonicsEnabled() {
        return mnemonicsEnabled;
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
        // Although taking the best available from the ordered list "Monaco", "Lucida Sans Typewriter", "Lucida Console", "Monospaced" would cater for all cases, getAllFonts is expensive.
        if (GuiUtilities.isMacOs()) {
            // Starting with Mac OS 10.6 the preferred monospaced font is Menlo.
            // If it is available, we are either on 10.6 or it has been installed
            // on purpose.
            String menlo = "Menlo";
            if (isFontFamilyAvailable(menlo)) {
                return menlo;
            }
            // "Monaco" is the traditional monospaced font on Mac OS up to and
            // including Mac OS 10.5.
            // "Lucida Sans Typewriter" will be available too.
            // Any of these will cover as much of Unicode as "Monospaced" would.
            // "Monospaced" would use "Courier New", which is unacceptable.
            return "Monaco";
        } else if (GuiUtilities.isWindows()) {
            // If we're using a JDK, we'll have "Lucida Sans Typewriter" available.
            // Not so if we've got a default JRE installation: http://java.sun.com/javase/6/docs/technotes/guides/intl/font.html
            // It looks like Windows XP ships with "Lucida Console", a squat "funhouse mirror" variant: http://www.microsoft.com/typography/fonts/winxp.htm
            // FIXME: we should probably choose "Lucida Sans Typewriter" over "Lucida Console", but that over "Monospaced".
            String lucidaSansTypewriter = "Lucida Sans Typewriter";
            if (isFontFamilyAvailable(lucidaSansTypewriter)) {
                return lucidaSansTypewriter;
            }
            // On Windows, "Monospaced" uses "Courier New" for the latin range, so it's a choice of last resort.
            // Far-east Asian users might disagree, but they seem to be a minority of our users.
            return "Monospaced";
        } else {
            // We're on Linux or Solaris, where the logical font "Monospaced" uses "Lucida Sans Typewriter" for the latin range.
            // Specifying "Monospaced" has the advantage of additionally getting us fallbacks for the other ranges.
            // FIXME: the fix for Sun bug 6378099 in Java 7 (b33) breaks this by using "Courier New" for the latin range. Bug or feature?
            return "Monospaced";
        }
    }
    
    private static boolean isFontFamilyAvailable(String fontFamily) {
        Font[] fonts = GraphicsEnvironment.getLocalGraphicsEnvironment().getAllFonts();
        for (Font font : fonts) {
            if (font.getFamily().equals(fontFamily)) {
                return true;
            }
        }
        return false;
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
     * Returns the amount of spacing between components appropriate for the system.
     */
    public static int getComponentSpacing() {
        return COMPONENT_SPACING;
    }
    
    private static int calculateComponentSpacing() {
        if (GuiUtilities.isWindows()) {
            return 4;
        }
        if (GuiUtilities.isMacOs()) {
            return 10;
        }
        return 6;
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
            ProcessUtilities.spawn(null, FileUtilities.findSupportBinary("gnome-startup").toString(), "stop", DESKTOP_STARTUP_ID);
        }
    }
    
    public static void selectFileInFileViewer(String fullPathname) {
        // FIXME: add GNOME support, if possible. See https://launchpad.net/distros/ubuntu/+source/nautilus/+bug/57537
        // FIXME: make into an action that also supplies an appropriate name for the action on the current platform.
        if (GuiUtilities.isMacOs()) {
            ProcessUtilities.spawn(null, "/usr/bin/osascript", "-e", "tell application \"Finder\" to select \"" + fullPathname + "\" as POSIX file", "-e", "tell application \"Finder\" to activate");
        } else if (GuiUtilities.isWindows()) {
            // See "Windows Explorer Command-Line Options", http://support.microsoft.com/default.aspx?scid=kb;EN-US;q152457
            // doesn't work: $ explorer '/select,C:\Program Files'
            // does work: $ explorer '/select,C:\Program' 'Files'
            ProcessUtilities.spawn(null, ("Explorer /select," + fullPathname).split(" "));
        }
    }
    
    public static void openUrl(String url) {
        try {
            Desktop.getDesktop().browse(new URI(url));
        } catch (UnsupportedOperationException ex) {
            String openUrl = System.getenv("JESSIES_WEB_BROWSER");
            if (openUrl == null) {
                SimpleDialog.showDetails(null, "Cannot open URL " + url, WEB_BROWSER_HELP_TEXT);
                return;
            }
            ArrayList<String> lines = new ArrayList<>();
            if (ProcessUtilities.backQuote(null, new String[] {openUrl, url}, lines, lines) != 0) {
                SimpleDialog.showAlert(null, "JESSIES_WEB_BROWSER command " + openUrl + " failed:", StringUtilities.join(lines, "\n"));
            }
        } catch (Throwable th) {
            SimpleDialog.showDetails(null, "Problem opening URL", th);
        }
    }
    
    public static void initLookAndFeel() {
        try {
            // Work around Sun bug 6389282 which prevents Java 6 applications that would use the GTK LAF from displaying on remote X11 displays.
            UIManager.getInstalledLookAndFeels();
            
            // Use the system LAF by default, unless the user has set the swing.defaultlaf property (also understood by UIManager).
            // http://java.sun.com/docs/books/tutorial/uiswing/lookandfeel/plaf.html
            String lafClassName = System.getProperty("swing.defaultlaf");
            if (lafClassName == null){
                lafClassName = UIManager.getSystemLookAndFeelClassName();
            }
            
            // FIXME: when we move to 1.6, remove this completely. The GTK LAF is okay there.
            if (lafClassName.contains("GTK") && System.getProperty("java.specification.version").equals("1.5")) {
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
                int defaultRowHeightForGtk = 18;
                int rowHeight = (int)(getGtkTextScale() * defaultRowHeightForGtk + 0.5);
                //Log.warn("Setting Table.rowHeight to " + rowHeight);
                UIManager.put("Table.rowHeight", rowHeight);
                fixWmClass();
            }
            
            // Sneakily enable the HungAwtExit MBean for all our GUI applications.
            e.debug.HungAwtExit.initMBean();
        } catch (Exception ex) {
            Log.warn("Problem setting up GUI defaults.", ex);
        }
    }

    public static float getGtkTextScale() {
        // Fix https://bugs.openjdk.java.net/browse/JDK-8029087
        int defaultDpi = 96;
        int dpi = ((int) Toolkit.getDefaultToolkit().getDesktopProperty("gnome.Xft/DPI")) / 1024;
        return dpi * 1.0f / defaultDpi;
    }

    public static Font applyFontScaling(Font oldFont) {
        if (isGtk() == false) {
            return oldFont;
        }
        return oldFont.deriveFont(oldFont.getSize2D() * getGtkTextScale());
    }

    public static Dimension scaleDimensionForText(int width, int height) {
        float scale = isGtk() ? getGtkTextScale() : 1.0f;
        width = (int) (width * scale);
        height = (int) (height * scale);
        return new Dimension(width, height);
    }
    
    public static int scaleSizeForText(int size) {
        float scale = isGtk() ? getGtkTextScale() : 1.0f;
        size = (int) (size * scale);
        return size;
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
     * 
     * See https://bugs.openjdk.java.net/browse/JDK-6528430.
     */
    private static void fixWmClass() {
        try {
            Toolkit xToolkit = Toolkit.getDefaultToolkit();
            Field awtAppClassNameField = xToolkit.getClass().getDeclaredField("awtAppClassName");
            awtAppClassNameField.setAccessible(true);
            awtAppClassNameField.set(xToolkit, Log.getApplicationName());
        } catch (Throwable th) {
            Log.warn("Failed to fix WM_CLASS for " + Log.getApplicationName() + " windows.", th);
        }
    }
    
    public static void setTextAntiAliasing(Graphics2D g, boolean antiAlias) {
        if (!antiAlias) {
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_OFF);
            g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_OFF);
            return;
        }
        // Get the desktop rendering hints so that if the user's chosen anti-aliased text, we give them what they've configured.
        // This is necessary to get the user's exact configuration (such as which order the LCD pixels are in).
        Map<?, ?> map = (Map<?, ?>) (Toolkit.getDefaultToolkit().getDesktopProperty("awt.font.desktophints"));
        if (map != null) {
            g.addRenderingHints(map);
        }
        // If the user has no global anti-aliasing settings, go for the JVM's default anti-aliasing.
        // Note that VALUE_ANTIALIAS_DEFAULT basically means "off".
        Object hint = g.getRenderingHint(RenderingHints.KEY_ANTIALIASING);
        if (hint == RenderingHints.VALUE_ANTIALIAS_OFF || hint == RenderingHints.VALUE_ANTIALIAS_DEFAULT) {
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        }
    }
    
    /**
     * Sets the opacity (1.0 => fully opaque, 0.0 => fully transparent) of the given Frame.
     */
    public static void setFrameAlpha(JFrame frame, double alpha) {
        if (frame.isUndecorated() == false) {
            Log.warn("Sorry, Dave, decorated windows can't be made translucent according to");
            Log.warn("http://docs.oracle.com/javase/7/docs/api/java/awt/Window.html#setOpacity%28float%29.");
            Log.warn("Perhaps you'd like to override that but only for the duration of this call,");
            Log.warn("otherwise you won't be able to move, resize or close your window.");
            return;
        }
        try {
            // This is only available on Java 7 and later.
            Window.class.getMethod("setOpacity", float.class).invoke(frame, (float) alpha);
            return;
        } catch (NoSuchMethodException ex) {
            // We don't want to spam the log just because the system is running Java 6.
        } catch (Throwable th) {
            // Exceptions thrown by a method invoked via reflection are reported
            // as the "cause" of an InvocationTargetException, so check that...
            Throwable cause = th.getCause();
            if (cause != null && cause instanceof UnsupportedOperationException) {
                // We don't want to spam the log just because the system doesn't support transparency.
            } else {
                Log.warn("Window.setOpacity failed.", th);
            }
        }
    }
    
    private static Border makeEmptyBorderResource(int top, int left, int bottom, int right) {
        return new javax.swing.plaf.BorderUIResource.EmptyBorderUIResource(top, left, bottom, right);
    }
    
    /**
     * Scrolls so that the maximum value of the the scroll bar is visible, even as the scroll bar's range changes.
     * If the user manually grabs the thumb to look at some part of the history, we leave the scroll bar alone.
     * When the user returns the scroll bar to the maximum value, auto-scrolling will start again.
     * 
     * Returns the added ChangeListener so that the caller can hand it back to stopMaximumShowing to disable the effect.
     */
    public static final ChangeListener keepMaximumShowing(final JScrollBar scrollBar) {
        ChangeListener changeListener = new ChangeListener() {
            private BoundedRangeModel model = scrollBar.getModel();
            private boolean wasAtMaximum;
            private int maximum;
            private int extent;
            private int value;
            
            // If we had a decent name for this class, it would be in its own file!
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
        };
        scrollBar.getModel().addChangeListener(changeListener);
        return changeListener;
    }
    
    /**
     * See keepMaximumShowing.
     */
    public static final void stopMaximumShowing(final JScrollBar scrollBar, ChangeListener changeListener) {
        scrollBar.getModel().removeChangeListener(changeListener);
    }
    
    public static final void waitForWindowToDisappear(final Component window) {
        final CountDownLatch done = new CountDownLatch(1);
        // FIXME: is this really the easiest way to watch for the component being removed from the hierarchy?
        window.addHierarchyListener(new HierarchyListener() {
            public void hierarchyChanged(HierarchyEvent e) {
                if ((e.getChangeFlags() & HierarchyEvent.SHOWING_CHANGED) != 0 && window.isShowing() == false) {
                    done.countDown();
                }
            }
        });
        try {
            done.await();
        } catch (InterruptedException ex) {
            Log.warn("InterruptedException while waiting for window to be closed", ex);
        }
    }
    
    // The quitHandlerLambda should return true to quit, false to cancel the quit.
    public static void setQuitHandler(java.util.function.BooleanSupplier quitHandlerLambda) {
        try {
            // TODO: write this directly when we require Java >= 9.
            Class<?> handlerClass = Class.forName("java.awt.desktop.QuitHandler");
            Object proxy = Proxy.newProxyInstance(ClassLoader.getSystemClassLoader(),
                                                  new Class<?>[] { handlerClass },
                                                      (__1, method, args) -> {
                                                          if (method.getName().equals("handleQuitRequestWith")) {
                                                              boolean shouldQuit = quitHandlerLambda.getAsBoolean();
                                                              Class<?> c = Class.forName("java.awt.desktop.QuitResponse");
                                                              Method m = c.getDeclaredMethod(shouldQuit ? "performQuit" : "cancelQuit");
                                                              m.invoke(args[1]);
                                                          }
                                                          return Void.TYPE;
                                                      });
            Desktop.class.getDeclaredMethod("setQuitHandler", handlerClass).invoke(Desktop.getDesktop(), proxy);
        } catch (ReflectiveOperationException ignored) {
        }
    }
}
