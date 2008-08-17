package e.util;

import org.junit.*;
import static org.junit.Assert.*;

public class RewriterTest {
    @Test public void testInches() {
        // Rewrite an ancient unit of length in SI units.
        String result = new Rewriter("([0-9]+(\\.[0-9]+)?)[- ]?(inch(es)?)") {
            public String replacement() {
                float inches = Float.parseFloat(group(1));
                return Float.toString(2.54f * inches) + " cm";
            }
        }.rewrite("a 17 inch display");
        assertEquals("a 43.18 cm display", result);
    }
    
    @Test public void testAlmanac() {
        // The "Searching and Replacing with Non-Constant Values Using a
        // Regular Expression" example from the Java Almanac.
        String result = new Rewriter("([a-zA-Z]+[0-9]+)") {
            public String replacement() {
                return group(1).toUpperCase();
            }
        }.rewrite("ab12 cd efg34");
        assertEquals("AB12 cd EFG34", result);
    }
    
    @Test public void testDollars() {
        String result = new Rewriter("([0-9]+) US cents") {
            public String replacement() {
                long dollars = Long.parseLong(group(1))/100;
                return "$" + dollars;
            }
        }.rewrite("5000 US cents");
        assertEquals("$50", result);
    }
    
    @Test public void testMilliseconds() {
        // Rewrite durations in milliseconds in ISO 8601 format.
        String result = new Rewriter("(\\d+)\\s*ms") {
            public String replacement() {
                long milliseconds = Long.parseLong(group(1));
                return TimeUtilities.msToIsoString(milliseconds);
            }
        }.rewrite("232341243 ms");
        assertEquals("P64H32M21.243S", result);
    }
}
