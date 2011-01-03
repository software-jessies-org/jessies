package terminator.model;

import java.awt.*;
import terminator.*;

/**
 * Objects of this class are immutable.
 */
public final class Style {
    private static final Style DEFAULT_STYLE = makeStyle(null, null, false, false, false);

    // This style's foreground/background color, or null to indicate this style doesn't affect the foreground/background color.
    // Note that the use of Colors means text styled while a given palette is in use for the lower 16 colors will always use those colors even if the user later switches to a different palette.
    private final Color foreground;
    private final Color background;

    private final boolean isBold;
    private final boolean isUnderlined;
    private final boolean isReverseVideo;
    
    @Override public String toString() {
        return "Style[foreground=" + foreground + ", background=" + background + ", isBold=" + isBold + ", isUnderlined=" + isUnderlined + ", isReverseVideo=" + isReverseVideo + "]";
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
        if (style.isBold != isBold) {
            return false;
        }
        if (style.isUnderlined != isUnderlined) {
            return false;
        }
        if (style.isReverseVideo != isReverseVideo) {
            return false;
        }
        return true;
    }
    
    @Override public int hashCode() {
        int result = 17;
        result = 31 * result + (foreground != null ? foreground.hashCode() : 0);
        result = 31 * result + (background != null ? background.hashCode() : 0);
        result = 31 * result + (isBold ? 1 : 0);
        result = 31 * result + (isUnderlined ? 1 : 0);
        result = 31 * result + (isReverseVideo ? 1 : 0);
        return result;
    }
    
    private Style(Color foreground, Color background, boolean isBold, boolean isUnderlined, boolean isReverseVideo) {
        this.foreground = foreground;
        this.background = background;
        this.isBold = isBold;
        this.isUnderlined = isUnderlined;
        this.isReverseVideo = isReverseVideo;
    }
    
    public Color getForeground() {
        return foreground != null ? foreground : Terminator.getPreferences().getColor(TerminatorPreferences.FOREGROUND_COLOR);
    }
    
    public Color getBackground() {
        return background != null ? background : Terminator.getPreferences().getColor(TerminatorPreferences.BACKGROUND_COLOR);
    }
    
    public boolean isBold() {
        return isBold;
    }
    
    public boolean isUnderlined() {
        return isUnderlined;
    }
    
    public boolean isReverseVideo() {
        return isReverseVideo;
    }
    
    public static Style getDefaultStyle() {
        return DEFAULT_STYLE;
    }
    
    public static Style makeStyle(Color foreground, Color background, boolean isBold, boolean isUnderlined, boolean isReverseVideo) {
        return new Style(foreground, background, isBold, isUnderlined, isReverseVideo);
    }
}
