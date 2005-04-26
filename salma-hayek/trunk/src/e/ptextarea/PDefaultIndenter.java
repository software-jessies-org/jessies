package e.ptextarea;

public class PDefaultIndenter extends PIndenter {
    public PDefaultIndenter(PTextArea textArea) {
        super(textArea);
    }
    
    public boolean isElectric(char c) {
        return false;
    }
}
