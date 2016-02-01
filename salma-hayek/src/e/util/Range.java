package e.util;

public final class Range {
    /**
     * A unique empty range. If you use this, you might save space, but you
     * also get extra checking: it's illegal to invoke getStart or getEnd
     * on NULL_RANGE (though not on a Range that just happens to be empty).
     */
    public static final Range NULL_RANGE = new Range(-1, -1);
    
    private final int start;
    private final int end;
    
    public Range(int start, int end) {
        this.start = start;
        this.end = end;
    }
    
    public boolean isEmpty() {
        // This operation is allowed on NULL_RANGE.
        return (start == end);
    }
    
    public boolean isNonEmpty() {
        // This operation is allowed on NULL_RANGE.
        return (start != end);
    }
    
    public int getStart() {
        checkNotNull();
        return start;
    }
    
    public int getEnd() {
        checkNotNull();
        return end;
    }
    
    public int length() {
        checkNotNull();
        return (end - start);
    }
    
    private void checkNotNull() {
        if (this == NULL_RANGE) {
            throw new IllegalStateException("can't use NULL_RANGE");
        }
    }
    
    public String toString() {
        if (this == NULL_RANGE) {
            return "Range[NULL_RANGE]";
        }
        return "Range[start=" + start + ",end=" + end + "]";
    }
}
