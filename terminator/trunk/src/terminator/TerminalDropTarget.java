package terminator;

import java.awt.datatransfer.*;
import java.awt.dnd.*;
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
			Transferable transferable = e.getTransferable();
			if (e.isDataFlavorSupported(DataFlavor.stringFlavor)) {
				e.acceptDrop(DnDConstants.ACTION_COPY);
				String text = (String) transferable.getTransferData(DataFlavor.stringFlavor);
				e.getDropTargetContext().dropComplete(true);
				textBuffer.insertText(text);
			} else if (e.isDataFlavorSupported(DataFlavor.javaFileListFlavor)) {
				e.acceptDrop(DnDConstants.ACTION_COPY);
				String text = "";
				List files = (List) transferable.getTransferData(DataFlavor.javaFileListFlavor);
				for (int i = 0; i < files.size(); ++i) {
					java.io.File file = (java.io.File) files.get(i);
					if (text.length() > 0) {
						text += ' ';
					}
					text += file.toString();
				}
				e.getDropTargetContext().dropComplete(true);
				textBuffer.insertText(text);
			} else {
				e.rejectDrop();
			}
		} catch (Exception ex) {
			Log.warn("Drop failed.", ex);
			e.rejectDrop();
		}
	}
}
