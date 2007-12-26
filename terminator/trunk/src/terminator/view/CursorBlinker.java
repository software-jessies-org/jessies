package terminator.view;

import e.gui.*;
import java.awt.event.*;
import terminator.*;

public class CursorBlinker implements ActionListener {
	private RepeatingComponentTimer timer;
	private TerminalView view;
	
	public CursorBlinker(TerminalView view) {
		this.view = view;
		this.timer = new RepeatingComponentTimer(view, 500, this);
	}
	
	public void start() {
		timer.start();
	}
	
	public void stop() {
		timer.stop();
	}
	
	public void actionPerformed(ActionEvent e) {
		if (Terminator.getPreferences().getBoolean(TerminatorPreferences.BLINK_CURSOR)) {
			view.blinkCursor();
		}
	}
}
