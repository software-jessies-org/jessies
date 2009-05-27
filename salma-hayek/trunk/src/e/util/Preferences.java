package e.util;

import e.forms.*;
import e.gui.*;
import java.awt.*;
import java.awt.event.*;
import java.util.*;
import java.util.List;
import java.util.regex.*;
import javax.swing.*;
import javax.swing.event.*;

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
 * Use initPreferencesMenuItem to add a "Preferences..." item to your "Edit" menu, and to automatically do the right thing on Mac OS.
 * Use addPreferencesListener if you need to be called back when a preference is changed.
 * 
 * @author Elliott Hughes
 */
public abstract class Preferences {
    private static final Pattern RESOURCE_PATTERN = Pattern.compile("(?:\\S+(?:\\*|\\.))?(\\S+):\\s*(.+)");
    
    private static class KeyAndTab {
        String key;
        String tab;
        
        KeyAndTab(String key, String tab) {
            this.key = key;
            this.tab = tab;
        }
    }
    
    // Mutable at any time.
    private HashMap<String, Object> preferences = new HashMap<String, Object>();
    // Immutable after initialization.
    private final HashMap<String, Object> defaults = new HashMap<String, Object>();
    
    private final HashMap<String, String> descriptions = new HashMap<String, String>();
    private final ArrayList<KeyAndTab> keysInUiOrder = new ArrayList<KeyAndTab>();
    
    private final ArrayList<String> tabTitles = new ArrayList<String>();
    
    private final HashMap<Class<?>, PreferencesHelper> helpers = new HashMap<Class<?>, PreferencesHelper>();
    private final HashMap<String, JComponent> customUis = new HashMap<String, JComponent>();
    
    private final ArrayList<Listener> listeners = new ArrayList<Listener>();
    
    // Non-null if the preferences dialog is currently showing.
    private FormBuilder form;
    
    public Preferences() {
        initHelpers();
        initPreferences();
    }
    
    // Override this so we know where to read from and write to.
    protected abstract String getPreferencesFilename();
    
    // Override this to call addPreference and addSeparator to set up your defaults.
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
    
    protected void addTab(String tabTitle) {
        tabTitles.add(tabTitle);
    }
    
    protected void addPreference(String key, Object value, String description) {
        addPreference(null, key, value, description);
    }
    
    protected void addPreference(String tabName, String key, Object value, String description) {
        put(key, value);
        defaults.put(key, value);
        descriptions.put(key, description);
        keysInUiOrder.add(new KeyAndTab(key, tabName));
    }
    
    protected void addSeparator() {
        addSeparator(null);
    }
    
