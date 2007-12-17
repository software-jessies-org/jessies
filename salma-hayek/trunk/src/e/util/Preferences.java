package e.util;

import e.forms.*;
import e.gui.*;
import java.awt.*;
import java.awt.event.*;
import java.util.*;
import java.util.List;
import java.util.regex.*;
import javax.swing.*;

/**
 * Manages application preferences.
 * 
 * Subclass this and add a "public static final String" field for each preference key.
 * Implement a "initPreferences" method that calls addPreference for each key, giving a default value and a description for the preferences dialog.
 * Preferences appear in the dialog in the order they're added.
 * Call addSeparator to leave a gap between preferences in the UI.
 * 
 * Call setCustomUiForKey if you need a custom UI component.
 * It may be best to construct your Preferences subclass, then initialize your UI, and then call setCustomUiForKey (which is why it's public rather than protected).
 * 
 * Call showPreferencesDialog for a preferences dialog.
 * Use addPreferencesListener if you need to be called back when a preference is changed.
 * 
 * @author Elliott Hughes
 */
public abstract class Preferences {
    private static final Pattern RESOURCE_PATTERN = Pattern.compile("(?:\\S+(?:\\*|\\.))?(\\S+):\\s*(.+)");
    
    // Mutable at any time.
    private HashMap<String, Object> preferences = new HashMap<String, Object>();
    // Immutable after initialization.
    private HashMap<String, Object> defaults = new HashMap<String, Object>();
    
    private HashMap<String, String> descriptions = new HashMap<String, String>();
    private ArrayList<String> keysInUiOrder = new ArrayList<String>();
    
    private HashMap<Class<?>, PreferencesHelper> helpers = new HashMap<Class<?>, PreferencesHelper>();
    private HashMap<String, JComponent> customUis = new HashMap<String, JComponent>();
    
    private ArrayList<Listener> listeners = new ArrayList<Listener>();
    
    // Non-null if the preferences dialog is currently showing.
    private FormBuilder form;
    
    public Preferences() {
        initHelpers();
        initPreferences();
    }
    
    protected abstract void initPreferences();
    
    public interface Listener {
        public void preferencesChanged();
    }
    
    public void addPreferencesListener(Listener l) {
        listeners.add(l);
    }
    
    public void removePreferencesListener(Listener l) {
        listeners.remove(l);
    }
    
    private void firePreferencesChanged() {
        for (Listener l : listeners) {
            l.preferencesChanged();
        }
    }
    
    protected void addPreference(String key, Object value, String description) {
        put(key, value);
        defaults.put(key, value);
        descriptions.put(key, description);
        keysInUiOrder.add(key);
    }
    
    protected void addSeparator() {
        keysInUiOrder.add(null);
    }
    
    public void setCustomUiForKey(String key, JComponent ui) {
        customUis.put(key, ui);
    }
    
    public void put(String key, Object value) {
        // No-one really wants a UIResource; they want the Color or Font.
        // Rather than force the caller to convert or face run-time type errors, we do the conversion here.
        if (value instanceof javax.swing.plaf.ColorUIResource) {
            Color color = (Color) value;
            value = new Color(color.getRGB());
        } else if (value instanceof javax.swing.plaf.FontUIResource) {
            Font font = (Font) value;
            value = new Font(font.getFamily(), font.getStyle(), font.getSize());
        }
        
        Object oldValue = preferences.get(key);
        if (oldValue != null && oldValue.getClass().isAssignableFrom(value.getClass()) == false) {
            throw new IllegalArgumentException("attempt to change value for key \"" + key + "\" from instance of " + oldValue.getClass() + " to " + value.getClass());
        }
        preferences.put(key, value);
        firePreferencesChanged();
    }
    
    public Object get(String key) {
        return preferences.get(key);
    }
    
    public String getString(String key) {
        return (String) preferences.get(key);
    }
    
    public boolean getBoolean(String key) {
        return (Boolean) preferences.get(key);
    }
    
    public Color getColor(String key) {
        return (Color) preferences.get(key);
    }
    
    public Font getFont(String key) {
        return (Font) preferences.get(key);
    }
    
    public int getInt(String key) {
        return (Integer) preferences.get(key);
    }
    
