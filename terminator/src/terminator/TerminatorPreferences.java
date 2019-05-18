package terminator;

import e.forms.*;
import e.util.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.*;
import java.awt.image.*;
import java.util.*;
import java.util.List;
import javax.swing.*;

public class TerminatorPreferences extends Preferences {
    public static final String BACKGROUND_COLOR = "background";
    public static final String CURSOR_COLOR = "cursorColor";
    public static final String FOREGROUND_COLOR = "foreground";
    public static final String SELECTION_COLOR = "selectionColor";
    
    public static final String ALPHA = "alpha";
    public static final String ALWAYS_SHOW_TABS = "alwaysShowTabs";
    public static final String ANTI_ALIAS = "antiAlias";
    public static final String BLINK_CURSOR = "cursorBlink";
    public static final String BLOCK_CURSOR = "blockCursor";
    public static final String FANCY_BELL = "fancyBell";
    public static final String FONT = "font";
    public static final String HIDE_MOUSE_WHEN_TYPING = "hideMouseWhenTyping";
    public static final String INITIAL_COLUMN_COUNT = "initialColumnCount";
    public static final String INITIAL_ROW_COUNT = "initialRowCount";
    public static final String PALETTE = "palette";
    public static final String SCROLL_ON_KEY_PRESS = "scrollKey";
    public static final String SCROLL_ON_TTY_OUTPUT = "scrollTtyOutput";
    public static final String VISUAL_BELL = "visualBell";
    
    /**
     * Experimental preference for acting on error links (from compilers).
     */
    public static final String ERROR_LINK_CMD = "errorLinkCommand";
    
    /**
     * Whether or not the alt key should be meta.
     * If true, you can't use alt as part of your system's input method.
     * If false, you can't comfortably use emacs(1).
     */
    public static final String USE_ALT_AS_META = "useAltAsMeta";
    
    /**
     * Whether or not to log everything to files in $HOME/.terminator/logs/.
     * Users may wish to disable this functionality for security reasons (eg company laptops).
     */
    public static final String LOG_TERMINAL_ACTIVITY = "logTerminalActivity";
    
    private static final Color CREAM = new Color(0xfefaea);
    private static final Color LIGHT_BLUE = new Color(0xb3d4ff);
    private static final Color NEAR_BLACK = new Color(0x181818);
    private static final Color NEAR_GREEN = new Color(0x72ff00);
    private static final Color NEAR_WHITE = new Color(0xeeeeee);
    private static final Color SELECTION_BLUE = new Color(0x1c2bff);
    private static final Color VERY_DARK_BLUE = new Color(0x000045);
    private static final Color DARK_GREY = new Color(0x2c2c2c);
    private static final Color LIGHT_GREY = new Color(0xdcdcdc);
    
    @Override protected String getPreferencesFilename() {
        return System.getProperty("org.jessies.terminator.optionsFile");
    }
    
    @Override protected void initPreferences() {
        setHelperForClass(Double.class, new AlphaHelper());
        setHelperForClass(Color[].class, new PaletteHelper());
        
        addTab("Behavior");
        addTab("Appearance");
        addTab("Presets");
        
        addPreference("Behavior", INITIAL_COLUMN_COUNT, Integer.valueOf(80), "New terminal width");
        addPreference("Behavior", INITIAL_ROW_COUNT, Integer.valueOf(24), "New terminal height");
        addPreference("Behavior", ALWAYS_SHOW_TABS, Boolean.FALSE, "Always show tab bar");
        addPreference("Behavior", SCROLL_ON_KEY_PRESS, Boolean.TRUE, "Scroll to bottom on key press");
        addPreference("Behavior", SCROLL_ON_TTY_OUTPUT, Boolean.FALSE, "Scroll to bottom on output");
        addPreference("Behavior", HIDE_MOUSE_WHEN_TYPING, Boolean.TRUE, "Hide mouse when typing");
        addPreference("Behavior", VISUAL_BELL, Boolean.TRUE, "Visual bell (as opposed to no bell)");
        addPreference("Behavior", USE_ALT_AS_META, Boolean.FALSE, "Use alt key as meta key (for Emacs)");
        addPreference("Behavior", LOG_TERMINAL_ACTIVITY, Boolean.TRUE, "Log terminal activity in $HOME/.terminator/logs/");
        addPreference("Behavior", ERROR_LINK_CMD, "", "Error link handling script");
        
        addPreference("Appearance", ANTI_ALIAS, Boolean.TRUE, "Anti-alias text");
        addPreference("Appearance", BLINK_CURSOR, Boolean.TRUE, "Blink cursor");
        addPreference("Appearance", BLOCK_CURSOR, Boolean.FALSE, "Use block cursor");
        addPreference("Appearance", FANCY_BELL, Boolean.TRUE, "High-quality rendering of the visual bell");
        addPreference("Appearance", ALPHA, Double.valueOf(1.0), "Terminal opacity");
        addPreference("Appearance", FONT, new Font(GuiUtilities.getMonospacedFontName(), Font.PLAIN, 12), "Font");
        addPreference("Appearance", PALETTE, Palettes.fromString("ANSI"), "Palette");
        
        // Defaults similar to most other modern terminals: black on white.
        addPreference("Appearance", BACKGROUND_COLOR, Color.WHITE, "Background");
        addPreference("Appearance", CURSOR_COLOR, Color.BLUE, "Cursor");
        addPreference("Appearance", FOREGROUND_COLOR, NEAR_BLACK, "Text foreground");
        addPreference("Appearance", SELECTION_COLOR, LIGHT_BLUE, "Selection background");
    }
    
