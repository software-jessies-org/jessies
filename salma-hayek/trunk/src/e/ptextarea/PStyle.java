package e.ptextarea;

import java.awt.*;

/**
 * An enum containing each style understood by PTextArea and its stylers.
 */
public enum PStyle {
    NORMAL("normal", javax.swing.UIManager.getColor("EditorPane.foreground"), Font.PLAIN),
    NEWLINE("newline", null, Font.PLAIN),
    STRING("string", Color.decode("#0000ff"), Font.PLAIN),
    COMMENT("comment", Color.decode("#227722"), Font.PLAIN),
    KEYWORD("keyword", Color.decode("#770022"), Font.PLAIN),
    ERROR("error", Color.RED, Font.PLAIN),
    HYPERLINK("hyperlink", Color.BLUE, Font.PLAIN),
    PREPROCESSOR("preprocessor", Color.decode("#708090"), Font.PLAIN),
    UNPRINTABLE("unprintable", Color.RED, Font.PLAIN),
    PATCH_AT("patch-at", Color.GRAY, Font.PLAIN),
    PATCH_MINUS("patch-minus", Color.RED, Font.PLAIN),
    PATCH_PLUS("patch-plus", Color.BLUE, Font.PLAIN),
    NORMAL_BOLD("normal-bold", NORMAL.getColor(), Font.BOLD),
    NORMAL_ITALIC("normal-italic", NORMAL.getColor(), Font.ITALIC)
    ;
    
    private String name;
    private Color color;
    private int fontFlags;
    
    private PStyle(String name, Color color, int fontFlags) {
        this.name = name;
        this.color = color;
        this.fontFlags = fontFlags;
    }
    
    public String getName() {
        return name;
    }
    
    public Color getColor() {
        return color;
    }
    
    public int getFontFlags() {
        return fontFlags;
    }
    
    public boolean isUnderlined() {
        return (this == PStyle.HYPERLINK);
    }
    
    public String toString() {
        return "PStyle[" + name + ",color=" + color + "]";
    }
}
