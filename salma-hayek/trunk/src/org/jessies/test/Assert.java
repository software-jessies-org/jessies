package org.jessies.test;

/**
 * Assertions for the simple Java unit testing framework.
 */
public final class Assert {
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
