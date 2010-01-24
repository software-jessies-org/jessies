package e.util;

import java.util.*;
import org.jessies.test.*;

/**
 * Compares strings. Case-insensitive comparisons (the default) fall back to case-sensitive comparisons for tie-breaking.
 * This scheme ensures deterministic ordering.
 */
public final class SmartStringComparator implements Comparator<String> {
    private boolean caseSensitive;
    
    public SmartStringComparator caseSensitive(boolean caseSensitive) {
        this.caseSensitive = caseSensitive;
        return this;
    }
    
    public boolean caseSensitive() {
        return caseSensitive;
    }
    
    public int compare(String lhs, String rhs) {
        // FIXME: add an option to not sort spaces first (by ignoring spaces, as in dictionaries)?
        // FIXME: add an option to sort "foo11" and "foo2" as "foo2", "foo11" (by recognizing embedded numbers)?
        int result = 0;
        if (!caseSensitive) {
            result = lhs.compareToIgnoreCase(rhs);
        }
        // If case-insensitive comparison didn't help (or wasn't tried), fall back to case-sensitive comparison.
        if (result == 0) {
            result = lhs.compareTo(rhs);
        }
        return result;
    }
    
    @Test private static void testTieBreaking() {
        Assert.lt(new SmartStringComparator().compare("hello_A", "hello_a"), 0);
        Assert.gt(new SmartStringComparator().compare("hello_a", "hello_A"), 0);
        Assert.equals(new SmartStringComparator().compare("hello_a", "hello_a"), 0);
    }
    
    @Test private static void testCaseSensitivity() {
        Assert.lt(new SmartStringComparator().caseSensitive(true).compare("Main", "init"), 0);
        Assert.gt(new SmartStringComparator().caseSensitive(false).compare("Main", "init"), 0);
    }
}
