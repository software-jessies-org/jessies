package e.ptextarea;

public class PPerlIndenter extends PCFamilyIndenter {
    public PPerlIndenter(PTextArea textArea) {
        super(textArea);
    }
    
    @Override
    protected boolean isLabel(String activePartOfLine) {
        return false;
    }
}
