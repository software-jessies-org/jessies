package terminator.view;

import terminator.*;

public interface JTerminalPaneFactory {
	public JTerminalPane create(TerminalPaneMaster controller);
	
	public class Command implements JTerminalPaneFactory {
		private String command;
		private String title;
		
		public Command(String command, String title) {
			this.command = command;
			this.title = title;
		}
		
		public JTerminalPane create(TerminalPaneMaster controller) {
			return JTerminalPane.newCommandWithTitle(controller, command, title);
		}
	}
	
	public class Shell implements JTerminalPaneFactory {
		public JTerminalPane create(TerminalPaneMaster controller) {
			return JTerminalPane.newShell(controller);
		}
	}
}
