package e.ptextarea;

public class PCppIndenter extends PJavaIndenter {
    public PCppIndenter(PTextArea textArea) {
        super(textArea);
    }
    
    public boolean isInNeedOfClosingSemicolon(String line) {
        return line.matches(".*\\b(class|enum|struct|union)\\b.*");
    }
}
