package e.ptextarea;

/**
 * A CharSequence that returns ' ' for uninteresting characters, for the
 * purposes of bracket matching. For example, if you're matching a bracket
 * in something of style NORMAL, you don't want it to match a COMMENT or
 * STRING.
 * 
 * This is a rather inefficient implementation if you're editing a large file
 * because it iterates over all the logical segments, even if it doesn't need
 * to, and it copies the entire text.
 */
public class PSameStyleCharSequence implements CharSequence {
    private StringBuffer mangledText;
    
    // FIXME: need to take an offset or a PLineSegment as a parameter so we
    // know what to ignore. I think there are two cases: if you're in style
    // NORMAL, you want to see any other NORMAL text. If you're not, you only
    // want to see text of the same style that's contiguous with where you
    // are. A bracket in this comment, say, shouldn't match a bracket in
    // another comment. Single-line comments make this difficult, but let's
    // face it: the important case is the NORMAL case.
    public PSameStyleCharSequence(PTextArea textArea) {
        // FIXME: 1.5 has a StringBuffer(CharSequence) constructor.
        this.mangledText = new StringBuffer(textArea.getTextBuffer().toString());
        
        PSegmentIterator it = textArea.getLogicalSegmentIterator(0);
        while (it.hasNext()) {
            PLineSegment segment = it.next();
            if (segment.getStyle() != PStyle.NORMAL) {
                for (int i = segment.getOffset(); i < segment.getEnd(); ++i) {
                    /* TEST: this { should match this } */
                    /* TEST: the for loop's brace shouldn't match this } */
                    /* TEST: this { shouldn't match anything */
                    mangledText.setCharAt(i, ' ');
                }
            }
        }
    }
    
    public char charAt(int index) {
        return mangledText.charAt(index);
    }
    
    public int length() {
        return mangledText.length();
    }
    
    public CharSequence subSequence(int start, int end) {
        throw new UnsupportedOperationException("subSequence(" + start + ", " + end + ")");
    }
}
