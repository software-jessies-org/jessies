package e.util;

import java.util.*;

public class Bag<V> {
    private static final Integer ONE = Integer.valueOf(1);
    private Map<V, Integer> objectToCountMap = new TreeMap<V, Integer>();
    
    public Bag() {
    }
    
    public void add(V key) {
        Integer count = objectToCountMap.get(key);
        objectToCountMap.put(key, (count == null ? ONE : (count.intValue() + 1)));
    }
    
    public boolean contains(V key) {
        return (occurrenceCount(key) != 0);
    }
    
    public boolean isEmpty() {
        return (objectToCountMap.size() == 0);
    }
    
    public int occurrenceCount(V key) {
        Integer count = objectToCountMap.get(key);
        return (count == null) ? 0 : count.intValue();
    }
    
    public V commonestItem() {
        V result = null;
        int resultCount = 0;
        
        for (Map.Entry<V, Integer> entry : objectToCountMap.entrySet()) {
            int thisCount = entry.getValue();
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
        for (Map.Entry<V, Integer> entry : objectToCountMap.entrySet()) {
            int thisCount = entry.getValue();
            result.append("<" + entry.getKey() + ", " + thisCount + ">");
        }
        result.append("]");
        return result.toString();
    }
}
