/*
    Copyright (C) 2004, Elliott Hughes.

    This file is part of KnowAll.

    KnowAll is free software; you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation; either version 2 of the License, or
    (at your option) any later version.

    KnowAll is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with KnowAll; if not, write to the Free Software
    Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA

 */

import e.gui.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.datatransfer.*;

public class ClipboardMonitor {
    private String oldContent = "";

    public ClipboardMonitor(final Component c, final ClipboardListener listener) {
        RepeatingComponentTimer timer = new RepeatingComponentTimer(c, 500, new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                checkClipboard(listener);
            }
        });
        timer.start();
    }

    public void checkClipboard(final ClipboardListener listener) {
        Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
        Transferable transferable = clipboard.getContents(null);
        DataFlavor flavor = DataFlavor.stringFlavor;
        if (transferable.isDataFlavorSupported(flavor) == false) {
            // For now we don't support non-string clipboard contents.
            return;
        }

        try {
            String content = (String) transferable.getTransferData(flavor);
            if (content.equals(oldContent)) {
                return;
            }
            oldContent = content;
            listener.clipboardContentsChangedTo(content);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }
}