    protected void addSeparator(String tabName) {
        keysInUiOrder.add(new KeyAndTab(null, tabName));
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
    
    public void initPreferencesMenuItem(JMenu editMenu) {
        if (GuiUtilities.isMacOs()) {
            com.apple.eawt.Application.getApplication().setEnabledPreferencesMenu(true);
            com.apple.eawt.Application.getApplication().addApplicationListener(new com.apple.eawt.ApplicationAdapter() {
                @Override
                public void handlePreferences(com.apple.eawt.ApplicationEvent e) {
                    showPreferencesDialog();
                    e.setHandled(true);
                }
            });
        } else {
            editMenu.addSeparator();
            editMenu.add(makeShowPreferencesAction());
        }
    }
    
    public Action makeShowPreferencesAction() {
        return new ShowPreferencesAction(this);
    }
    
    private static class ShowPreferencesAction extends AbstractAction {
        private Preferences preferences;
        
        public ShowPreferencesAction(Preferences preferences) {
            // Firefox has 'n' as the mnemonic, but GNOME says 'e'.
            GuiUtilities.configureAction(this, "Pr_eferences...", null);
            this.preferences = preferences;
            GnomeStockIcon.configureAction(this);
        }
        
        public void actionPerformed(ActionEvent e) {
            preferences.showPreferencesDialog();
        }
    }
    
    private void showPreferencesDialog() {
        // Find out which Frame should be the parent of the dialog.
        Component focusOwner = KeyboardFocusManager.getCurrentKeyboardFocusManager().getPermanentFocusOwner();
        if (focusOwner instanceof Frame == false) {
            focusOwner = SwingUtilities.getAncestorOfClass(Frame.class, focusOwner);
        }
        if (focusOwner != null && focusOwner.isShowing() && focusOwner.getLocationOnScreen().y < 0) {
            // This is probably the Mac OS hidden frame hack, in which case we should center on the screen.
            focusOwner = null;
        }
        final Frame parent = (Frame) focusOwner;
        
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
        
        form = new FormBuilder(parent, "Preferences", tabTitles.size() > 0 ? tabTitles : Arrays.asList("<anonymous>"));
        
        final List<FormPanel> formPanels = form.getFormPanels();
        
        willAddRows(formPanels);
        
        for (KeyAndTab keyAndTab : keysInUiOrder) {
            final FormPanel formPanel = formPanels.get(indexOfTab(keyAndTab.tab));
            if (keyAndTab == null) {
                formPanel.addEmptyRow();
                continue;
            }
            final String key = keyAndTab.key;
            final String description = descriptions.get(key);
            if (description != null) {
                if (customUis.get(key) != null) {
                    formPanel.addRow(description + ":", customUis.get(key));
                } else {
                    helperForKey(key).addRow(formPanel, key, description);
                }
            }
        }
        
        didAddRows(formPanels);
        
        // Save the preferences if the user hits "Save".
        form.getFormDialog().setAcceptCallable(new java.util.concurrent.Callable<Boolean>() {
            public Boolean call() {
                boolean saved = writeToDisk();
                if (saved == false) {
                    SimpleDialog.showAlert(parent, "Couldn't save preferences.", "There was a problem writing preferences to \"" + getPreferencesFilename() + "\".");
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
        
        final JButton extraButton = getExtraButton();
        if (extraButton != null) {
            form.getFormDialog().setExtraButton(extraButton);
        }
        
        form.getFormDialog().setRememberBounds(false);
        form.show("Save");
    }
    
    private int indexOfTab(String tabName) {
        if (tabName == null) {
            return 0;
        }
        return tabTitles.indexOf(tabName);
    }
    
    // Override this to add rows before the automatic ones.
    protected void willAddRows(List<FormPanel> formPanels) {
        // Don't add code here. This is for subclasses!
    }
    
    // Override this to add rows after the automatic ones.
    protected void didAddRows(List<FormPanel> formPanels) {
        // Don't add code here. This is for subclasses!
    }
    
    // Override this to return any JButton you'd like added to the preferences dialog.
    protected JButton getExtraButton() {
        return null;
    }
    
    public void readFromDisk() {
        String filename = getPreferencesFilename();
        try {
            if (FileUtilities.exists(filename) == false) {
                return;
            }
            String data = StringUtilities.readFile(getPreferencesFilename());
            if (data.startsWith("<?xml ")) {
                processXmlString(data);
            } else {
                processResourceLines(data.split("\n"));
            }
        } catch (Exception ex) {
            Log.warn("Problem reading preferences from \"" + getPreferencesFilename() + "\"", ex);
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
    
    public boolean writeToDisk() {
        String filename = getPreferencesFilename();
        try {
            org.w3c.dom.Document document = XmlUtilities.makeEmptyDocument();
            org.w3c.dom.Element root = document.createElement("preferences");
            document.appendChild(root);
            
            for (KeyAndTab keyAndTab : keysInUiOrder) {
                final String key = keyAndTab.key;
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
        public void addRow(FormPanel formPanel, String key, final String description);
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
        
        public void addRow(FormPanel formPanel, final String key, final String description) {
            final JCheckBox checkBox = new JCheckBox(description, getBoolean(key));
            checkBox.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    put(key, checkBox.isSelected());
                }
            });
            formPanel.addRow("", checkBox);
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
        
        public void addRow(FormPanel formPanel, final String key, final String description) {
            final JComboBox fontNameComboBox = makeFontNameComboBox(key);
            final JComboBox fontSizeComboBox = makeFontSizeComboBox(key);
            
            // updateComboBoxFont (below) sets the combo box font so that when you choose a font you can see a preview.
            // Ensure that when the font name or font size changes, we call updateComboBoxFont.
            ActionListener actionListener = new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    put(key, new Font(fontNameComboBox.getSelectedItem().toString(), Font.PLAIN, Integer.parseInt(fontSizeComboBox.getSelectedItem().toString())));
                    updateComboBoxFont(key, fontNameComboBox);
                }
            };
            updateComboBoxFont(key, fontNameComboBox);
            fontNameComboBox.addActionListener(actionListener);
            fontSizeComboBox.addActionListener(actionListener);
            
            // It's too expensive to show a preview of every possible font when the user pulls up the combo box's menu, sadly.
            // You can do it if you're determined, by caching images at install-time like MS Word.
            // A better choice on our budget would probably be writing our own JFontChooser along the lines of the GTK+ font chooser (which includes a preview).
            // In the meantime, this renderer ensures that we don't use the currently selected font to mislead, but use the default combo box pop-up font instead.
            final ListCellRenderer defaultRenderer = fontNameComboBox.getRenderer();
            fontNameComboBox.setRenderer(new ListCellRenderer() {
                public Component getListCellRendererComponent(JList list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
                    Component result = defaultRenderer.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                    if (index != -1) {
                        result.setFont(UIManager.getFont("List.font"));
                    }
                    return result;
                }
            });
            
            // Stick the two combo boxes (font name and font size) together.
            JPanel fontChooser = new JPanel(new BorderLayout(2, 0));
            fontChooser.add(fontNameComboBox, BorderLayout.CENTER);
            fontChooser.add(fontSizeComboBox, BorderLayout.EAST);
            formPanel.addRow(description + ":", fontChooser);
        }
        
        private JComboBox makeFontNameComboBox(String key) {
            JComboBox fontNameComboBox = new JComboBox();
            // FIXME: filter out unsuitable fonts. "Zapf Dingbats", for example.
            // FIXME: pull monospaced fonts to the top of the list?
            // FIXME: if the current setting is a monospaced font, only allow other monospaced fonts?
            // FIXME: Windows uses a hard-coded whitelist of suitable monospaced fonts (http://blogs.msdn.com/oldnewthing/archive/2007/05/16/2659903.aspx).
            for (String name : GraphicsEnvironment.getLocalGraphicsEnvironment().getAvailableFontFamilyNames()) {
                fontNameComboBox.addItem(name);
            }
            fontNameComboBox.setSelectedItem(getFont(key).getFamily());
            return fontNameComboBox;
        }
        
        private JComboBox makeFontSizeComboBox(String key) {
            JComboBox fontSizeComboBox = new JComboBox(new Integer[] { 6, 7, 8, 9, 10, 11, 12, 13, 14, 16, 18, 20, 22, 24, 26, 28, 32, 36, 40, 48, 56, 64, 72 });
            fontSizeComboBox.setSelectedItem(getFont(key).getSize());
            return fontSizeComboBox;
        }
        
        private void updateComboBoxFont(String key, JComboBox fontNameComboBox) {
            fontNameComboBox.setFont(getFont(key).deriveFont(fontNameComboBox.getFont().getSize2D()));
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
        
        public void addRow(final FormPanel formPanel, final String key, final String description) {
            final Color originalColor = getColor(key);
            final ColorSwatchIcon icon = new ColorSwatchIcon(originalColor, new Dimension(60, 20));
            final JButton button = new JButton(icon);
            button.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    // JColorChooser.showDialog looks terrible, and doesn't let us dynamically respond to the user's choice.
                    final JColorChooser colorChooser = new JColorChooser(getColor(key));
                    // No platform uses anything like Swing's hideous preview panel...
                    colorChooser.setPreviewPanel(new JPanel());
                    // ...and we want to provide a live preview ourselves.
                    colorChooser.getSelectionModel().addChangeListener(new ChangeListener() {
                        public void stateChanged(ChangeEvent e) {
                            setColor(colorChooser.getColor());
                        }
                    });
                    // Use FormBuilder to provide a decent dialog.
                    final Frame parent = (Frame) SwingUtilities.getAncestorOfClass(Frame.class, formPanel);
                    final String title = description;
                    final FormBuilder form = new FormBuilder(parent, title);
                    form.getFormPanel().addRow(null, colorChooser);
                    if (form.show("OK") == false) {
                        // Reset the color if the user hit "Cancel".
                        setColor(originalColor);
                    }
                }
                
                private void setColor(final Color newColor) {
                    put(key, newColor);
                    icon.setColor(newColor);
                    button.repaint();
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
            
            formPanel.addRow(description + ":", button);
        }
    }
    
    private class IntegerHelper implements PreferencesHelper {
        public String encode(String key) {
            return Integer.toString(getInt(key));
        }
        
        public Object decode(String valueString) {
            return Integer.valueOf(valueString);
        }
        
        public void addRow(FormPanel formPanel, final String key, final String description) {
            final JTextField textField = new JTextField(preferences.get(key).toString());
            textField.getDocument().addDocumentListener(new DocumentAdapter() {
                public void documentChanged() {
                    boolean okay = false;
                    try {
                        int newValue = Integer.parseInt(textField.getText());
                        // FIXME: really, an integer preference should have an explicit range.
                        if (newValue > 0) {
                            put(key, newValue);
                            okay = true;
                        }
                    } catch (NumberFormatException ex) {
                    }
                    textField.setForeground(okay ? UIManager.getColor("TextField.foreground") : Color.RED);
                }
            });
            formPanel.addRow(description + ":", textField);
        }
    }
    
    private class StringHelper implements PreferencesHelper {
        public String encode(String key) {
            return getString(key);
        }
        
        public Object decode(String valueString) {
            return valueString;
        }
        
        public void addRow(FormPanel formPanel, final String key, final String description) {
            final JTextField textField = new JTextField(preferences.get(key).toString(), 20);
            textField.getDocument().addDocumentListener(new DocumentAdapter() {
                public void documentChanged() {
                    put(key, textField.getText());
                }
            });
            textField.setCaretPosition(0);
            formPanel.addRow(description + ":", textField);
        }
    }
}
