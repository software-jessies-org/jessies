package e.gui;

import java.awt.*;
import javax.swing.*;

public class EMenu extends Menu {
    public EMenu(String name, AbstractAction[] items) {
        super(name);
        for (int i = 0; i < items.length; i++) {
            if (items[i] != null) {
                MenuItem item = new MenuItem((String) items[i].getValue(Action.NAME));
                item.addActionListener(items[i]);
                add(item);
            } else {
                addSeparator();
            }
        }
    }
}
