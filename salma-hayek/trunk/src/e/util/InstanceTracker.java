package e.util;

import java.lang.ref.*;
import java.util.*;

/**
 * Lets you keep track of all the instances of a given class. Just add a call
 * to InstanceTracker.addInstance to the class' constructor, and you can
 * later use InstanceTracker.getInstancesOfClass to get a list of the instances
 * that are still extant.
 */
public class InstanceTracker {
    private static final HashMap CLASS_TO_INSTANCES_MAP = new HashMap();
    private static final boolean DEBUG = false;
    
    public static synchronized void addInstance(Object instance) {
        if (DEBUG) {
            System.err.println("Adding " + instance + " of class " + instance.getClass() + ".");
        }
        ArrayList instances = instancesOfClass(instance.getClass());
        instances.add(new WeakReference(instance));
    }
    
    private static synchronized ArrayList instancesOfClass(Class klass) {
        ArrayList instances = (ArrayList) CLASS_TO_INSTANCES_MAP.get(klass);
        if (instances == null) {
            instances = new ArrayList();
            CLASS_TO_INSTANCES_MAP.put(klass, instances);
        }
        if (DEBUG) {
            System.err.println("Returning list " + instances + " for " + klass + ".");
        }
        return instances;
    }
    
    public static synchronized Object[] getInstancesOfClass(Class klass) {
        ArrayList result = new ArrayList();
        List weakReferences = instancesOfClass(klass);
        if (DEBUG) {
            System.err.println("Weak references: " + weakReferences.size());
        }
        for (int i = 0; i < weakReferences.size(); ++i) {
            WeakReference weakReference = (WeakReference) weakReferences.get(i);
            Object instance = weakReference.get();
            if (instance != null) {
                result.add(instance);
            }
        }
        if (DEBUG) {
            System.err.println("References: " + result.size());
        }
        return result.toArray(new Object[result.size()]);
    }
    
    private InstanceTracker() {
    }
}
