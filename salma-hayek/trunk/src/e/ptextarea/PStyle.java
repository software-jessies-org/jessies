package e.ptextarea;

import java.awt.*;

/**
 * An enum containing each style understood by PTextArea and its stylers.
 */
public final class PStyle {
    public static final PStyle NORMAL = new PStyle(Color.BLACK);
    public static final PStyle STRING = new PStyle(Color.decode("#0000ff"));
    public static final PStyle COMMENT = new PStyle(Color.decode("#227722"));
    public static final PStyle KEYWORD = new PStyle(Color.decode("#770022"));
    public static final PStyle ERROR = new PStyle(Color.RED);
    public static final PStyle HYPERLINK = new PStyle(Color.BLUE);
    
    private Color color;
    
    public final Color getColor() {
        return color;
    }
    
    private PStyle(Color color) {
        // This should be an enum come Java 1.5.
        this.color = color;
    }
}
