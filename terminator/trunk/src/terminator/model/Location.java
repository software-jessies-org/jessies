package terminator.model;

/**
A Location holds the line index, and character offset within the line of a particular character.

@author Phil Norman
*/

public final class Location implements Comparable<Location> {
    private int lineIndex;
    private int charOffset;
    
    public Location(int lineIndex, int charOffset) {
        this.lineIndex = lineIndex;
        this.charOffset = charOffset;
    }
    
    public int getLineIndex() {
        return lineIndex;
    }
    
    public int getCharOffset() {
        return charOffset;
    }
    
    @Override public String toString() {
        return "Location[line " + lineIndex + ", char " + charOffset + "]";
    }
    
    @Override public int hashCode() {
        int result = 17;
        result = 31 * result + charOffset;
        result = 31 * result + lineIndex;
        return result;
    }
    
    @Override public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj instanceof Location == false) {
            return false;
        }
        Location other = (Location) obj;
        return other.lineIndex == lineIndex && other.charOffset == charOffset;
    }
    
    public boolean charOffsetInRange(int begin, int end) {
        return (getCharOffset() >= begin && getCharOffset() < end);
    }
    
    public int compareTo(Location other) {
        if (other.getLineIndex() == getLineIndex()) {
            return getCharOffset() - other.getCharOffset();
        } else {
            return getLineIndex() - other.getLineIndex();
        }
    }

    public static Location min(Location one, Location two) {
        return (one.compareTo(two) < 0) ? one : two;
    }

    public static Location max(Location one, Location two) {
        return (one.compareTo(two) > 0) ? one : two;
    }
}
