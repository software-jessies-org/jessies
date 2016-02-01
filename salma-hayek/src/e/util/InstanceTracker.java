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
    // Really, this is a map from Class<x> to ArrayList<WeakReference<x>>, where x is not a compile-time constant.
    // I don't think there's any way to express that, so we say WeakReference<?> to silence javac's raw-type warnings.
    private static final HashMap<Class<?>, ArrayList<WeakReference<?>>> CLASS_TO_INSTANCES_MAP = new HashMap<Class<?>, ArrayList<WeakReference<?>>>();
    private static final boolean DEBUG = false;
    
    public static synchronized <T> void addInstance(T instance) {
        if (DEBUG) {
            System.err.println("Adding " + instance + " of class " + instance.getClass() + ".");
        }
        ArrayList<WeakReference<?>> instances = instancesOfClass(instance.getClass());
        instances.add(new WeakReference<T>(instance));
    }
    
    // Really, you're getting ArrayList<WeakReference<T>> for the given Class<T>, but I don't think we can express that.
    private static synchronized ArrayList<WeakReference<?>> instancesOfClass(Class<?> klass) {
        ArrayList<WeakReference<?>> instances = CLASS_TO_INSTANCES_MAP.get(klass);
        if (instances == null) {
            instances = new ArrayList<WeakReference<?>>();
            CLASS_TO_INSTANCES_MAP.put(klass, instances);
        }
        if (DEBUG) {
            System.err.println("Returning list " + instances + " for " + klass + ".");
        }
        return instances;
    }
    
    public static synchronized <T> List<T> getInstancesOfClass(Class<T> klass) {
        ArrayList<T> result = new ArrayList<T>();
        List<WeakReference<?>> weakReferences = instancesOfClass(klass);
        if (DEBUG) {
            System.err.println("Weak references: " + weakReferences.size());
        }
        for (WeakReference<?> weakReference : weakReferences) {
            T instance = klass.cast(weakReference.get());
            if (instance != null) {
                result.add(instance);
            }
        }
        if (DEBUG) {
            System.err.println("References: " + result.size());
        }
        return result;
    }
    
    private InstanceTracker() {
    }
}
