package terminatorn;

import java.util.*;

/**

Stuff we're unsure about:
G - process graphics.

@author Phil Norman
*/

public class EscapeParser {
	private boolean isComplete = false;
	private String sequence = "";

	private EndRecogniser endRecogniser;
	
	private static final HashMap END_RECOGNISERS = new HashMap();
	static {
		addSequenceEndRecognisers("6789=>DEHMZcno", new LengthEndRecogniser(1));
		addSequenceEndRecognisers("#()*+@", new LengthEndRecogniser(2));
		addSequenceEndRecognisers("[", new CSIEndRecogniser());
		addSequenceEndRecognisers("]", new XTermEndRecogniser());
	}
	private static void addSequenceEndRecognisers(String chars, EndRecogniser recogniser) {
		for (int i = 0; i < chars.length(); i++) {
			END_RECOGNISERS.put(new Character(chars.charAt(i)), recogniser);
		}
	}
	
	public void addChar(char ch) {
		sequence += ch;
		if (sequence.length() == 1) {
			endRecogniser = (EndRecogniser) END_RECOGNISERS.get(new Character(ch));
			if (endRecogniser == null) {
				Log.warn("Unable to find escape sequence end recogniser for start char \"" + ch + "\"");
			}
		}
		isComplete = (endRecogniser == null) ? true : endRecogniser.isAtEnd(sequence);
	}
	
	public boolean isComplete() {
		return isComplete;
	}
	
	public String toString() {
		return sequence;
	}
	
	private interface EndRecogniser {
		public boolean isAtEnd(String sequence);
	}
	
	private static class LengthEndRecogniser implements EndRecogniser {
		private int length;
	
		public LengthEndRecogniser(int length) {
			this.length = length;
		}
		
		public boolean isAtEnd(String sequence) {
			return (sequence.length() == length);
		}
	}
	
	private static class CSIEndRecogniser implements EndRecogniser {
		public boolean isAtEnd(String sequence) {
			// We don't need to check for sequence.length() == 0, since EndRecognisers are always
			// created after the first char has been read.
			if (sequence.length() == 1) {
				return false;
			}
			char endChar = sequence.charAt(sequence.length() - 1);
			return (endChar < ' ' || endChar >= '@');
		}
	}
	
	private static class XTermEndRecogniser implements EndRecogniser {
		private boolean isInInitialNumber = true;

		public boolean isAtEnd(String sequence) {
			// We don't need to check for sequence.length() == 0, since EndRecognisers are always
			// created after the first char has been read.
			if (sequence.length() == 1) {
				return false;
			}
			char endChar = sequence.charAt(sequence.length() - 1);
			if (endChar < ' ') {
				return true;
			}
			if (isInInitialNumber) {
				if (endChar == ';') {
					isInInitialNumber = false;
					return false;
				} else if (Character.isDigit(endChar) == false) {
					return true;
				}
			}
			return (endChar < ' ');
		}
	}
}
