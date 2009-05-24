package terminator;

import com.apple.eawt.*;
import e.gui.*;
import e.util.*;
import java.awt.*;
import java.awt.event.KeyEvent;
import java.io.*;
import java.net.*;
import java.util.*;
import java.util.List;
import terminator.view.*;

public class Terminator {
	private static final Terminator INSTANCE = new Terminator();
	
	private TerminatorPreferences preferences;
	private Color boldForegroundColor;
	
	private List<String> arguments;
	private Frames frames = new Frames();
	
	public static Terminator getSharedInstance() {
		return INSTANCE;
	}
	
	public static TerminatorPreferences getPreferences() {
		return INSTANCE.preferences;
	}
	
	private Terminator() {
		initPreferences();
		initAboutBox();
		initMacOsEventHandlers();
	}
	
	private void initPreferences() {
		preferences = new TerminatorPreferences();
		preferences.addPreferencesListener(new Preferences.Listener() {
			public void preferencesChanged() {
				optionsDidChange();
			}
		});
		preferences.readFromDisk();
	}
	
	public Color getBoldColor() {
		return boldForegroundColor;
	}
	
	private void initMacOsEventHandlers() {
		if (GuiUtilities.isMacOs() == false) {
			return;
		}
		
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
		aboutBox.addCopyright("Copyright (C) 2004-2009 software.jessies.org team.");
		aboutBox.addCopyright("All Rights Reserved.");
		aboutBox.setLicense(AboutBox.License.GPL_2_OR_LATER);
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
	public boolean parseCommandLine(final String[] argumentArray, PrintWriter out) {
		// Ignore "-xrm <resource-string>" argument pairs.
		this.arguments = new ArrayList<String>();
		for (int i = 0; i < argumentArray.length; ++i) {
			if (argumentArray[i].equals("-xrm")) {
				// FIXME: we want the ability to override preferences on a per-terminal (or just per-window?) basis. GNOME Terminal works around this by letting each terminal choose a "profile", rather than offering the ability to override arbitrary preferences.
				//String resourceString = arguments[++i];
				//processResourceString(resourceString);
			} else {
				arguments.add(argumentArray[i]);
			}
		}
		
		if (arguments.contains("-h") || arguments.contains("-help") || arguments.contains("--help")) {
			showUsage(out);
		} else {
			initUi();
			return true;
		}
		return false;
	}

	private void parseOriginalCommandLine(final String[] argumentArray, PrintWriter out) {
		if (parseCommandLine(argumentArray, out)) {
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
	 * Invoked (via our Preferences.Listener, above) by the preferences dialog whenever an option is changed.
	 */
	private void optionsDidChange() {
		boldForegroundColor = Palettes.getBrightColorFor(preferences.getColor(TerminatorPreferences.FOREGROUND_COLOR));
		
		// On the Mac, the Command key (called 'meta' by Java) is always used for keyboard equivalents.
		// On other systems, Control tends to be used, but in the special case of terminal emulators this conflicts with the ability to type control characters.
		// The traditional work-around has always been to use Alt, which -- conveniently for Mac users -- is in the same place on a PC keyboard as Command on a Mac keyboard.
		// Things are complicated if we want to support emacs(1), which uses the alt key as meta.
		// Things are complicated in a different direction if we want to support input methods that use alt.
		// At the moment, we assume that Linux users who want characters not on their keyboard will switch keyboard mapping dynamically (which works fine).
		// We can avoid the question on Mac OS for now because disabling input methods doesn't currently work properly, and we don't get the key events anyway.
		if (GuiUtilities.isMacOs() == false) {
			final boolean useAltAsMeta = preferences.getBoolean(TerminatorPreferences.USE_ALT_AS_META);
			int modifiers = KeyEvent.ALT_MASK;
			if (useAltAsMeta) {
				modifiers = KeyEvent.SHIFT_MASK | KeyEvent.CTRL_MASK;
			}
			TerminatorMenuBar.setDefaultKeyStrokeModifiers(modifiers);
			// When useAltAsMeta is true, we want Alt-F to go to Emacs.
			// When useAltAsMeta is false, we want Alt-F to invoke the Find action.
			// In neither case do we want Alt-F to open the File menu.
			GuiUtilities.setMnemonicsEnabled(false);
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
			if (word.equals("-n") || word.equals("-T")) {
				name = arguments.get(++i);
				continue;
			}
			if (word.equals("--working-directory")) {
				workingDirectory = arguments.get(++i);
				continue;
			}
			if (word.equals("-e")) {
				List<String> argV = arguments.subList(++i, arguments.size());
				if (argV.isEmpty()) {
					showUsage(System.err);
					System.exit(1);
				}
				result.add(JTerminalPane.newCommandWithArgV(name, workingDirectory, argV));
				break;
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
	
	private static void showUsage(Appendable out) {
		try {
			GuiUtilities.finishGnomeStartup();
			out.append("Usage: terminator [--help] [[-n <name>] [--working-directory <directory>] [<command>]]...\n");
		} catch (IOException ex) {
			// Who cares?
		}
	}
	
	public static void main(final String[] arguments) {
		EventQueue.invokeLater(new Runnable() {
			public void run() {
				try {
					GuiUtilities.initLookAndFeel();
					Terminator.getSharedInstance().optionsDidChange();
					
					PrintWriter outWriter = new PrintWriter(System.out);
					Terminator.getSharedInstance().parseOriginalCommandLine(arguments, outWriter);
					outWriter.flush();
				} catch (Throwable th) {
					Log.warn("Couldn't start Terminator.", th);
					System.exit(1);
				}
			}
		});
	}
}
