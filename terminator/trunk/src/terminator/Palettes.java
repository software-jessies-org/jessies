package terminator;

import java.awt.Color;
import java.util.*;

public class Palettes {
    /** Tango palette from gnome-terminal 2.24. */
    private static final Color[] TANGO_COLORS = new Color[] {
        new Color(0x2e3436),
        new Color(0xcc0000),
        new Color(0x4e9a06),
        new Color(0xc4a000),
        new Color(0x3465a4),
        new Color(0x75507b),
        new Color(0x06989a),
        new Color(0xd3d7cf),
        new Color(0x555753),
        new Color(0xef2929),
        new Color(0x8ae234),
        new Color(0xfce94f),
        new Color(0x729fcf),
        new Color(0xad7fa8),
        new Color(0x34e2e2),
        new Color(0xeeeeec),
    };
    
    /* Linux palette from gnome-terminal 2.24. */
    private static final Color[] LINUX_COLORS = new Color[] {
        new Color(0x000000),
        new Color(0xaa0000),
        new Color(0x00aa00),
        new Color(0xaa5500),
        new Color(0x0000aa),
        new Color(0xaa00aa),
        new Color(0x00aaaa),
        new Color(0xaaaaaa),
        new Color(0x555555),
        new Color(0xff5555),
        new Color(0x55ff55),
        new Color(0xffff55),
        new Color(0x5555ff),
        new Color(0xff55ff),
        new Color(0x55ffff),
        new Color(0xffffff),
    };
    
    /* XTerm palette from gnome-terminal 2.24. */
    private static final Color[] XTERM_COLORS = new Color[] {
        new Color(0x000000),
        new Color(0xcd0000),
        new Color(0x00cd00),
        new Color(0xcdcd00),
        new Color(0x1e90ff),
        new Color(0xcd00cd),
        new Color(0x00cdcd),
        new Color(0xe5e5e5),
        new Color(0x4c4c4c),
        new Color(0xff0000),
        new Color(0x00ff00),
        new Color(0xffff00),
        new Color(0x4682b4),
        new Color(0xff00ff),
        new Color(0x00ffff),
        new Color(0xffffff),
    };
    
    /* RXVT palette from gnome-terminal 2.24. */
    private static final Color[] RXVT_COLORS = new Color[] {
        new Color(0x000000),
        new Color(0xcd0000),
        new Color(0x00cd00),
        new Color(0xcdcd00),
        new Color(0x0000cd),
        new Color(0xcd00cd),
        new Color(0x00cdcd),
        new Color(0xfaebd7),
        new Color(0x404040),
        new Color(0xff0000),
        new Color(0x00ff00),
        new Color(0xffff00),
        new Color(0x0000ff),
        new Color(0xff00ff),
        new Color(0x00ffff),
        new Color(0xffffff),
    };
    
    /** "ANSI" palette from Terminator. Where did we get this? */
    private static final Color[] ANSI_COLORS = {
        //
        // Normal intensity colors 0-7.
        //
        
        // Color 0: black
        new Color(0x000000),
        // Color 1: red3
        new Color(0xcd0000),
        // Color 2: green3
        new Color(0x00cd00),
        // Color 3: yellow3
        new Color(0xcdcd00),
        // Color 4: blue2
        new Color(0x0000ee),
        // Color 5: magenta3
        new Color(0xcd00cd),
        // Color 6: cyan3
        new Color(0x00cdcd),
        // Color 7: grey90
        new Color(0xe5e5e5),
        
        //
        // Bold variants of colors 0-7.
        // There are xterm-16color and rxvt-16color variants, but I've not seen them used, and don't know of anything that would take advantage of the extra colors (which would require a significantly more complicated terminfo, and support for extra sequences).
        //
        
        // Color 8: gray50
        new Color(0x7f7f7f),
        // Color 9: red
        new Color(0xff0000),
        // Color 10: green
        new Color(0x00ff00),
        // Color 11: yellow
        new Color(0xffff00),
        // Color 12: rgb:5c/5c/ff
        new Color(0x5c5cff),
        // Color 13: magenta
        new Color(0xff00ff),
        // Color 14: cyan
        new Color(0x00ffff),
        // Color 15: white
        new Color(0xffffff),
    };
    
    private static final HashMap<String, Color[]> palettes = new HashMap<String, Color[]>();
    static {
        palettes.put("ANSI", ANSI_COLORS);
        palettes.put("Linux", LINUX_COLORS);
        palettes.put("Rxvt", RXVT_COLORS);
        palettes.put("Tango", TANGO_COLORS);
        palettes.put("XTerm", XTERM_COLORS);
    }
    
    public static String toString(Color[] colors) {
        for (Map.Entry<String, Color[]> palette : palettes.entrySet()) {
            if (palette.getValue() == colors) {
                return palette.getKey();
            }
        }
        throw new IllegalArgumentException("unknown palette: " + colors);
    }
    
    public static List<String> names() {
        ArrayList<String> result = new ArrayList<String>(palettes.keySet());
        Collections.sort(result);
        return Collections.unmodifiableList(result);
    }
    
    public static Color[] fromString(String palette) {
        return palettes.get(palette);
    }
    
    private static Color[] currentPalette() {
        return (Color[]) Terminator.getPreferences().get(TerminatorPreferences.PALETTE);
    }
    
    /**
     * Returns the color corresponding to 'index' (0-7), in its normal or bright variant.
     */
    public static Color getColor(int index, boolean bright) {
        if (!bright) {
            return currentPalette()[index];
        } else {
            return currentPalette()[index + 8];
        }
    }
    
    /**
     * Tries to get a good bold foreground color.
     * This is equivalent to "colorBD" in XTerm, but isn't under the control of the user.
     * This is mainly a historical accident.
     * (But as long as no-one cares, it's quite nice that we automatically choose a good bold color.)
     */
    public static Color getBrightColorFor(Color color) {
        // If the color is one of the "standard" colors, use the corresponding bright variant.
        // We try the user's preferred palette first in case of conflicts.
        for (Color[] palette : new Color[][] { currentPalette(), ANSI_COLORS, LINUX_COLORS, RXVT_COLORS, TANGO_COLORS, XTERM_COLORS }) {
            for (int i = 0; i < 8; ++i) {
                if (color.equals(palette[i])) {
                    return palette[i + 8];
                }
            }
        }
        
        // That didn't work, so try to invent a suitable color.
        // The typical use of boldForegroundColor is to turn off-white into pure white or off-black.
        // One approach might be to use the NTSC or HDTV luminance formula, but it's not obvious that they generalize to other colors.
        // Adjusting each component individually if it's close to full-on or full-off is simple and seems like it might generalize.
        return new Color(adjustForBD(color.getRed()), adjustForBD(color.getGreen()), adjustForBD(color.getBlue()));
    }
    
    private static int adjustForBD(int component) {
        // These limits are somewhat arbitrary "round" hex numbers.
        // 0x11 would be too close to the LCD "blacker than black".
        // The default XTerm normal-intensity and bold blacks differ by 0x30.
        if (component < 0x33) {
            return 0x00;
        } else if (component > 0xcc) {
            return 0xff;
        } else {
            return component;
        }
    }
}
