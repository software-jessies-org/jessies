package terminator;

import java.io.*;
import e.util.*;

/**

@author Phil Norman
*/

public interface JTerminalPaneFactory {
	public JTerminalPane create(Controller controller);
	
	public class Command implements JTerminalPaneFactory {
		private String command;
		private String title;
		
		public Command(String command, String title) {
			this.command = command;
			if (title == null) {
				int spaceIndex = command.indexOf(' ');
				title = ((spaceIndex == -1) ? command : command.substring(0, spaceIndex)).trim();
			}
			this.title = title;
		}
		
		public JTerminalPane create(Controller controller) {
			return new JTerminalPane(controller, title, command, false);
		}
	}
	
	public class Shell implements JTerminalPaneFactory {
		public JTerminalPane create(Controller controller) {
			String user = System.getProperty("user.name");
			String command = getUserShell(user);
			if (Options.getSharedInstance().isLoginShell()) {
				command += " -l";
			}
			return new JTerminalPane(controller, user + "@localhost", command, true);
		}
	
		/**
		* Returns the command to execute as the user's shell, parsed from the /etc/passwd file.
		* On any kind of failure, 'bash' is returned as default.
		*/
		private static String getUserShell(String user) {
			File passwdFile = new File("/etc/passwd");
			if (passwdFile.exists()) {
				BufferedReader in = null;
				try {
					in = new BufferedReader(new FileReader(passwdFile));
					String line;
					while ((line = in.readLine()) != null) {
						if (line.startsWith(user + ":")) {
							return line.substring(line.lastIndexOf(':') + 1);
						}
					}
				} catch (IOException ex) {
					ex.printStackTrace();
				} finally {
					if (in != null) {
						try {
							in.close();
						} catch (IOException ex) {
							Log.warn("Couldn't close file.", ex);
						}
					}
				}
			}
			return "bash";
		}
	}
}
