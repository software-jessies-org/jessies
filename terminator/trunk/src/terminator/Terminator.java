package terminator;

import java.io.*;
import java.util.*;
import java.util.List;
import javax.swing.*;
import e.util.*;
import terminator.view.*;

public class Terminator {
	private List arguments;
	private ArrayList frames = new ArrayList();
	
	public Terminator(final String[] argumentArray) throws IOException {
		Log.setApplicationName("Terminator");
		arguments = Options.getSharedInstance().parseCommandLine(argumentArray);
		if (arguments.contains("-h") || arguments.contains("-help") || arguments.contains("--help")) {
			showUsage(System.err);
		}
		if (arguments.contains("-v") || arguments.contains("-version") || arguments.contains("--version")) {
			showVersion();
		}
		initUi();
	}
	
	public void frameClosed(TerminatorFrame frame) {
		frames.remove(frame);
		if (frames.size() == 0) {
			System.exit(0);  // Check if on Mac OS X, and exit conditionally.
		}
	}
	
	public void openFrame() {
		JTerminalPaneFactory pane = new JTerminalPaneFactory.Shell();
		TerminatorFrame frame = new TerminatorFrame(this, new JTerminalPaneFactory[] { pane });
		frames.add(frame);
	}
	
	/**
	 * Sets up the user interface on the AWT event thread. I've never
	 * seen this matter in practice, but strictly speaking, you're
	 * supposed to do this.
	 */
	private void initUi() {
		SwingUtilities.invokeLater(new Runnable() {
			public void run() {
				JTerminalPaneFactory[] terminals = getInitialTerminals();
				TerminatorFrame frame = new TerminatorFrame(Terminator.this, terminals);
				frames.add(frame);
			}
		});
	}
	
	private JTerminalPaneFactory[] getInitialTerminals() {
		ArrayList result = new ArrayList();
		String name = null;
		for (int i = 0; i < arguments.size(); ++i) {
			String word = (String) arguments.get(i);
			if (word.equals("-n")) {
				name = (String) arguments.get(++i);
				continue;
			}
			
			String command = word;
			result.add(new JTerminalPaneFactory.Command(command, name));
			name = null;
		}
		
		if (arguments.isEmpty()) {
			result.add(new JTerminalPaneFactory.Shell());
		}
		return (JTerminalPaneFactory[]) result.toArray(new JTerminalPaneFactory[result.size()]);
	}

	public void showUsage(PrintStream out) {
		out.println("Usage: Terminator [--help] [-xrm <resource-string>]... [[-n <name>] command]...");
		out.println();
		out.println("Current resource settings:");
		Options.getSharedInstance().showOptions(out);
		out.println();
		out.println("Terminator will read your .Xdefaults and .Xresources files, and use");
		out.println("resources of class Rxvt, Terminator or XTerm.");
		System.exit(0);
	}
	
	public void showVersion() {
		System.err.println("Terminator 1.1 (3rd June 2004), copyright Phil Norman, Elliott Hughes.");
		System.exit(0);
	}

	public static void main(final String[] arguments) throws IOException {
		new Terminator(arguments);
	}
}
