package e.util;

import java.awt.*;

/**
 * Provides convenience methods for getting specifically-typed preferences from some abstract storage.
 * 
 * @author Phil Norman
 */

public abstract class PreferenceGetter {
    public PreferenceGetter() { }
    
    public abstract Object get(String key);
    
    
    public String getString(String key) {
        return (String) get(key);
    }
    
    public boolean getBoolean(String key) {
        return (Boolean) get(key);
    }
    
    public Color getColor(String key) {
        return (Color) get(key);
    }
    
    public Font getFont(String key) {
        return (Font) get(key);
    }
    
    public int getInt(String key) {
        return (Integer) get(key);
    }
}