    public void showPreferencesDialog(final Frame parent, final String filename) {
        // We can't keep reusing a form that we create just once, because you can't change the owner of an existing JDialog.
        // But we don't want to pop up another dialog if one's already up, so defer to the last one if it's still up.
        if (form != null) {
            for (FormPanel panel : form.getFormPanels()) {
                if (panel.isShowing()) {
                    ((JDialog) SwingUtilities.getAncestorOfClass(JDialog.class, panel)).toFront();
                    return;
                }
            }
        }
        
        // FIXME: this isn't very general. what if we want a bunch of related (non-color) configuration on its own tab?
        boolean hasColors = false;
        for (String key : keysInUiOrder) {
            if (key != null && preferences.get(key) instanceof Color) {
                hasColors = true;
                break;
            }
        }
        
        if (hasColors) {
            form = new FormBuilder(parent, "Preferences", new String[] { "General", "Colors" });
        } else {
            form = new FormBuilder(parent, "Preferences");
        }
        
        List<FormPanel> formPanels = form.getFormPanels();
        FormPanel generalPanel = formPanels.get(0);
        FormPanel colorsPanel = hasColors ? formPanels.get(1) : null;
        Map<String, ColorHelper> colorPreferences = new HashMap<String, ColorHelper>();
        
        // FIXME: need some general way to add color scheme support so people don't have to mess about with individual colors.
        /*
        // Offer various preset color combinations.
        // Note that these get fossilized into the user's preferences; updating values here doesn't affect users who've already clicked the button.
        colorsPanel.addRow("Presets:", makePresetButton(colorPreferences, "  Terminator  ", VERY_DARK_BLUE, NEAR_WHITE, Color.GREEN, SELECTION_BLUE));
        colorsPanel.addRow("", makePresetButton(colorPreferences, "Black on White", Color.WHITE, NEAR_BLACK, Color.BLUE, LIGHT_BLUE));
        colorsPanel.addRow("", makePresetButton(colorPreferences, "Green on Black", Color.BLACK, NEAR_GREEN, Color.GREEN, SELECTION_BLUE));
        colorsPanel.addRow("", makePresetButton(colorPreferences, "White on Black", Color.BLACK, NEAR_WHITE, Color.GREEN, Color.DARK_GRAY));
        */
        
        for (String key : keysInUiOrder) {
            if (key == null) {
                // FIXME: what if we're currently in the color keys?
                generalPanel.addEmptyRow();
                continue;
            }
            String description = descriptions.get(key);
            if (description != null) {
                Object value = preferences.get(key);
                if (customUis.get(key) != null) {
                    generalPanel.addRow(description + ":", customUis.get(key));
                } else {
                    helperForKey(key).addRow(formPanels, key, description);
                }
            }
        }
        
        // Save the preferences if the user hits "Save".
        form.getFormDialog().setAcceptCallable(new java.util.concurrent.Callable<Boolean>() {
            public Boolean call() {
                boolean saved = writeToDisk(filename);
                if (saved == false) {
                    SimpleDialog.showAlert(parent, "Couldn't save preferences.", "There was a problem writing preferences to \"" + filename + "\".");
                } else {
                    form = null;
                }
                return saved;
            }
        });
        
        // Restore the preferences if the user hits "Cancel".
        final HashMap<String, Object> initialPreferences = new HashMap<String, Object>(preferences);
        form.getFormDialog().setCancelRunnable(new Runnable() {
            public void run() {
                preferences = initialPreferences;
                firePreferencesChanged();
            }
        });
        
        form.getFormDialog().setRememberBounds(false);
        form.show("Save");
    }
    
    public void readFromDisk(String filename) {
        if (FileUtilities.exists(filename) == false) {
            return;
        }
        try {
            String data = StringUtilities.readFile(filename);
            if (data.startsWith("<?xml ")) {
                processXmlString(data);
            } else {
                processResourceLines(data.split("\n"));
            }
        } catch (Exception ex) {
            Log.warn("Problem reading preferences from \"" + filename + "\"", ex);
        }
    }
    
    // Process the current XML style of preferences written by writeToDisk.
    private void processXmlString(String data) throws Exception {
        org.w3c.dom.Document document = XmlUtilities.readXmlFromString(data);
        org.w3c.dom.Element root = document.getDocumentElement();
        for (org.w3c.dom.Node node = root.getFirstChild(); node != null; node = node.getNextSibling()) {
            if (node.getNodeType() == org.w3c.dom.Node.ELEMENT_NODE) {
                String key = node.getAttributes().getNamedItem("key").getNodeValue();
                String value = node.getTextContent();
                decodePreference(key, value);
            }
        }
    }
    
