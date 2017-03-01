package e.ptextarea;

import java.util.*;

public abstract class PAbstractTextStyler implements PTextStyler {
    protected PTextArea textArea;
    
    public PAbstractTextStyler(PTextArea textArea) {
        this.textArea = textArea;
    }
    
    public abstract List<PLineSegment> getTextSegments(int line);
    
    public boolean keywordsAreCaseSensitive() {
        return true;
    }
    
    protected class TextSegmentListBuilder {
        private ArrayList<PLineSegment> list = new ArrayList<PLineSegment>();
        private int lineStartOffset;
        private int start = 0;
        
        public TextSegmentListBuilder(int lineStartOffset) {
            this.lineStartOffset = lineStartOffset;
        }
        
        public void addStyledSegment(int end, PStyle style) {
            list.add(new PTextSegment(textArea, lineStartOffset + start, lineStartOffset + end, style));
            start = end;
        }
        
        public List<PLineSegment> getSegmentList() {
            return list;
        }
    }
}
