package terminatorn;

/**
A Location holds the line index, and character offset within the line of a particular character.

@author Phil Norman
*/

public class Location {
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
}
