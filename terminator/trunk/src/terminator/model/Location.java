package terminator.model;

/**
A Location holds the line index, and character offset within the line of a particular character.

@author Phil Norman
*/

public final class Location implements Comparable {
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
	
	public String toString() {
		return "Location[line " + lineIndex + ", char " + charOffset + "]";
	}
	
	public int hashCode() {  // Ought to use a prime, but I can't be bothered to work one out.
		return (getLineIndex() * 163477) ^ getCharOffset();
	}
	
	public boolean equals(Object obj) {
		if (obj instanceof Location) {
			Location other = (Location) obj;
			return other.getLineIndex() == getLineIndex() && other.getCharOffset() == getCharOffset();
		} else {
			return false;
		}
	}
	
	public boolean charOffsetInRange(int begin, int end) {
		return (getCharOffset() >= begin && getCharOffset() < end);
	}
	
	public int compareTo(Object obj) {
		Location other = (Location) obj;
		if (other.getLineIndex() == getLineIndex()) {
			return getCharOffset() - other.getCharOffset();
		} else {
			return getLineIndex() - other.getLineIndex();
		}
	}
}
