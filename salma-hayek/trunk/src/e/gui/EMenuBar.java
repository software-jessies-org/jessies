package e.gui;

import java.awt.*;

public class EMenuBar extends MenuBar {
    public EMenuBar(EMenu[] menus) {
        for (int i = 0; i < menus.length; i++) {
            this.add(menus[i]);
        }
    }
}
