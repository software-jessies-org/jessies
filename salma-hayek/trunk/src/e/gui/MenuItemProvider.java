package e.gui;

import java.awt.event.*;
import java.util.*;
import javax.swing.*;

public interface MenuItemProvider {
    public void provideMenuItems(MouseEvent event, Collection<Action> actions);
}
