package e.ptextarea;

public class PPerlIndenter extends PJavaIndenter {
    public PPerlIndenter(PTextArea textArea) {
        super(textArea);
    }
    
    protected boolean shouldMoveHashToColumnZero() {
        return false;
    }
}
