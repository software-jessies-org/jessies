package e.ptextarea;

import java.util.List;

public abstract class PAbstractTextStyler implements PTextStyler {
    protected PTextArea textArea;
    
    public PAbstractTextStyler(PTextArea textArea) {
        this.textArea = textArea;
    }
    
    public abstract List<PLineSegment> getTextSegments(int line);
    
    public boolean keywordsAreCaseSensitive() {
        return true;
    }
}
