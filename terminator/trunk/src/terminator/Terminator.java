package terminator;

import com.apple.eawt.*;
import e.gui.*;
import e.util.*;
import java.awt.EventQueue;
import java.io.*;
import java.net.*;
import java.util.*;
import java.util.List;
import javax.swing.*;
import terminator.view.*;

public class Terminator {
	private static final Terminator INSTANCE = new Terminator();
	
	private List<String> arguments;
	private Frames frames = new Frames();
	
	public static Terminator getSharedInstance() {
		return INSTANCE;
	}
	
	private Terminator() {
		initAboutBox();
		initMacOsEventHandlers();
	}
	
	private void initMacOsEventHandlers() {
		if (GuiUtilities.isMacOs() == false) {
			return;
		}
		
		Application.getApplication().setEnabledPreferencesMenu(true);
		Application.getApplication().addApplicationListener(new ApplicationAdapter() {
			@Override
			public void handleReOpenApplication(ApplicationEvent e) {
				if (frames.isEmpty()) {
					openFrame(JTerminalPane.newShell());
				}
				e.setHandled(true);
			}
			
			@Override
			public void handleOpenFile(ApplicationEvent e) {
				SimpleDialog.showAlert(null, "Received 'open file' AppleEvent", e.toString());
				Log.warn("open file " + e.toString());
			}
			
			@Override
			public void handlePreferences(ApplicationEvent e) {
				Options.getSharedInstance().showPreferencesDialog(null);
				e.setHandled(true);
			}
			
			@Override
			public void handleQuit(ApplicationEvent e) {
				// We can't iterate over "frames" directly because we're causing frames to close and be removed from the list.
				for (TerminatorFrame frame : frames.toArrayList()) {
					frame.handleWindowCloseRequestFromUser();
				}
				
				// If there are windows still open, the user changed their mind; otherwise quit.
				e.setHandled(frames.isEmpty());
			}
		});
	}
	
	private void initAboutBox() {
		AboutBox aboutBox = AboutBox.getSharedInstance();
		aboutBox.setWebSiteAddress("http://software.jessies.org/terminator/");
		aboutBox.addCopyright("Copyright (C) 2004-2007 Free Software Foundation, Inc.");
		aboutBox.addCopyright("All Rights Reserved.");
	}
	
	private void startTerminatorServer() {
		InetAddress loopbackAddress = null;
		try {
			loopbackAddress = InetAddress.getByName(null);
		} catch (UnknownHostException ex) {
			Log.warn("Problem looking up the loopback address", ex);
		}
		new InAppServer("Terminator", System.getProperty("org.jessies.terminator.serverPortFileName"), loopbackAddress, TerminatorServer.class, new TerminatorServer());
	}
	
	// Returns whether we started the UI.
	public boolean parseCommandLine(final String[] argumentArray, PrintWriter out, PrintWriter err) throws IOException {
		arguments = Options.getSharedInstance().parseCommandLine(argumentArray);
		if (arguments.contains("-h") || arguments.contains("-help") || arguments.contains("--help")) {
			showUsage(out);
		} else {
			initUi();
			return true;
		}
		return false;
	}

	private void parseOriginalCommandLine(final String[] argumentArray, PrintWriter out, PrintWriter err) throws IOException {
		if (parseCommandLine(argumentArray, out, err)) {
			startTerminatorServer();
		}
	}
	
	public Frames getFrames() {
		return frames;
	}
	
	public void openFrame(JTerminalPane terminalPane) {
		new TerminatorFrame(Collections.singletonList(terminalPane));
	}
	
	/**
	 * Sets up the user interface on the AWT event thread.
	 */
	private void initUi() {
		EventQueue.invokeLater(new Runnable() {
			public void run() {
				new TerminatorFrame(getInitialTerminals());
			}
		});
	}
	
	/**
	 * Invoked by the preferences dialog whenever an option is changed.
	 */
	public void optionsDidChange() {
		// On the Mac, the Command key (called 'meta' by Java) is always used for keyboard equivalents.
		// On other systems, Control tends to be used, but in the special case of terminal emulators this conflicts with the ability to type control characters.
		// The traditional work-around has always been to use Alt, which -- conveniently for Mac users -- is in the same place on a PC keyboard as Command on a Mac keyboard.
		// Things are complicated if we want to support emacs(1), which uses the alt key as meta.
		// Things are complicated in a different direction if we want to support input methods that use alt.
		// At the moment, we assume that Linux users who want characters not on their keyboard will switch keyboard mapping dynamically (which works fine).
		// We can avoid the question on Mac OS for now because disabling input methods doesn't currently work properly, and we don't get the key events anyway.
		if (GuiUtilities.isMacOs() == false) {
			GuiUtilities.setDefaultKeyStrokeModifier(Options.getSharedInstance().shouldUseAltKeyAsMeta() ? java.awt.event.KeyEvent.SHIFT_MASK | java.awt.event.KeyEvent.CTRL_MASK : java.awt.event.KeyEvent.ALT_MASK);
		}
		
		for (int i = 0; i < frames.size(); ++i) {
			frames.get(i).optionsDidChange();
		}
	}
	
	private List<JTerminalPane> getInitialTerminals() {
		ArrayList<JTerminalPane> result = new ArrayList<JTerminalPane>();
		String name = null;
		String workingDirectory = null;
		for (int i = 0; i < arguments.size(); ++i) {
			String word = arguments.get(i);
			if (word.equals("-n")) {
				name = arguments.get(++i);
				continue;
			}
			if (word.equals("--working-directory")) {
				workingDirectory = arguments.get(++i);
				continue;
			}
			
			// We can't hope to imitate the shell's parsing of a string, so pass it unmolested to the shell.
			String command = word;
			result.add(JTerminalPane.newCommandWithName(command, name, workingDirectory));
			name = null;
		}
		
		if (result.isEmpty()) {
			result.add(JTerminalPane.newShellWithName(name, workingDirectory));
		}
		return result;
	}

	private static void showUsage(Appendable out) throws IOException {
		out.append("Usage: terminator [--help] [-xrm <resource-string>]... [[-n <name>] [--working-directory <directory>] [<command>]]...\n");
		out.append("\n");
		out.append("Current resource settings:\n");
		Options.getSharedInstance().showOptions(out, true);
		out.append("\n");
		out.append("Terminator only uses resources of class Terminator and only from the command line, not from your .Xdefaults and .Xresources files.\n");
	}
	
	public static void main(final String[] arguments) {
		EventQueue.invokeLater(new Runnable() {
			public void run() {
				try {
					GuiUtilities.initLookAndFeel();
					Terminator.getSharedInstance().optionsDidChange();
					
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
