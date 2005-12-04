package terminator;

import com.apple.eawt.*;
import e.gui.*;
import e.util.*;
import java.awt.EventQueue;
import java.io.*;
import java.util.*;
import java.util.List;
import javax.swing.*;
import terminator.view.*;

public class Terminator {
	private static final Terminator INSTANCE = new Terminator();
	private List<String> arguments;
	private ArrayList<TerminatorFrame> frames = new ArrayList<TerminatorFrame>();
	
	public static Terminator getSharedInstance() {
		return INSTANCE;
	}
	
	private Terminator() {
		Log.setApplicationName("Terminator");
		Application application = new Application();
		application.addApplicationListener(new ApplicationAdapter() {
			public void handleReOpenApplication(ApplicationEvent e) {
				if (frames.size() == 0) {
					openFrame();
				}
				e.setHandled(true);
			}
			public void handleQuit(ApplicationEvent e) {
				boolean quit = true;
				if (frames.size() != 0) {
					quit = SimpleDialog.askQuestion(frames.get(0), "Terminator", "You have " + StringUtilities.pluralize(frames.size(), "window", "windows") + " which may contain running processes. Do you want to quit and risk terminating these processes?", "Quit");
				}
				if (quit) {
					e.setHandled(true);
				}
			}
		});
	}
	
	private void startTerminatorServer() {
		String display = System.getenv("DISPLAY");
		if (display == null) {
			display = "";
		}
		new InAppServer("Terminator", "~/.terminal-logs/.terminator-server-port" + display, TerminatorServer.class, new TerminatorServer());
	}
	
	/**
	 * Ensures that, on Mac OS, we always have our menu bar visible, even
	 * when there are no terminal windows open. We use a dummy window with
	 * a copy of the menu bar attached. When no other window has the focus,
	 * but the application is focused, this hidden window gets the focus,
	 * and its menu is used for the screen menu bar.
	 */
	private void initMenuBar() {
		JFrame hiddenFrame = new JFrame("Mac OS implementation detail");
		hiddenFrame.setJMenuBar(new TerminatorMenuBar());
		hiddenFrame.setUndecorated(true);
		hiddenFrame.setVisible(true);
	}
	
	// Returns whether we started the UI.
	public boolean parseCommandLine(final String[] argumentArray, PrintWriter out, PrintWriter err) throws IOException {
		arguments = Options.getSharedInstance().parseCommandLine(argumentArray);
		if (arguments.contains("-h") || arguments.contains("-help") || arguments.contains("--help")) {
			showUsage(out);
		} else if (arguments.contains("-v") || arguments.contains("-version") || arguments.contains("--version")) {
			showVersion(err);
		} else {
			initUi();
			return true;
		}
		return false;
	}

	private void parseOriginalCommandLine(final String[] argumentArray, PrintWriter out, PrintWriter err) throws IOException {
		if (parseCommandLine(argumentArray, out, err)) {
			startTerminatorServer();
			if (GuiUtilities.isMacOs()) {
				initMenuBar();
			}
		}
	}
	
	public void frameClosed(TerminatorFrame frame) {
		frames.remove(frame);
		if (frames.size() == 0 && GuiUtilities.isMacOs() == false) {
			System.exit(0);
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
		EventQueue.invokeLater(new Runnable() {
			public void run() {
				JTerminalPaneFactory[] terminals = getInitialTerminals();
				TerminatorFrame frame = new TerminatorFrame(Terminator.this, terminals);
				frames.add(frame);
			}
		});
	}
	
	private JTerminalPaneFactory[] getInitialTerminals() {
		ArrayList<JTerminalPaneFactory> result = new ArrayList<JTerminalPaneFactory>();
		String name = null;
		for (int i = 0; i < arguments.size(); ++i) {
			String word = arguments.get(i);
			if (word.equals("-n")) {
				name = arguments.get(++i);
				continue;
			}
			
			String command = word;
			result.add(new JTerminalPaneFactory.Command(command, name));
			name = null;
		}
		
		if (arguments.isEmpty()) {
			result.add(new JTerminalPaneFactory.Shell());
		}
		return result.toArray(new JTerminalPaneFactory[result.size()]);
	}

	public void showUsage(PrintWriter out) {
		out.println("Usage: Terminator [--help] [-xrm <resource-string>]... [[-n <name>] command]...");
		out.println();
		out.println("Current resource settings:");
		Options.getSharedInstance().showOptions(out);
		out.println();
		out.println("Terminator will read your .Xdefaults and .Xresources files, and use");
		out.println("resources of class Rxvt, Terminator or XTerm.");
	}
	
	public void showVersion(PrintWriter out) {
		out.println("Terminator (see ChangeLog for author and version information)");
		out.println("Copyright (C) 2004-2005 Free Software Foundation, Inc.");
		out.println("This is free software; see the source for copying conditions.  There is NO");
		out.println("warranty; not even for MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.");
	}

	public static void main(final String[] arguments) {
		EventQueue.invokeLater(new Runnable() {
			public void run() {
				try {
					GuiUtilities.initLookAndFeel();
					PrintWriter outWriter = new PrintWriter(System.out);
					PrintWriter errWriter = new PrintWriter(System.err);
					Terminator.getSharedInstance().parseOriginalCommandLine(arguments, outWriter, errWriter);
					outWriter.flush();
					errWriter.flush();
				} catch (Throwable th) {
					Log.warn("Couldn't start Terminator.", th);
					System.exit(1);
				}
			}
		});
	}
}
