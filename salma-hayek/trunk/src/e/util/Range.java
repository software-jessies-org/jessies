package e.util;

public class Range {
    public int start;
    public int end;
    
    public boolean isEmpty() {
        return (start == end);
    }
    
    public boolean isNonEmpty() {
        return (start != end);
    }
    
    public int length() {
        return (end - start);
    }
    
    public String toString() {
        return "Range[start=" + start + ",end=" + end + "]";
    }
}
