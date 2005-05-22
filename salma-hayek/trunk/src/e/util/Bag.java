package e.util;

import java.util.*;

public class Bag {
    private static final Integer ONE = new Integer(1);
    private Map<Object, Integer> objectToCountMap = new TreeMap<Object, Integer>();
    
    public Bag() {
    }
    
    public void add(Object key) {
        Integer count = objectToCountMap.get(key);
        objectToCountMap.put(key, (count == null ? ONE : (count.intValue() + 1)));
    }
    
    public boolean contains(Object key) {
        return (occurrenceCount(key) != 0);
    }
    
    public boolean isEmpty() {
        return (objectToCountMap.size() == 0);
    }
    
    public int occurrenceCount(Object key) {
        Integer count = objectToCountMap.get(key);
        return (count == null) ? 0 : count.intValue();
    }
    
    public Object commonestItem() {
        Object result = null;
        int resultCount = 0;
        
        for (Map.Entry<Object, Integer> entry : objectToCountMap.entrySet()) {
            int thisCount = entry.getValue();
            if (thisCount > resultCount) {
                result = entry.getKey();
                resultCount = thisCount;
            }
        }
        return result;
    }
    
    public String toString() {
        StringBuffer result = new StringBuffer();
        result.append("Bag[");
        for (Map.Entry<Object, Integer> entry : objectToCountMap.entrySet()) {
            int thisCount = entry.getValue();
            result.append("<" + entry.getKey() + ", " + thisCount + ">");
        }
        result.append("]");
        return result.toString();
    }
}
