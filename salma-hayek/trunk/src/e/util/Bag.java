package e.util;

import java.util.*;

public class Bag {
    private static final Integer ONE = new Integer(1);
    private Map objectToCountMap = new TreeMap();
    
    public Bag() {
    }
    
    public void add(Object key) {
        Integer count = (Integer) objectToCountMap.get(key);
        objectToCountMap.put(key, (count == null ? ONE : new Integer(count.intValue() + 1)));
    }
    
    public boolean contains(Object key) {
        return (occurrenceCount(key) != 0);
    }
    
    public boolean isEmpty() {
        return (objectToCountMap.size() == 0);
    }
    
    public int occurrenceCount(Object key) {
        Integer count = (Integer) objectToCountMap.get(key);
        return (count == null) ? 0 : count.intValue();
    }
    
    public Object commonestItem() {
        Object result = null;
        int resultCount = 0;
        
        Set entries = objectToCountMap.entrySet();
        Iterator it = entries.iterator();
        while (it.hasNext()) {
            Map.Entry entry = (Map.Entry) it.next();
            int thisCount = ((Integer) entry.getValue()).intValue();
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
        Set entries = objectToCountMap.entrySet();
        Iterator it = entries.iterator();
        while (it.hasNext()) {
            Map.Entry entry = (Map.Entry) it.next();
            int thisCount = ((Integer) entry.getValue()).intValue();
            result.append("<" + entry.getKey().toString() + ", " + thisCount + ">");
        }
        result.append("]");
        return result.toString();
    }
}
