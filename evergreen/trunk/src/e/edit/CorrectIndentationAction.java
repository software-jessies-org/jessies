package e.edit;

import java.awt.*;
import java.awt.event.*;
import java.text.*;

import javax.swing.*;
import javax.swing.event.*;
import javax.swing.text.*;

import e.util.*;

/**
The ETextArea action that automatically corrects the current line's indentation.
*/
public class CorrectIndentationAction extends TextAction {
    public static final String ACTION_NAME = "correct-indentation";

    public CorrectIndentationAction() {
        super(ACTION_NAME);
    }
    
    public void actionPerformed(ActionEvent e) {
        ETextArea target = (ETextArea) getFocusedComponent();
        target.correctIndentation();
    }
}
