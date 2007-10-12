package terminator;

import java.awt.datatransfer.*;
import java.awt.dnd.*;
import java.io.File;
import java.net.*;
import java.util.*;
import e.util.*;
import terminator.view.*;

/**
 * Makes a TerminalView a drop target for strings and files.
 * Strings are inserted as if pasted.
 * Lists of files are inserted as space-separated lists of their filenames.
 */
public class TerminalDropTarget extends DropTargetAdapter {
	private static DataFlavor uriListFlavor;
	static {
		try {
			uriListFlavor = new DataFlavor("text/uri-list;class=java.lang.String");
		} catch (Exception ex) {
			// Can't happen.
		}
	}
	
	private TerminalView view;

	public TerminalDropTarget(TerminalView view) {
		this.view = view;
		new DropTarget(view, this);
	}
	
	public void drop(DropTargetDropEvent e) {
		try {
			String textToInsert = "";
			Transferable transferable = e.getTransferable();
			if (e.isDataFlavorSupported(uriListFlavor)) {
				// Sun 4899516: without support for text/uri-list, we can't accept files dropped from Nautilus on Linux.
				e.acceptDrop(DnDConstants.ACTION_COPY);
				List<String> files = translateUriStringToFileList((String) transferable.getTransferData(uriListFlavor));
				textToInsert = makeQuotedFilenameList(files);
			} else if (e.isDataFlavorSupported(DataFlavor.javaFileListFlavor)) {
				e.acceptDrop(DnDConstants.ACTION_COPY);
				List<?> files = (List<?>) transferable.getTransferData(DataFlavor.javaFileListFlavor);
				textToInsert = makeQuotedFilenameList(files);
			} else if (e.isDataFlavorSupported(DataFlavor.stringFlavor)) {
				e.acceptDrop(DnDConstants.ACTION_COPY);
				textToInsert = (String) transferable.getTransferData(DataFlavor.stringFlavor);
			} else {
				e.rejectDrop();
				return;
			}
			e.getDropTargetContext().dropComplete(true);
			view.getTerminalControl().sendUtf8String(textToInsert);
		} catch (Exception ex) {
			Log.warn("Drop failed.", ex);
			e.rejectDrop();
		}
	}
	
	private String makeQuotedFilenameList(List<? extends Object> files) {
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
	
	// Sun 4899516: we need to translate a URI list (represented as a String) manually.
	private List<String> translateUriStringToFileList(String uriList) {
		ArrayList<String> result = new ArrayList<String>();
		for (String uri : uriList.split("\r\n")) {
			if (uri.startsWith("#")) {
				continue;
			}
			if (uri.startsWith("file:")) {
				// If the URI represents a file, the filename is likely to be more useful at the shell.
				try {
					// Work around the fact that Apple (2007-10-11) supply "localhost" and Sun insist on no authority.
					uri = new File(new URI(uri.replaceAll("^file://localhost/", "file:///"))).toString();
				} catch (Exception ex) {
					// If there's something File/URI didn't like about it, just fall through and use it as-is.
				}
			}
			result.add(uri);
		}
		return result;
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
