package terminator.view;

import e.gui.*;
import java.awt.event.*;
import javax.swing.*;
import terminator.*;

public class CursorBlinker implements ActionListener {
	private RepeatingComponentTimer timer;
	private JTextBuffer text;
	
	public CursorBlinker(JTextBuffer text) {
		this.text = text;
		this.timer = new RepeatingComponentTimer(text, 500, this);
	}
	
	public void start() {
		timer.start();
	}
	
	public void stop() {
		timer.stop();
	}
	
	public void actionPerformed(ActionEvent e) {
		if (Options.getSharedInstance().shouldCursorBlink()) {
			text.blinkCursor();
		}
	}
}
