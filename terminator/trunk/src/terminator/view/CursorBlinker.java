package terminator.view;

import java.awt.event.*;
import javax.swing.*;
import terminator.*;

public class CursorBlinker implements ActionListener {
	private Timer timer;
	private JTextBuffer text;
	
	public CursorBlinker(JTextBuffer text) {
		this.text = text;
		if (Options.getSharedInstance().shouldCursorBlink()) {
			this.timer = new Timer(500, this);
		}
	}
	
	public void start() {
		if (timer != null) {
			timer.start();
		}
	}
	
	public void stop() {
		if (timer != null) {
			timer.stop();
		}
	}
	
	public void actionPerformed(ActionEvent e) {
		text.blinkCursor();
	}
}
