package terminator.view;

import javax.swing.*;
import javax.swing.event.*;

/**

@author Phil Norman
*/

public class StickyBottomScrollBarListener implements ChangeListener {
	private BoundedRangeModel model;
	private boolean wasAtBottom = true;
	private int maximum;
	private int extent;
	private int value;
	
	public StickyBottomScrollBarListener(JScrollBar bar) {
		model = bar.getModel();
		model.addChangeListener(this);
		updateValues();
		wasAtBottom = calculateBottomness();
	}
	
	private void updateValues() {
		maximum = model.getMaximum();
		extent = model.getExtent();
		value = model.getValue();
	}
	
	public void stateChanged(ChangeEvent event) {
		if (model.getMaximum() != maximum || model.getExtent() != extent) {
			updateValues();
			if (wasAtBottom) {
				scrollToBottom();
			}
			wasAtBottom = calculateBottomness();
		} else if (model.getValue() != value) {
			updateValues();
			wasAtBottom = calculateBottomness();
		} else {
			updateValues();
		}
	}
	
	private void scrollToBottom() {
		model.setValue(maximum - extent);
	}
	
	private boolean calculateBottomness() {
		return (value + extent == maximum);
	}
}
