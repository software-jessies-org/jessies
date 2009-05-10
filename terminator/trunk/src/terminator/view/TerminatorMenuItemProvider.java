package terminator.view;

import e.gui.*;
import e.util.*;
import java.awt.datatransfer.*;
import java.awt.event.*;
import java.util.*;
import javax.swing.*;
import terminator.*;

public class TerminatorMenuItemProvider implements MenuItemProvider {
	private final JTerminalPane terminalPane;
	
	private Action[] menuAndKeyActions = new Action[] {
		new TerminatorMenuBar.CopyAction(),
		new TerminatorMenuBar.PasteAction(),
		null,
		new TerminatorMenuBar.NewShellAction(),
		new TerminatorMenuBar.NewShellTabAction(),
		null,
		new TerminatorMenuBar.NewShellHereAction(),
		new TerminatorMenuBar.NewShellTabHereAction(),
		null,
		new TerminatorMenuBar.CloseAction(),
		null,
		new TerminatorMenuBar.FindAction(),
		new TerminatorMenuBar.FindNextAction(),
		new TerminatorMenuBar.FindPreviousAction(),
		null,
		new TerminatorMenuBar.ClearScrollbackAction(),
		null,
		new TerminatorMenuBar.CycleTabAction(1),
		new TerminatorMenuBar.CycleTabAction(-1),
		null,
		new TerminatorMenuBar.ShowInfoAction(),
		new TerminatorMenuBar.ResetAction()
	};
	
	public TerminatorMenuItemProvider(JTerminalPane terminalPane) {
		this.terminalPane = terminalPane;
	}
	
	public void provideMenuItems(MouseEvent e, Collection<Action> actions) {
		actions.addAll(Arrays.asList(menuAndKeyActions));
		addInfoItems(actions);
	}
	
	private void addInfoItems(Collection<Action> actions) {
		// Mac OS doesn't have an X11-like system-wide selection, so we just grab our own selected text directly.
		// Windows can be treated like either here because we're deliberately making it pretend to be retarded, to be like PuTTY.
		String selectedText = GuiUtilities.isMacOs() ? terminalPane.getSelectionHighlighter().getTabbedString() : getSystemSelection();
		addSelectionInfoItems(actions, selectedText);
		EPopupMenu.addNumberInfoItems(actions, selectedText);
	}
	
	private void addSelectionInfoItems(Collection<Action> actions, String selectedText) {
		if (selectedText.length() == 0) {
			return;
		}
		
		int selectedLineCount = 0;
		for (int i = 0; i < selectedText.length(); ++i) {
			if (selectedText.charAt(i) == '\n') {
				++selectedLineCount;
			}
		}
		actions.add(null);
		actions.add(EPopupMenu.makeInfoItem("Selection"));
		actions.add(EPopupMenu.makeInfoItem("  characters: " + selectedText.length()));
		if (selectedLineCount != 0) {
			actions.add(EPopupMenu.makeInfoItem("  lines: " + selectedLineCount));
		}
	}
	
	private String getSystemSelection() {
		String result = "";
		try {
			Clipboard selection = terminalPane.getToolkit().getSystemSelection();
			if (selection == null) {
				selection = terminalPane.getToolkit().getSystemClipboard();
			}
			Transferable transferable = selection.getContents(null);
			result = (String) transferable.getTransferData(DataFlavor.stringFlavor);
		} catch (Exception ex) {
			Log.warn("Couldn't get system selection", ex);
		}
		return result;
	}
}