    // Process the legacy X11 resources style of preferences (used in old versions of Terminator).
    private void processResourceLines(String[] lines) {
        for (String line : lines) {
            line = line.trim();
            if (line.length() == 0 || line.startsWith("#")) {
                continue;
            }
            Matcher matcher = RESOURCE_PATTERN.matcher(line);
            if (matcher.find()) {
                String key = matcher.group(1);
                String valueString = matcher.group(2);
                decodePreference(key, valueString);
            }
        }
    }
    
    private void decodePreference(String key, String valueString) {
        PreferencesHelper helper = helperForKey(key);
        if (helper != null) {
            preferences.put(key, helper.decode(valueString));
        } else {
            Log.warn("No PreferencesHelper for key \"" + key + "\" with encoded value \"" + valueString + "\"");
        }
    }
    
    public boolean writeToDisk(String filename) {
        try {
            org.w3c.dom.Document document = XmlUtilities.makeEmptyDocument();
            org.w3c.dom.Element root = document.createElement("preferences");
            document.appendChild(root);
            
            for (String key : keysInUiOrder) {
                if (key == null) {
                    continue;
                }
                // Only write out non-default settings.
                // That way we can change defaults for things the user doesn't care about without having them using fossilized values.
                if (preferences.get(key).equals(defaults.get(key)) == false) {
                    String description = descriptions.get(key);
                    if (description != null) {
                        root.appendChild(document.createComment(description));
                    }
                    org.w3c.dom.Element settingElement = document.createElement("setting");
                    settingElement.setAttribute("key", key);
                    settingElement.setTextContent(helperForKey(key).encode(key));
                    root.appendChild(settingElement);
                }
            }
            XmlUtilities.writeXmlToDisk(filename, document);
            return true;
        } catch (Exception ex) {
            Log.warn("Problem writing preferences to \"" + filename + "\"", ex);
            return false;
        }
    }
    
    private PreferencesHelper helperForKey(String key) {
        Object currentValue = preferences.get(key);
        return (currentValue == null) ? null : helpers.get(currentValue.getClass());
    }
    
    // This interface isn't generic because we only have raw Object values in the HashMap.
    public interface PreferencesHelper {
        public String encode(String key);
        public Object decode(String valueString);
        public void addRow(List<FormPanel> formPanels, String key, final String description);
    }
    
    protected void setHelperForClass(Class<?> c, PreferencesHelper helper) {
        helpers.put(c, helper);
    }
    
    private void initHelpers() {
        setHelperForClass(Boolean.class, new BooleanHelper());
        setHelperForClass(Color.class, new ColorHelper());
        setHelperForClass(Font.class, new FontHelper());
        setHelperForClass(Integer.class, new IntegerHelper());
        setHelperForClass(String.class, new StringHelper());
    }
    
    private class BooleanHelper implements PreferencesHelper {
        public String encode(String key) {
            return Boolean.toString(getBoolean(key));
        }
        
        public Object decode(String valueString) {
            return Boolean.valueOf(valueString);
        }
        
