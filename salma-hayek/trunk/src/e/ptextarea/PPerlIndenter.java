package e.ptextarea;

public class PPerlIndenter extends PJavaIndenter {
    public PPerlIndenter(PTextArea textArea) {
        super(textArea);
    }
    
    protected String stripComments(String line) {
        return line.replaceFirst("#.*", "");
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
