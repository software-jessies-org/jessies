package terminator.terminal.escape;

import java.util.*;
import e.util.*;
import terminator.terminal.*;

/**

Stuff we're unsure about:
G - process graphics.

@author Phil Norman
*/

public class EscapeParser {
	private boolean isComplete = false;
	private String sequence = "";

	private SequenceRecognizer seqRecognizer;
	
	private static final HashMap SEQ_RECOGNIZERS = new HashMap();
	static {
		addSequenceRecognizers("6789=>DEHMZcno", new SingleCharSequenceRecognizer());
		addSequenceRecognizers("#()*+$@", new TwoCharSequenceRecognizer());
		addSequenceRecognizers("[", new CSISequenceRecognizer());
		addSequenceRecognizers("]", new XTermSequenceRecognizer());
	}
	private static void addSequenceRecognizers(String chars, SequenceRecognizer recognizer) {
		for (int i = 0; i < chars.length(); i++) {
			SEQ_RECOGNIZERS.put(new Character(chars.charAt(i)), recognizer);
		}
	}
	
	public void addChar(char ch) {
		sequence += ch;
		if (sequence.length() == 1) {
			seqRecognizer = (SequenceRecognizer) SEQ_RECOGNIZERS.get(new Character(ch));
			if (seqRecognizer == null) {
				Log.warn("Unable to find escape sequence end recognizer for start char \"" + ch + "\"");
			}
		}
		isComplete = (seqRecognizer == null) ? true : seqRecognizer.isAtEnd(sequence);
	}
	
	public boolean isComplete() {
		return isComplete;
	}
	
	public TerminalAction getAction(TerminalControl terminalControl) {
//		Log.warn("Getting action for ESC sequence \"" + sequence + "\"");
		return (seqRecognizer == null) ? null : seqRecognizer.getTerminalAction(terminalControl, sequence);
	}
	
	public String toString() {
		return sequence;
	}
	
	private interface SequenceRecognizer {
		public boolean isAtEnd(String sequence);
		public TerminalAction getTerminalAction(TerminalControl terminalControl, String sequence);
	}
	
	private static class SingleCharSequenceRecognizer implements SequenceRecognizer {
		public boolean isAtEnd(String sequence) {
			return (sequence.length() == 1);
		}
		
		public TerminalAction getTerminalAction(TerminalControl terminalControl, String sequence) {
			return new SingleCharEscapeAction(terminalControl, sequence.charAt(0));
		}
	}
	
	private static class TwoCharSequenceRecognizer implements SequenceRecognizer {
		public boolean isAtEnd(String sequence) {
			return (sequence.length() == 2);
		}
		
		public TerminalAction getTerminalAction(TerminalControl terminalControl, String sequence) {
			return new TwoCharEscapeAction(terminalControl, sequence);
		}
	}
	
	private static class CSISequenceRecognizer implements SequenceRecognizer {
		public boolean isAtEnd(String sequence) {
			// We don't need to check for sequence.length() == 0, since SequenceRecognizers are always
			// created after the first char has been read.
			if (sequence.length() == 1) {
				return false;
			}
			char endChar = sequence.charAt(sequence.length() - 1);
			return (endChar < ' ' || endChar >= '@');
		}
		
		public TerminalAction getTerminalAction(TerminalControl terminalControl, String sequence) {
			return new CSIEscapeAction(terminalControl, sequence);
		}
	}
	
	private static class XTermSequenceRecognizer implements SequenceRecognizer {
		private boolean isInInitialNumber = true;

		public boolean isAtEnd(String sequence) {
			// We don't need to check for sequence.length() == 0, since SequenceRecognizers are always
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
		
		public TerminalAction getTerminalAction(TerminalControl terminalControl, String sequence) {
			return new XTermEscapeAction(sequence);
		}
	}
}
