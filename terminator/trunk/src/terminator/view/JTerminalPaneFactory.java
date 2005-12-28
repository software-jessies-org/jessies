package terminator.view;

/**
 * FIXME: do we need this class? Why can't we work directly with JTerminalPane instances?
 */
public interface JTerminalPaneFactory {
	public JTerminalPane create();
	
	public static class Command implements JTerminalPaneFactory {
		private String command;
		private String title;
		
		public Command(String command, String title) {
			this.command = command;
			this.title = title;
		}
		
		public JTerminalPane create() {
			return JTerminalPane.newCommandWithTitle(command, title);
		}
	}
	
	public static class Shell implements JTerminalPaneFactory {
		public JTerminalPane create() {
			return JTerminalPane.newShell();
		}
	}
}
