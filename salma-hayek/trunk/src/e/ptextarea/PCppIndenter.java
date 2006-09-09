package e.ptextarea;

public class PCppIndenter extends PSimpleIndenter {
    public PCppIndenter(PTextArea textArea) {
        super(textArea);
    }
    
    @Override
    public boolean isInNeedOfClosingSemicolon(String line) {
        return line.matches(".*\\b(class|enum|struct|union)\\b.*");
    }

    private static boolean isCppAccessSpecifier(String activePartOfLine) {
        return activePartOfLine.matches("(private|public|protected)\\s*:");
    }
    
    @Override
    protected boolean isLabel(String activePartOfLine) {
        return isCppAccessSpecifier(activePartOfLine) || isSwitchLabel(activePartOfLine);
    }

    @Override
    protected boolean shouldMoveHashToColumnZero() {
        return true;
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
