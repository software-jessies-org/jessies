package terminator.view;

import terminator.*;

public interface JTerminalPaneFactory {
	public JTerminalPane create(TerminalPaneMaster controller);
	
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
		
		public JTerminalPane create(TerminalPaneMaster controller) {
			return new JTerminalPane(controller, title, command, false);
		}
	}
	
	public class Shell implements JTerminalPaneFactory {
		public JTerminalPane create(TerminalPaneMaster controller) {
			return JTerminalPane.newShell(controller);
		}
	}
}
