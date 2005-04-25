package e.ptextarea;

public class PPerlIndenter extends PSimpleIndenter {
    public PPerlIndenter(PTextArea textArea) {
        super(textArea);
    }
    
    protected String stripComments(String line) {
        return stripHashComment(line);
    }

    protected boolean isLabel(String activePartOfLine) {
        return false;
    }

    protected boolean shouldMoveHashToColumnZero() {
        return false;
    }
    protected boolean shouldMoveLabels() {
        return false;
    }
    protected boolean shouldContinueDocComments() {
        return false;
    }
}
