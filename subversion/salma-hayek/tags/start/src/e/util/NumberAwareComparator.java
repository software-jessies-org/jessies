package e.util;

public class NumberAwareComparator implements java.util.Comparator {
    private static final int SAME = 0;
    private static final int LESSER = -1;
    private static final int GREATER = 1;

    public static final int compareInts(int i1, int i2) {
        if (i1 == i2) {
            return SAME;
        }
        return (i1 < i2) ? LESSER : GREATER;
    }

    public int compare(Object o1, Object o2) {
        if (o1 == o2) {
            return 0;
        }
        return compareStringsNumerically((String) o1, 0, (String) o2, 0);
    }

    public static int compareStringsNumerically(String s1, int offset1, String s2, int offset2) {
        char c1 = '\0';
        char c2 = '\0';

        // skip all common, nonnumeric characters
        int len1 = s1.length() - offset1;
        int len2 = s2.length() - offset2;
        int count = Math.min(len1, len2);
        int i;
        for (i = 0; i < count; i++) {
            c1 = s1.charAt(offset1 + i);
            c2 = s2.charAt(offset2 + i);
            if (c1 != c2 || Character.isDigit(c1) || Character.isDigit(c2)) {
                break;
            }
        }

        // one string is prefix of the other string
        // so we just need to compare the two lengths
        if (i == count) {
            return compareInts(len1, len2);
        }

        boolean isDecimal1 = Character.isDigit(c1);
        boolean isDecimal2 = Character.isDigit(c2);

        if (!isDecimal1 || !isDecimal2) {
            return (c1 - c2);
        }

        // both substrings have a numeric prefix
        long v1 = c1 - '0';
        long v2 = c2 - '0';
        int i1, i2;

        // calculate the integer values
        for (i1 = offset1 + i + 1; i1 < len1; i1++) {
            char c = s1.charAt(i1);
            if (Character.isDigit(c)) {
                v1 = (10 * v1) + c - '0';
            } else {
                break;
            }
        }
        for (i2 = offset2 + i + 1; i2 < len2; i2++) {
            char c = s2.charAt(i2);
            if (Character.isDigit(c)) {
                v2 = (10 * v2) + c - '0';
            } else {
                break;
            }
        }

        if (v1 == v2) {
            // Both strings are equal so far, so we need to look further.
            return compareStringsNumerically(s1, i1, s2, i2);
        } else {
            return (v1 < v2) ? LESSER : GREATER;
        }
    }

    public static void main(String[] args) {
        test("hello", "world");
        test("hello", "hello");
        test("hello", "hello there");
        test("file1", "file2");
        test("file10", "file2");
        test("file100", "file9");
        System.exit(0);
    }

    public static void test(String s1, String s2) {
        System.out.println(s1 + " v " + s2 + " = " + new NumberAwareComparator().compare(s1, s2));
    }
}
