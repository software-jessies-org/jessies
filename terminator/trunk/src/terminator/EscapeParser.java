package terminatorn;

import java.util.*;

import terminatorn.escape.*;

/**

Stuff we're unsure about:
G - process graphics.

@author Phil Norman
*/

public class EscapeParser {
	private boolean isComplete = false;
	private String sequence = "";

	private SequenceRecogniser seqRecogniser;
	
	private static final HashMap SEQ_RECOGNISERS = new HashMap();
	static {
		addSequenceRecognisers("6789=>DEHMZcno", new SingleCharSequenceRecogniser());
		addSequenceRecognisers("#()*+$@", new TwoCharSequenceRecogniser());
		addSequenceRecognisers("[", new CSISequenceRecogniser());
		addSequenceRecognisers("]", new XTermSequenceRecogniser());
	}
	private static void addSequenceRecognisers(String chars, SequenceRecogniser recogniser) {
		for (int i = 0; i < chars.length(); i++) {
			SEQ_RECOGNISERS.put(new Character(chars.charAt(i)), recogniser);
		}
	}
	
	public void addChar(char ch) {
		sequence += ch;
		if (sequence.length() == 1) {
			seqRecogniser = (SequenceRecogniser) SEQ_RECOGNISERS.get(new Character(ch));
			if (seqRecogniser == null) {
				Log.warn("Unable to find escape sequence end recogniser for start char \"" + ch + "\"");
			}
		}
		isComplete = (seqRecogniser == null) ? true : seqRecogniser.isAtEnd(sequence);
	}
	
	public boolean isComplete() {
		return isComplete;
	}
	
	public TelnetAction getAction(TelnetControl telnetControl) {
//		Log.warn("Getting action for ESC sequence \"" + sequence + "\"");
		return (seqRecogniser == null) ? null : seqRecogniser.getTelnetAction(telnetControl, sequence);
	}
	
	public String toString() {
		return sequence;
	}
	
	private interface SequenceRecogniser {
		public boolean isAtEnd(String sequence);
		public TelnetAction getTelnetAction(TelnetControl telnetControl, String sequence);
	}
	
	private static class SingleCharSequenceRecogniser implements SequenceRecogniser {
		public boolean isAtEnd(String sequence) {
			return (sequence.length() == 1);
		}
		
		public TelnetAction getTelnetAction(TelnetControl telnetControl, String sequence) {
			return new SingleCharEscapeAction(sequence.charAt(0));
		}
	}
	
	private static class TwoCharSequenceRecogniser implements SequenceRecogniser {
		public boolean isAtEnd(String sequence) {
			return (sequence.length() == 2);
		}
		
		public TelnetAction getTelnetAction(TelnetControl telnetControl, String sequence) {
			return new TwoCharEscapeAction(sequence);
		}
	}
	
	private static class CSISequenceRecogniser implements SequenceRecogniser {
		public boolean isAtEnd(String sequence) {
			// We don't need to check for sequence.length() == 0, since SequenceRecognisers are always
			// created after the first char has been read.
			if (sequence.length() == 1) {
				return false;
			}
			char endChar = sequence.charAt(sequence.length() - 1);
			return (endChar < ' ' || endChar >= '@');
		}
		
		public TelnetAction getTelnetAction(TelnetControl telnetControl, String sequence) {
			return new CSIEscapeAction(telnetControl, sequence);
		}
	}
	
	private static class XTermSequenceRecogniser implements SequenceRecogniser {
		private boolean isInInitialNumber = true;

		public boolean isAtEnd(String sequence) {
			// We don't need to check for sequence.length() == 0, since SequenceRecognisers are always
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
		
		public TelnetAction getTelnetAction(TelnetControl telnetControl, String sequence) {
			return new XTermEscapeAction(sequence);
		}
	}
}
