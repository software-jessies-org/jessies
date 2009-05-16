package org.jessies.test;

import java.util.List;

/**
 * Assertions for the simple Java unit testing framework.
 */
public final class Assert {
    public static void equals(List<?> lhs, List<?> rhs) {
        if (lhs.size() != rhs.size()) {
            throw new RuntimeException("lhs.size() (" + lhs.size() + ") != rhs.size() (" + rhs.size() + ")");
        }
        for (int i = 0; i < lhs.size(); ++i) {
            Assert.equals(lhs.get(i), rhs.get(i));
        }
    }
    
    public static void equals(Object lhs, Object rhs) {
        if (lhs == null && rhs == null) {
            // Okay, I guess.
        } else if (lhs == null && rhs != null) {
            throw new RuntimeException(toString(lhs) + " != " + toString(rhs));
        } else if (lhs != null && rhs == null) {
            throw new RuntimeException(toString(lhs) + " != " + toString(rhs));
        } else if (!lhs.equals(rhs)) {
            throw new RuntimeException(toString(lhs) + " != " + toString(rhs));
        }
    }
    
    public static void equals(double lhs, double rhs, double epsilon) {
        if (Math.abs(lhs - rhs) > epsilon) {
            throw new RuntimeException(lhs + " != " + rhs + " (+/- " + epsilon + ")");
        }
    }
    
    public static void contains(String lhs, String rhs) {
        if (!lhs.contains(rhs)) {
            throw new RuntimeException(toString(lhs) + " does not contain " + toString(rhs));
        }
    }
    
    // Outputs 'o' in the most useful form given the circumstances.
    private static String toString(Object o) {
        if (o instanceof String) {
            // Quote strings to make it clearer where they begin and end.
            // FIXME: escape funny stuff?
            return "\"" + o + "\"";
        } else {
            return o.toString();
        }
    }
    
    private Assert() {
    }
}