    // Offer various preset color combinations.
    // Note that these get fossilized into the user's preferences; updating values here doesn't affect users who've already clicked the button.
    @Override protected void willAddRows(List<FormPanel> formPanels) {
        ArrayList<JButton> buttons = new ArrayList<>();
        buttons.add(makePresetButton("White on Blue", VERY_DARK_BLUE, NEAR_WHITE, Color.GREEN, SELECTION_BLUE));
        buttons.add(makePresetButton("Green on Black", Color.BLACK, NEAR_GREEN, Color.GREEN, SELECTION_BLUE));
        buttons.add(makePresetButton("White on Black", Color.BLACK, NEAR_WHITE, Color.GREEN, Color.DARK_GRAY));
        buttons.add(makePresetButton("Black on White", Color.WHITE, NEAR_BLACK, Color.BLUE, LIGHT_BLUE));
        buttons.add(makePresetButton("Black on Cream", CREAM, NEAR_BLACK, Color.RED, LIGHT_BLUE));
        buttons.add(makePresetButton("Dark Theme", DARK_GREY, LIGHT_GREY, Color.GREEN, SELECTION_BLUE));
        ComponentUtilities.tieButtonSizes(buttons);
        
        String description = "Presets:";
        for (JButton button : buttons) {
            formPanels.get(2).addRow(description, button);
            description = "";
        }
    }
    
    private JButton makePresetButton(String name, final Color background, final Color foreground, final Color cursor, final Color selection) {
        // FIXME: ideally, we'd update the button image when the user changes the anti-aliasing preference.
        JButton button = new JButton(new PresetIcon(name, background, foreground));
        button.putClientProperty("JButton.buttonType", "gradient"); // Mac OS 10.5
        button.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                put(BACKGROUND_COLOR, background);
                put(FOREGROUND_COLOR, foreground);
                put(CURSOR_COLOR, cursor);
                put(SELECTION_COLOR, selection);
            }
        });
        return button;
    }
    
    class PresetIcon implements Icon {
        private String name;
        private Color bg;
        private Color fg;
        private int height;
        private int width;
        
        public PresetIcon(String name, Color background, Color foreground) {
            this.name = name;
            this.bg = background;
            this.fg = foreground;
            
            // This seems ugly and awkward, but I can't think of a simpler way to get a suitably-sized BufferedImage to work with, without hard-coding dimensions.
            // We want all images to be the same width, so we use a constant string rather than the button text.
            BufferedImage image = new BufferedImage(1, 1, BufferedImage.TYPE_INT_RGB);
            Graphics2D g = image.createGraphics();
            g.setFont(getFont(TerminatorPreferences.FONT));
            FontMetrics metrics = g.getFontMetrics();
            this.height = (int) (1.4* metrics.getHeight());
            this.width = (int) (metrics.getStringBounds("XXXXXXXXXXXXXXXX", g).getWidth() * 1.2);
            g.dispose();
        }
        
        @Override public int getIconWidth() {
            return width;
        }
        
        @Override public int getIconHeight() {
            return height;
        }
        
        @Override public void paintIcon(Component c, Graphics og, int ox, int oy) {
            Graphics2D g = (Graphics2D) og;
            GuiUtilities.setTextAntiAliasing(g, Terminator.getPreferences().getBoolean(TerminatorPreferences.ANTI_ALIAS));
            g.setFont(getFont(TerminatorPreferences.FONT));
            g.setColor(bg);
            g.fillRect(ox, oy, getIconWidth(), getIconHeight());
            g.setColor(bg.darker());
            g.drawRect(ox, oy, getIconWidth() - 1, getIconHeight() - 1);
            g.setColor(fg);
            Rectangle2D stringBounds = g.getFontMetrics().getStringBounds(name, g);
            final int x = (getIconWidth() - (int) stringBounds.getWidth()) / 2;
            int y = (getIconHeight() - (int) stringBounds.getHeight()) / 2 + g.getFontMetrics().getAscent();
            y = (int) (y / 1.1);
            g.drawString(name, ox + x, oy + y);
        }
    }

    public double getDouble(String key) {
        return (Double) get(key);
    }
    
    private class AlphaHelper implements PreferencesHelper {
        public String encode(String key) {
            return Double.toString(getDouble(key));
        }
        
        public Object decode(String valueString) {
            return Double.valueOf(valueString);
        }
        
        public void addRow(FormPanel formPanel, final String key, final String description) {
            final JSlider slider = new JSlider(0, 255);
            slider.setValue((int) (slider.getMaximum() * getDouble(key)));
            slider.addChangeListener(new javax.swing.event.ChangeListener() {
                public void stateChanged(javax.swing.event.ChangeEvent e) {
                    put(key, ((double) slider.getValue())/slider.getMaximum());
                }
            });
            formPanel.addRow(description + ":", slider);
        }
    }
    
    private class PaletteHelper implements PreferencesHelper {
        public String encode(String key) {
            return Palettes.toString((Color[]) get(key));
        }
        
        public Object decode(String valueString) {
            return Palettes.fromString(valueString);
        }
        
        public void addRow(FormPanel formPanel, final String key, final String description) {
            final JComboBox<String> choices = new JComboBox<>();
            for (String paletteName : Palettes.names()) {
                choices.addItem(paletteName);
            }
            choices.setSelectedItem(encode(key));
            choices.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    put(key, decode((String) choices.getSelectedItem()));
                }
            });
            formPanel.addRow(description + ":", choices);
        }
    }
}
