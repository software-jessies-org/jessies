package e.ptextarea;

public class PCharSequenceIterator implements PCharIterator {
    private CharSequence sequence;
    private int offset;
    private boolean iterateForward;
    
    public PCharSequenceIterator(CharSequence sequence, int offset, boolean iterateForward) {
        this.sequence = sequence;
        this.offset = offset;
        this.iterateForward = iterateForward;
    }
    
    public boolean hasNext() {
        return iterateForward ? (offset < sequence.length()) : (offset >= 0);
    }
    
    private int getStep() {
        return iterateForward ? 1 : -1;
    }
    
    public char next() {
        char result = sequence.charAt(offset);
        offset += getStep();
        return result;
    }
    
    public int getOffsetOfLastChar() {
        return offset - getStep();
    }
}
