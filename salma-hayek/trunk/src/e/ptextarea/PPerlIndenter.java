package e.ptextarea;

public class PPerlIndenter extends PSimpleIndenter {
    public PPerlIndenter(PTextArea textArea) {
        super(textArea);
    }
    
    @Override
    protected boolean isLabel(String activePartOfLine) {
        return false;
    }

    @Override
    protected boolean shouldMoveHashToColumnZero() {
        return false;
    }
    
    @Override
    protected boolean shouldMoveLabels() {
        return false;
    }
    
    @Override
    protected boolean shouldContinueDocComments() {
        return false;
    }
}
