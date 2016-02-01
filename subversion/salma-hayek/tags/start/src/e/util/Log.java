package e.util;

import java.text.*;
import java.util.*;
import java.lang.reflect.*;

public class Log {
    private static String applicationName = "unknown";
    private static DateFormat dateFormatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.S Z");

    public static void setApplicationName(String name) {
        applicationName = name;
        warn("Application started.");
    }

    public static void warn(String message) {
        warn(message, null);
    }

    public static void warn(String message, Throwable th) {
        String time = dateFormatter.format(new Date());
        System.err.println(time + " " + applicationName + ": "  + message);
        if (th != null) {
            System.err.println("Associated exception:");
            th.printStackTrace();
        }
    }
    
    public static String inspect(Object o) {
        Class c = o.getClass();
        
        if (c == String.class) {
            return "\"" + o.toString() + "\"";
        }
        
        if (c.isArray()) {
            return inspectArray(o);
        } else if (c.isPrimitive()) {
            return "" + o;
        }
        
        StringBuffer result = new StringBuffer(c.getName());
        result.append('[');
        
        Field[] fields = c.getDeclaredFields();
        boolean needComma = false;
        for (int i = 0; i < fields.length; i++) {
            Field field = fields[i];
            if (Modifier.isStatic(field.getModifiers())) {
                continue;
            }
            if (needComma) {
                result.append(',');
            }
            result.append(field.getName());
            result.append('=');
            try {
                result.append(inspect(field.get(o)));
            } catch (Throwable th) {
                result.append("?");//th.getMessage());
            }
            needComma = true;
        }
        
        result.append(']');
        return result.toString();
    }
    
    public static String inspectArray(Object o) {
        Class c = o.getClass();
        Class itemClass = c.getComponentType();
        StringBuffer result = new StringBuffer(itemClass.getName());
        result.append("[] = {");
        if (itemClass.isPrimitive()) {
            result.append("primitives!");
        } else {
            Object[] array = (Object[]) o;
            for (int i = 0; i < array.length; i++) {
                if (i > 0) {
                    result.append(',');
                }
                result.append(inspect(array[i]));
            }
        }
        result.append('}');
        return result.toString();
    }
    
    public static void main(String[] args) {
        int[] numbers = new int[] { 1, 4, 173 };
        System.err.println(inspect(Boolean.TRUE));
        System.err.println(inspect(java.awt.Color.RED));
        System.err.println(inspect(args));
        System.err.println(inspect(numbers));
    }

    /** Protects against instantiation. */
    private Log() {
    }
}
