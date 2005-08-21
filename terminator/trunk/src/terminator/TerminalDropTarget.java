package terminator;

import java.awt.datatransfer.*;
import java.awt.dnd.*;
import java.io.File;
import java.util.List;
import e.util.*;
import terminator.view.*;

/**
 * Makes a JTextBuffer a drop target for strings and files. Strings are
 * inserted as if pasted; lists of files are inserted as space-separated
 * lists of their filenames.
 */
public class TerminalDropTarget extends DropTargetAdapter {
	private JTextBuffer textBuffer;

	public TerminalDropTarget(JTextBuffer textBuffer) {
		this.textBuffer = textBuffer;
		new DropTarget(textBuffer, this);
	}
	
	public void drop(DropTargetDropEvent e) {
		try {
			String textToInsert = "";
			Transferable transferable = e.getTransferable();
			if (e.isDataFlavorSupported(DataFlavor.stringFlavor)) {
				e.acceptDrop(DnDConstants.ACTION_COPY);
				textToInsert = (String) transferable.getTransferData(DataFlavor.stringFlavor);
				e.getDropTargetContext().dropComplete(true);
			} else if (e.isDataFlavorSupported(DataFlavor.javaFileListFlavor)) {
				e.acceptDrop(DnDConstants.ACTION_COPY);
				List files = (List) transferable.getTransferData(DataFlavor.javaFileListFlavor);
				textToInsert = makeQuotedFilenameList(files);
				e.getDropTargetContext().dropComplete(true);
			} else {
				e.rejectDrop();
				return;
			}
			textBuffer.getTerminalControl().sendUtf8String(textToInsert);
		} catch (Exception ex) {
			Log.warn("Drop failed.", ex);
			e.rejectDrop();
		}
	}
	
	private String makeQuotedFilenameList(List files) {
		StringBuilder result = new StringBuilder();
		for (Object file : files) {
			String filename = file.toString();
			if (containsQuotableCharacters(filename)) {
				filename = '"' + filename + '"';
			}
			result.append(filename);
			result.append(' ');
		}
		return result.toString();
	}
	
	/**
	 * Tests whether the given string contains any characters that bash(1)
	 * would need to have escaped. Based on sh_backslash_quote in bash 3.0's
	 * "libs/sh/shquote.c".
	 */
	private boolean containsQuotableCharacters(String s) {
		for (int i = 0; i < s.length(); ++i) {
			switch (s.charAt(i)) {
			case ' ': case '\t': case '\n': // IFS whitespace
			case '\'': case '"': case '\\': // quoting chars
			case '|': case '&': case ';': case '(': case ')': case '<': case '>': // shell metacharacters
			case '!': case '{': case '}': // reserved words
			case '*': case '[': case '?': case ']': case '^': // globbing chars
			case '$': case '`': // expansion chars
			case ',': // brace expansion
			case '#': // comment char
				return true;
			default:
				break;
			}
		}
		return false;
	}
}
