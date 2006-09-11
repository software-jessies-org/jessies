package e.ptextarea;

public class PPerlIndenter extends PCFamilyIndenter {
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
}
