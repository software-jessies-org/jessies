package e.gui;

import java.awt.*;
import javax.swing.*;

public class EMenu extends Menu {
    public EMenu(String name, AbstractAction[] items) {
        super(name);
        for (AbstractAction action : items) {
            if (action != null) {
                MenuItem item = new MenuItem((String) action.getValue(Action.NAME));
                item.addActionListener(action);
                add(item);
            } else {
                addSeparator();
            }
        }
    }
}
