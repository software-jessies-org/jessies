package e.util;

import java.util.*;
import org.jessies.test.*;

public final class CollectionUtilities {
    /**
     * Returns the first position in 'list' where 'key' could be inserted without violating ordering according to 'comparator'.
     * (Collections.binarySearch makes no guarantee about which index will be returned if the list contains multiple elements equal
     * to the key.)
     * 
     * As with Collections.binarySearch, results are undefined if 'list' is not sorted into ascending order according to
     * 'comparator'.
     */
    public static <T> int lowerBound(List<? extends T> list, T key, Comparator<? super T> comparator) {
        int index = Collections.binarySearch(list, key, comparator);
        if (index < 0) {
            return -index - 1;
        }
        // FIXME: O(n) is distressing on a sorted list.
        while (index > 0 && comparator.compare(list.get(index - 1), key) == 0) {
            --index;
        }
        return index;
    }
    
    @Test private static void testLowerBound() {
        // http://www.sgi.com/tech/stl/upper_bound.html uses this as an example.
        final List<Integer> ints = Arrays.asList(1, 2, 3, 3, 3, 5, 8);
        Assert.equals(lowerBound(ints, 1, naturalOrder()), 0);
        Assert.equals(lowerBound(ints, 2, naturalOrder()), 1);
        Assert.equals(lowerBound(ints, 3, naturalOrder()), 2);
        Assert.equals(lowerBound(ints, 4, naturalOrder()), 5);
        Assert.equals(lowerBound(ints, 5, naturalOrder()), 5);
        Assert.equals(lowerBound(ints, 6, naturalOrder()), 6);
        Assert.equals(lowerBound(ints, 7, naturalOrder()), 6);
        Assert.equals(lowerBound(ints, 8, naturalOrder()), 6);
        Assert.equals(lowerBound(ints, 9, naturalOrder()), 7);
    }
    
    /**
     * Returns the last position in 'list' where 'key' could be inserted without violating ordering according to 'comparator'.
     * (Collections.binarySearch makes no guarantee about which index will be returned if the list contains multiple elements equal
     * to the key.)
     * 
     * As with Collections.binarySearch, results are undefined if 'list' is not sorted into ascending order according to
     * 'comparator'.
     */
    public static <T> int upperBound(List<? extends T> list, T key, Comparator<? super T> comparator) {
        int index = Collections.binarySearch(list, key, comparator);
        if (index < 0) {
            return -index - 1;
        }
        // FIXME: O(n) is distressing on a sorted list.
        while (index + 1 < list.size() && comparator.compare(list.get(index + 1), key) == 0) {
            ++index;
        }
        // We return the first index *past* the [run of] value[s].
        return index + 1;
    }
    
    @Test private static void testUpperBound() {
        // http://www.sgi.com/tech/stl/upper_bound.html uses this as an example.
        final List<Integer> ints = Arrays.asList(1, 2, 3, 3, 3, 5, 8);
        Assert.equals(upperBound(ints, 1, naturalOrder()), 1);
        Assert.equals(upperBound(ints, 2, naturalOrder()), 2);
        Assert.equals(upperBound(ints, 3, naturalOrder()), 5);
        Assert.equals(upperBound(ints, 4, naturalOrder()), 5);
        Assert.equals(upperBound(ints, 5, naturalOrder()), 6);
        Assert.equals(upperBound(ints, 6, naturalOrder()), 6);
        Assert.equals(upperBound(ints, 7, naturalOrder()), 6);
        Assert.equals(upperBound(ints, 8, naturalOrder()), 7);
    }
    
    /**
     * Returns a Comparator that imposes the natural ordering.
     * 
     * @see Collections.reverseOrder
     */
    @SuppressWarnings("unchecked")
    public static <T> Comparator<T> naturalOrder() {
        return (Comparator<T>) NATURAL_ORDER_COMPARATOR;
    }
    
    private static final Comparator<Object> NATURAL_ORDER_COMPARATOR = new NaturalOrderComparator<Object>();
    
    private static final class NaturalOrderComparator<T> implements Comparator<T> {
        public int compare(T lhs, T rhs) {
            @SuppressWarnings("unchecked") Comparable<T> comparableLhs = ((Comparable<T>) lhs);
            return comparableLhs.compareTo(rhs);
        }
    }
    
    private CollectionUtilities() {
    }
}
