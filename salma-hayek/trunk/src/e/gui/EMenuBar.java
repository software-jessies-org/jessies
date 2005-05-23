package e.gui;

import java.awt.*;

public class EMenuBar extends MenuBar {
    public EMenuBar(EMenu[] menus) {
        for (EMenu menu : menus) {
            add(menu);
        }
    }
}
