package e.util;

import java.awt.*;

/**
 * Wraps a Preferences object, and provides an interface to get hold of preferences from it, but with each
 * key prefixed with a given string.
 * 
 * @author Phil Norman
 */

public class PrefixedPreferences extends PreferenceGetter {
    private final Preferences preferences;
    private final String prefix;
    
    public PrefixedPreferences(Preferences preferences, String prefix) {
        this.preferences = preferences;
        this.prefix = prefix;
    }
    
    public Object get(String key) {
        return preferences.get(prefix + key);
    }
}
