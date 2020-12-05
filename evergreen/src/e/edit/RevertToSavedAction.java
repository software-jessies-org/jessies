package e.edit;

import e.gui.*;
import java.awt.event.*;

public class RevertToSavedAction extends ETextAction {
    public RevertToSavedAction() {
        super("_Revert to Saved", null);
        GnomeStockIcon.configureAction(this);
    }
    
    // pn, 2020-12-05
    // I'm disabling this function, because calling 'canRevertToSaved' involves checking file timestamps.
    // While this works OK for some filesystems, due to OS caching, on sshfs it's *extremely* slow, as it
    // doesn't cache anything locally. This wouldn't be a problem if isEnabled() were only called when the
    // menu is opened, but (due presumably to some chain of listeners I've not dug into), this function
    // actually gets called twice for every keypress. So by opening a file which is mounted by sshfs, the
    // speed of typed characters appearing in the text window is directly limited by the RTT to whatever
    // server you mounted the filesystem from.
    // The down-side of always enabling this menu item is merely that someone can try to revert to saved,
    // and be shown that there is no diff. This is acceptable (if not perfect) behaviour, while noticeable
    // lag while typing is definitely not.
    //@Override public boolean isEnabled() {
    //    final ETextWindow textWindow = getFocusedTextWindow();
    //    return (textWindow != null && textWindow.canRevertToSaved());
    //}
    
    public void actionPerformed(ActionEvent e) {
        ETextWindow window = getFocusedTextWindow();
        if (window == null) {
            return;
        }
        window.revertToSaved();
    }
}