        public void addRow(List<FormPanel> formPanels, final String key, final String description) {
            final JCheckBox checkBox = new JCheckBox(description, getBoolean(key));
            checkBox.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    put(key, checkBox.isSelected());
                }
            });
            formPanels.get(0).addRow("", checkBox);
        }
    }
    
    private class FontHelper implements PreferencesHelper {
        public String encode(String key) {
            // Translate the Font into something Font.decode can parse.
            Font font = getFont(key);
            String style = "plain";
            if (font.getStyle() == Font.BOLD) {
                style = "bold";
            } else if (font.getStyle() == Font.ITALIC) {
                style = "italic";
            } else if (font.getStyle() == (Font.BOLD | Font.ITALIC)) {
                style = "bolditalic";
            }
            return (font.getFamily() + "-" + style + "-" + font.getSize());
        }
        
        public Object decode(String valueString) {
            return Font.decode(valueString);
        }
        
        public void addRow(List<FormPanel> formPanels, final String key, final String description) {
            final JComboBox comboBox = new JComboBox();
            // FIXME: filter out unsuitable fonts. "Zapf Dingbats", for example.
            // FIXME: pull fixed fonts to the top of the list?
            for (String name : GraphicsEnvironment.getLocalGraphicsEnvironment().getAvailableFontFamilyNames()) {
                comboBox.addItem(name);
            }
            comboBox.setSelectedItem(getFont(key).getFamily());
            updateComboBoxFont(key, comboBox);
            comboBox.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    // FIXME: we need a component that lets you choose a size as well as a family! (choosing a style probably isn't necessary for any of our applications, though.)
                    put(key, new Font(comboBox.getSelectedItem().toString(), Font.PLAIN, 12));
                    updateComboBoxFont(key, comboBox);
                }
            });
            // updateComboBoxFont sets the combo box font so that when you choose a font you can see a preview.
            // The alternatives in the pop-up menu, though, should either use their own fonts (which has a habit of causing performance problems) or the default combo box pop-up font. This renderer ensures the latter.
            final ListCellRenderer defaultRenderer = comboBox.getRenderer();
            comboBox.setRenderer(new ListCellRenderer() {
                public Component getListCellRendererComponent(JList list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
                    Component result = defaultRenderer.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                    if (index != -1) {
                        result.setFont(UIManager.getFont("List.font"));
                    }
                    return result;
                }
            });
            formPanels.get(0).addRow(description + ":", comboBox);
        }
        
        private void updateComboBoxFont(String key, JComboBox comboBox) {
            comboBox.setFont(getFont(key).deriveFont(comboBox.getFont().getSize2D()));
        }
    }
    
    private class ColorHelper implements PreferencesHelper {
        public String encode(String key) {
            // Translate the Color into something Color.decode can parse.
            return String.format("0x%06x", getColor(key).getRGB() & 0xffffff);
        }
        
        public Object decode(String valueString) {
            if (valueString.startsWith("#")) {
                // Support HTML-like colors for backwards compatibility, even though we always store colors in Java's preferred textual form.
                valueString = "0x" + valueString.substring(1);
            }
            return Color.decode(valueString);
        }
        
        public void addRow(List<FormPanel> formPanels, final String key, final String description) {
            final ColorSwatchIcon icon = new ColorSwatchIcon(getColor(key), new Dimension(60, 20));
            final JButton button = new JButton(icon);
            button.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    Color newColor = JColorChooser.showDialog(button, "Colors", getColor(key));
                    if (newColor != null) {
                        put(key, newColor);
                        icon.setColor(newColor);
                        button.repaint();
                    }
                }
            });
            // Remove the over-wide horizontal margins most LAFs use. They're trying to give text some breathing room, but we have no text.
            Insets margin = button.getMargin();
            margin.left = margin.right = margin.top = margin.bottom;
            button.setMargin(margin);
            if (System.getProperty("os.version").startsWith("10.4")) {
                button.putClientProperty("JButton.buttonType", "toolbar"); // Mac OS 10.4
            } else {
                button.putClientProperty("JButton.buttonType", "gradient"); // Mac OS 10.5
            }
            
            // FIXME: colorPreferences.put(key, colorPreference);
            formPanels.get(1).addRow(description + ":", button);
        }
    }
    
    private class IntegerHelper implements PreferencesHelper {
        public String encode(String key) {
            return Integer.toString(getInt(key));
        }
        
        public Object decode(String valueString) {
            return Integer.valueOf(valueString);
        }
        
        public void addRow(List<FormPanel> formPanels, final String key, final String description) {
            final ETextField textField = new ETextField(preferences.get(key).toString()) {
                @Override
                public void textChanged() {
                    boolean okay = false;
                    try {
                        int newValue = Integer.parseInt(getText());
                        // FIXME: really, an integer preference should have an explicit range.
                        if (newValue > 0) {
                            put(key, newValue);
                            okay = true;
                        }
                    } catch (NumberFormatException ex) {
                    }
                    setForeground(okay ? UIManager.getColor("TextField.foreground") : Color.RED);
                }
            };
            formPanels.get(0).addRow(description + ":", textField);
        }
    }
    
    private class StringHelper implements PreferencesHelper {
        public String encode(String key) {
            return getString(key);
        }
        
        public Object decode(String valueString) {
            return valueString;
        }
        
        public void addRow(List<FormPanel> formPanels, final String key, final String description) {
            final ETextField textField = new ETextField(preferences.get(key).toString(), 20) {
                @Override
                public void textChanged() {
                    put(key, getText());
                }
            };
            textField.setCaretPosition(0);
            formPanels.get(0).addRow(description + ":", textField);
        }
    }
}
