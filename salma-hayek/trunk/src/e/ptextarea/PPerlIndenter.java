package e.ptextarea;

public class PPerlIndenter extends PIndenter {
    public PPerlIndenter(PTextArea textArea) {
        super(textArea);
    }
    
    protected boolean shouldMoveHashToColumnZero() {
        return false;
    }
}
