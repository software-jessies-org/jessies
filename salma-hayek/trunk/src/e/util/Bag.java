package e.util;

import java.util.*;

public class Bag<V> {
    private static class IntBox {
        public int value = 1;
        
        public String toString() {
            return Integer.toString(value);
        }
    }
    
    private Map<V, IntBox> objectToCountMap = new TreeMap<V, IntBox>();
    
    public Bag() {
    }
    
    public void add(V key) {
        IntBox count = objectToCountMap.get(key);
        if (count == null) {
            objectToCountMap.put(key, new IntBox());
        } else {
            ++count.value;
        }
    }
    
    public boolean contains(V key) {
        return (occurrenceCount(key) != 0);
    }
    
    public boolean isEmpty() {
        return (objectToCountMap.size() == 0);
    }
    
    public int occurrenceCount(V key) {
        IntBox count = objectToCountMap.get(key);
        return (count == null) ? 0 : count.value;
    }
    
    public V commonestItem() {
        V result = null;
        int resultCount = 0;
        
        for (Map.Entry<V, IntBox> entry : objectToCountMap.entrySet()) {
            int thisCount = entry.getValue().value;
            if (thisCount > resultCount) {
                result = entry.getKey();
                resultCount = thisCount;
            }
        }
        return result;
    }
    
    public String toString() {
        StringBuilder result = new StringBuilder();
        result.append("Bag[");
        for (Map.Entry<V, IntBox> entry : objectToCountMap.entrySet()) {
            result.append("<" + entry.getKey() + ", " + entry.getValue() + ">");
        }
        result.append("]");
        return result.toString();
    }
}
