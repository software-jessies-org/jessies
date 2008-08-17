package e.util;

import org.junit.*;
import static org.junit.Assert.*;

public class TimeUtilitiesTest {
    @Test public void testNsToS() {
        assertEquals(0.5, TimeUtilities.nsToS(500000000), 0.01);
    }
}
