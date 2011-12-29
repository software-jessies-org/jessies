package e.util;

import java.awt.*;

/**
 * Wraps a Preferences object, and provides an interface to get hold of preferences from it, but with each
 * key prefixed with a given string.
 * 
 * @author Phil Norman
 */

public class PrefixedPreferences {
    private Preferences preferences;
    private String prefix;
    
    public PrefixedPreferences(Preferences preferences, String prefix) {
        this.preferences = preferences;
        this.prefix = prefix;
    }
    
    public Object get(String key) {
        return preferences.get(prefix + key);
    }
    
    public String getString(String key) {
        return (String) preferences.get(prefix + key);
    }
    
    public boolean getBoolean(String key) {
        return (Boolean) preferences.get(prefix + key);
    }
    
    public Color getColor(String key) {
        return (Color) preferences.get(prefix + key);
    }
    
    public Font getFont(String key) {
        return (Font) preferences.get(prefix + key);
    }
    
    public int getInt(String key) {
        return (Integer) preferences.get(prefix + key);
    }
}
