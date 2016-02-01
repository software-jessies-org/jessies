package org.jessies.test;

import java.util.*;
import java.util.regex.*;

/**
 * Assertions for the simple Java unit testing framework.
 */
public final class Assert {
    public static void failure(String message) {
        throw new RuntimeException(message);
    }
    
    public static void gt(int lhs, int rhs) {
        if (lhs <= rhs) {
            failure("lhs (" + lhs + ") <= rhs (" + rhs + ")");
        }
    }
    
    public static void lt(int lhs, int rhs) {
        if (lhs >= rhs) {
            failure("lhs (" + lhs + ") >= rhs (" + rhs + ")");
        }
    }
    
    public static void equals(List<?> lhs, List<?> rhs) {
        if (lhs.size() != rhs.size()) {
            failure("lhs.size() (" + lhs.size() + ") != rhs.size() (" + rhs.size() + ")");
        }
        for (int i = 0; i < lhs.size(); ++i) {
            Assert.equals(lhs.get(i), rhs.get(i));
        }
    }
    
    public static void equals(Object lhs, Object rhs) {
        if (lhs == null && rhs == null) {
            // Okay, I guess.
        } else if (lhs == null && rhs != null) {
            failure(toString(lhs) + " != " + toString(rhs));
        } else if (lhs != null && rhs == null) {
            failure(toString(lhs) + " != " + toString(rhs));
        } else if (!lhs.equals(rhs)) {
            failure(toString(lhs) + " != " + toString(rhs));
        }
    }
    
    public static void equals(double lhs, double rhs, double epsilon) {
        if (Math.abs(lhs - rhs) > epsilon) {
            failure(lhs + " != " + rhs + " (+/- " + epsilon + ")");
        }
    }
    
    public static void contains(String lhs, String rhs) {
        if (!lhs.contains(rhs)) {
            failure(toString(lhs) + " does not contain " + toString(rhs));
        }
    }
    
    public static void startsWith(String s, String prefix) {
        if (!s.startsWith(prefix)) {
            failure(toString(s) + " does not start with " + toString(prefix));
        }
    }
    
    /**
     * Tests whether 'pattern' matches 'text' resulting in the given expected matches.
     * Note that the expected matches are all capturing group 1.
     * For example: Assert.matches(Pattern.compile("(a*)b*", "ab aab", "a", "aa")).
     * If this doesn't suit your needs, consider adding an extra parameter like "$1" to describe the group(s) to be compared.
     */
    public static void matches(final Pattern pattern, final String text, final String... expectedMatches) {
        final List<String> expected = Arrays.asList(expectedMatches);
        final List<String> actual = new ArrayList<String>();
        final Matcher matcher = pattern.matcher(text);
        while (matcher.find()) {
            actual.add(matcher.group(1));
        }
        Assert.equals(expected, actual);
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
