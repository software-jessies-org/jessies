package terminatorn;

import java.awt.event.*;
import javax.swing.*;

public class CursorBlinker implements ActionListener {
	private Timer timer;
	private JTextBuffer text;
	
	public CursorBlinker(JTextBuffer text) {
		this.text = text;
		this.timer = new Timer(500, this);
	}
	
	public void start() {
		timer.start();
	}
	
	public void stop() {
		timer.stop();
	}
	
	public void actionPerformed(ActionEvent e) {
		text.blinkCursor();
	}
}
