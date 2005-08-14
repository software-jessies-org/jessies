package e.ptextarea;

import java.awt.*;

/**
 * An enum containing each style understood by PTextArea and its stylers.
 */
public enum PStyle {
    NORMAL("normal", javax.swing.UIManager.getColor("TextArea.foreground")),
    NEWLINE("newline", null),
    STRING("string", Color.decode("#0000ff")),
    COMMENT("comment", Color.decode("#227722")),
    KEYWORD("keyword", Color.decode("#770022")),
    ERROR("error", Color.RED),
    HYPERLINK("hyperlink", Color.BLUE),
    PREPROCESSOR("preprocessor", Color.decode("#708090")),
    UNPRINTABLE("unprintable", Color.RED),
    PATCH_MINUS("patch-minus", Color.RED),
    PATCH_PLUS("patch-plus", Color.BLUE),
    ;
    
    private String name;
    private Color color;
    
    private PStyle(String name, Color color) {
        this.name = name;
        this.color = color;
    }
    
    public String getName() {
        return name;
    }
    
    public Color getColor() {
        return color;
    }
    
    public boolean isUnderlined() {
        return (this == PStyle.HYPERLINK);
    }
    
    public String toString() {
        return "PStyle[" + name + ",color=" + color + "]";
    }
}
