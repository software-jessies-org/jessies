package e.ptextarea;

import java.awt.*;

/**
 * An enum containing each style understood by PTextArea and its stylers.
 */
public final class PStyle {
    public static final PStyle NORMAL = new PStyle("normal", javax.swing.UIManager.getColor("TextArea.foreground"));
    public static final PStyle STRING = new PStyle("string", Color.decode("#0000ff"));
    public static final PStyle COMMENT = new PStyle("comment", Color.decode("#227722"));
    public static final PStyle KEYWORD = new PStyle("keyword", Color.decode("#770022"));
    public static final PStyle ERROR = new PStyle("error", Color.RED);
    public static final PStyle HYPERLINK = new PStyle("hyperlink", Color.BLUE);
    
    private String name;
    private Color color;
    
    public String getName() {
        return name;
    }
    
    public Color getColor() {
        return color;
    }
    
    public String toString() {
        return "PStyle[" + name + ",color=" + color + "]";
    }
    
    private PStyle(String name, Color color) {
        // This should be an enum come Java 1.5.
        this.name = name;
        this.color = color;
    }
}
