package e.edit;

import java.awt.*;
import java.awt.event.*;
import java.text.*;

import javax.swing.*;
import javax.swing.event.*;
import javax.swing.text.*;

import e.util.*;

/**
The ETextArea action that inserts a tab.
*/
public class InsertTabAction extends TextAction {
    public static final String ACTION_NAME = "insert-tab";

    public InsertTabAction() {
        super(ACTION_NAME);
    }

    public void actionPerformed(ActionEvent e) {
        ETextArea target = (ETextArea) getFocusedComponent();
        String tab = Parameters.getParameter("indent.string", "\t");
        target.replaceSelection(tab);
    }
}
