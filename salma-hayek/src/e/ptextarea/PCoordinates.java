package e.ptextarea;


/**
 * A PCoordinates contains the coordinates of a character measured in two dimensions,
 * namely the index of the line upon which the character is found, and the offset of that
 * character from the start of the line.
 * 
 * @author Phil Norman
 */

public class PCoordinates {
    private int lineIndex;
    private int charOffset;
    
    public PCoordinates(int lineIndex, int charOffset) {
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
        return "PCoordinates[line=" + lineIndex + ", char=" + charOffset + "]";
    }
}
