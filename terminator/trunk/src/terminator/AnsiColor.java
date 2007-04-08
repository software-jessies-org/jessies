package terminator;

import java.awt.Color;

/**
 * Hands out requested ANSI colors.
 */
public final class AnsiColor {
    private static final Color[] ansiColors = {
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
    
    public static Color byIndex(int index) {
        return ansiColors[index];
    }
    
    private AnsiColor() {
    }
}
