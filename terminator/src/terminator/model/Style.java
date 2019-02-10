package terminator.model;

import java.awt.*;
import terminator.*;

/**
 * Objects of this class are immutable.
 */
public final class Style {
    private static final Style DEFAULT_STYLE = makeStyle(null, null, 0);
    
    public static final int BOLD = (1 << 0);
    
    // ECMA-048 says "faint, decreased intensity or second colour".
    // gnome-terminal implements this as grey.
    // xterm does nothing.
    public static final int DIM = (1 << 1);
    public static final int UNDERLINE = (1 << 2);
    public static final int ITALIC = (1 << 3);
    public static final int BLINK = (1 << 4);
    public static final int REVERSE = (1 << 5);
    public static final int HIDDEN = (1 << 6);
    public static final int STRIKETHROUGH = (1 << 7);
    
    // This style's foreground/background color, or null to indicate this style doesn't affect the foreground/background color.
    // Note that the use of Colors means text styled while a given palette is in use for the lower 16 colors will always use those colors even if the user later switches to a different palette.
    private final Palettes.Ink foreground;
    private final Palettes.Ink background;
    private final int attributes;
    
    @Override public String toString() {
        return "Style[foreground=" + foreground + ", background=" + background + ", attributes=" + Integer.toHexString(attributes) + "]";
    }
    
    @Override public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj instanceof Style == false) {
            return false;
        }
        Style style = (Style) obj;
        if (foreground == null ? style.foreground != null : foreground.equals(style.foreground) == false) {
            return false;
        }
        if (background == null ? style.background != null : background.equals(style.background) == false) {
            return false;
        }
        if (style.attributes != attributes) {
            return false;
        }
        return true;
    }
    
    @Override public int hashCode() {
        int result = 17;
        result = 31 * result + (foreground != null ? foreground.hashCode() : 0);
        result = 31 * result + (background != null ? background.hashCode() : 0);
        result = 31 * result + (attributes);
        return result;
    }
    
    private Style(Palettes.Ink foreground, Palettes.Ink background, int attributes) {
        this.foreground = foreground;
        this.background = background;
        this.attributes = attributes;
    }
    
    public int getAttributes() {
        return attributes;
    }
    
    // getRawForeground can return null if there's no foreground set. Use this to derive styles which retain
    // the state of using the default foreground in case it changes.
    public Palettes.Ink getRawForeground() {
        return foreground;
    }
    
    // getRawBackground can return null if there's no background set. Use this to derive styles which retain
    // the state of using the default background in case it changes.
    public Palettes.Ink getRawBackground() {
        return background;
    }
    
    public Color getForeground() {
        return foreground != null ? foreground.get() : Terminator.getPreferences().getColor(TerminatorPreferences.FOREGROUND_COLOR);
    }
    
    public Color getBackground() {
        return background != null ? background.get() : Terminator.getPreferences().getColor(TerminatorPreferences.BACKGROUND_COLOR);
    }
    
    public static Style getDefaultStyle() {
        return DEFAULT_STYLE;
    }
    
    public static Style makeStyle(Palettes.Ink foreground, Palettes.Ink background, int attributes) {
        return new Style(foreground, background, attributes);
    }
}
