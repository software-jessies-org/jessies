package terminatorn;

import java.awt.*;
import java.awt.event.*;

import javax.swing.*;

/**

@author Phil Norman
*/

public class StringEntryDialog extends JDialog {
	private JTextField textField = new JTextField(20);
	private boolean cancelled;
	
	public StringEntryDialog(Component component, String title) {
		super(JOptionPane.getFrameForComponent(component), title, true);
		JPanel panel = new JPanel(new BorderLayout());
		panel.add(textField, BorderLayout.CENTER);
		panel.add(getButtonPanel(), BorderLayout.SOUTH);
		getContentPane().add(panel);
	}

	public static String getString(Component component, String title) {
		StringEntryDialog dialog = new StringEntryDialog(component, title);
		dialog.pack();
		dialog.setLocationRelativeTo(component);
		dialog.show();
		if (dialog.wasCancelled()) {
			return null;
		} else {
			return dialog.getString();
		}
	}
	
	public String getString() {
		return textField.getText();
	}
	
	public boolean wasCancelled() {
		return cancelled;
	}
	
	public Component getButtonPanel() {
		JButton okButton = getButton("OK", new ActionListener() {
			public void actionPerformed(ActionEvent event) {
				okAction();
			}
		});
		getRootPane().setDefaultButton(okButton);
		return getButtonPanel(new JButton[] {
			okButton,
			getButton("Cancel", new ActionListener() {
				public void actionPerformed(ActionEvent event) {
					cancelAction();
				}
			}),
		});
	}
	
	public void okAction() {
		cancelled = false;
		hide();
	}

	public void cancelAction() {
		cancelled = true;
		hide();
	}

	private JButton getButton(String text, ActionListener listener) {
		JButton result = new JButton(text);
		result.addActionListener(listener);
		return result;
	}
	
	private JPanel getButtonPanel(JButton[] buttons) {
		JPanel result = new JPanel(new FlowLayout(FlowLayout.RIGHT));
		JPanel buttonContainer = new JPanel(new GridLayout(1, buttons.length, 5, 0));
		for (int i = 0; i < buttons.length; i++) {
			buttonContainer.add(buttons[i]);
		}
		result.add(buttonContainer);
		return result;
	}
}
