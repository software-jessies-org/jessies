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
		// I know there are people who copy & paste from pop-up menus, but would any of them even know what a terminal emulator is?
		new TerminatorMenuBar.CopyAction(),
		new TerminatorMenuBar.PasteAction(),
		null,
		// These two items don't seem as pertinent as their obviously context-specific relatives below.
		new TerminatorMenuBar.NewShellAction(),
		new TerminatorMenuBar.NewShellTabAction(),
		null,
		new TerminatorMenuBar.NewShellHereAction(),
		new TerminatorMenuBar.NewShellTabHereAction(),
		null,
		new TerminatorMenuBar.CloseAction(),
		null,
		new TerminatorMenuBar.ClearScrollbackAction(),
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
		int codePoint = toCodePoint(selectedText);
		if (codePoint != -1) {
			describeCharacter(actions, codePoint);
		} else {
			actions.add(EPopupMenu.makeInfoItem("  characters: " + selectedText.length()));
		}
		if (selectedLineCount != 0) {
			actions.add(EPopupMenu.makeInfoItem("  lines: " + selectedLineCount));
		}
	}
	
	private int toCodePoint(String s) {
		int result = -1;
		if (s.length() == 1) {
			result = s.codePointAt(0);
		} else if (s.length() >= 6 && s.length() <= 8 && (s.startsWith("\\u") || s.startsWith("U+"))) {
			try {
				result = Integer.parseInt(s.substring(2), 16);
			} catch (NumberFormatException ignored) {
			}
		}
		return Character.isValidCodePoint(result) ? result : -1;
	}
	
	private void describeCharacter(Collection<Action> actions, int codePoint) {
		actions.add(EPopupMenu.makeInfoItem(String.format("  character '%c'", codePoint)));
		actions.add(EPopupMenu.makeInfoItem(String.format("  code point U+%04X", codePoint)));
		Character.UnicodeBlock block = Character.UnicodeBlock.of(codePoint);
		if (block != null) {
			actions.add(EPopupMenu.makeInfoItem("  block " + block));
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
