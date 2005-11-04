package e.debugger;

import java.util.*;

/**
 * A listener informed when execution in the debug target VM is suspended or resumed.
 */

public interface VmSuspensionListener extends EventListener {
    
    /**
     * The VM has suspended execution.
     */
    public void vmSuspended();
    
    /**
     * VM execution has resumed.
     */
    public void vmResumed();
}
