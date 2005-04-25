package e.ptextarea;

public class PCppIndenter extends PSimpleIndenter {
    public PCppIndenter(PTextArea textArea) {
        super(textArea);
    }
    
    public boolean isInNeedOfClosingSemicolon(String line) {
        return line.matches(".*\\b(class|enum|struct|union)\\b.*");
    }

    private static boolean isCppAccessSpecifier(String activePartOfLine) {
        return activePartOfLine.matches("(private|public|protected)\\s*:");
    }
    
    protected String stripComments(String line) {
        return stripMultiLineComments(stripDoubleSlashComment(line));
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
