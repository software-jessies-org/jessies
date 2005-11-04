package e.debugger;

import com.sun.jdi.*;

/**
 * Opens a given Location in an editor.
 */

public interface LocationOpener {
	
	public void openLocation(Location location);
}
