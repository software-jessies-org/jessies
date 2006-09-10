package e.ptextarea;

public class PJavaIndenter extends PCFamilyIndenter {
    public PJavaIndenter(PTextArea textArea) {
        super(textArea);
    }
    
    @Override
    protected boolean isLabel(String activePartOfLine) {
        return isSwitchLabel(activePartOfLine);
    }
    
    @Override
    protected boolean shouldMoveHashToColumnZero() {
        return false;
    }
    
    @Override
    protected boolean shouldMoveLabels() {
        return true;
    }
    
    @Override
    protected boolean shouldContinueDocComments() {
        return true;
    }
}
