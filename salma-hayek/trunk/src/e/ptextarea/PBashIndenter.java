package e.ptextarea;

import java.util.regex.*;

public class PBashIndenter extends PGenericIndenter {
    public PBashIndenter(PTextArea textArea) {
        super(textArea,
            "(\\{(?![^\\}]*\\})|\\b(then|elif|else)\\b(?!.*fi)|\\bdo\\b(?!.+done)|\\bcase\\s+\\S+\\s+in\\b(?!.*esac)|\\[\\[)",
            "\\$\\{.*\\}",
            "(\\}|\\b(fi|elif|else)\\b|\\bdone\\b|\\besac\\b|\\]\\])",
            "{}cefin");
    }
}
