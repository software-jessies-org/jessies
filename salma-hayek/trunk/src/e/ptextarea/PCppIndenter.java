package e.ptextarea;

public class PCppIndenter extends PJavaIndenter {
    public PCppIndenter(PTextArea textArea) {
        super(textArea);
    }
    
    public boolean isInNeedOfClosingSemicolon(String line) {
        return line.matches(".*\\b(class|enum|struct|union)\\b.*");
    }

    public boolean isCppAccessSpecifier(String activePartOfLine) {
        return activePartOfLine.matches("(private|public|protected)\\s*:");
    }
    
    protected String stripComments(String line) {
        return stripCppComments(line);
    }
    
    protected boolean isLabel(String activePartOfLine) {
        return isCppAccessSpecifier(activePartOfLine) || isSwitchLabel(activePartOfLine);
    }

    protected boolean shouldMoveHashToColumnZero() {
        return true;
    }
    protected boolean shouldMoveLabels() {
        return true;
    }
    protected boolean shouldContinueDocComments() {
        return true;
    }
}
