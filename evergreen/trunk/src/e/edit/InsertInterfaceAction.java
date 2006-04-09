package e.edit;

import java.awt.event.*;
import java.lang.reflect.*;
import java.util.regex.*;

/**
 * Pastes in the boilerplate associated with implementing an interface. You can
 * use it to finish lines of the form:
 * 
 *   addWindowListener(|
 * 
 * or:
 * 
 *   setRunnable(new Runnable(|
 */
public class InsertInterfaceAction extends ETextAction {
    public static final String ACTION_NAME = "Insert Java Interface";
    
    public InsertInterfaceAction() {
        super(ACTION_NAME);
        putValue(ACCELERATOR_KEY, e.util.GuiUtilities.makeKeyStroke("I", true));
    }
    
    public boolean isEnabled() {
        ETextWindow textWindow = getFocusedTextWindow();
        return (textWindow != null && textWindow.isJava());
    }
    
    public void actionPerformed(ActionEvent e) {
        pasteSourceCode();
    }
    
    private void pasteSourceCode() {
        String text = Edit.getInstance().getAdvisor().getLookupString();
        
        String className = null;
        String prefix = "";
        // "new WindowListener(" -> "WindowListener"
        Matcher matcher = Pattern.compile("(?x) .* \\b new \\s+ (\\S+?) \\s* \\(? $").matcher(text);
        if (matcher.find()) {
            className = matcher.group(1);
            if (text.endsWith("(") == false) {
                prefix = "(";
            }
        }
        // "addWindowListener(" -> "WindowListener"
        matcher = Pattern.compile("(?x) .* \\b add(\\S+?) \\s* \\(? $").matcher(text);
        if (matcher.find()) {
            className = matcher.group(1);
            if (text.endsWith("(") == false) {
                prefix = "(";
            }
            prefix += "new " + className + "(";
        }
        if (className == null) {
            Edit.getInstance().showAlert("Couldn't insert interface", "It wasn't possible to work out what interface you're referring to.");
            return;
        }
        
        Class[] classes = JavaDoc.getClasses(className);
        if (classes.length != 1) {
            Edit.getInstance().showAlert("Couldn't insert interface", "The class name \"" + className + "\" must be unique, but matches " + classes.length + " classes.");
            return;
        }
        
        String source = prefix + sourceCodeForClass(classes[0]);
        //System.out.println("source=" + e.util.StringUtilities.escapeForJava(source));
        getTextArea().pasteAndReIndent(source);
    }
    
    /**
     * Returns Java source code to implement all the public abstract methods
     * in the given class.
     */
    private static String sourceCodeForClass(Class c) {
        StringBuilder s = new StringBuilder(") {");
        if (c.getName().endsWith("Adapter")) {
            s.append(sourceCodeForMethods(c.getDeclaredMethods(), false));
        } else {
            s.append(sourceCodeForMethods(c.getMethods(), true));
        }
        s.append("\n}");
        return s.toString();
    }
    
    private static String sourceCodeForMethods(Method[] methods, boolean implementAbstractMethods) {
        StringBuilder s = new StringBuilder();
        for (Method m : methods) {
            int mods = m.getModifiers();
            if (Modifier.isPublic(mods) && Modifier.isAbstract(mods) == implementAbstractMethods) {
                s.append("\npublic ");
                s.append(sourceCodeForMethod(m));
            }
        }
        return s.toString();
    }
    
    /**
     * Returns Java source code to implement the given method. A method with
     * a non-void return type will contain a return statement missing its
     * expression.
     */
    private static String sourceCodeForMethod(Method m) {
        StringBuilder s = new StringBuilder();
        String returnType = m.getReturnType().getName();
        returnType = returnType.substring(returnType.lastIndexOf(".") + 1);
        s.append(returnType);
        s.append(" ");
        s.append(m.getName());
        s.append("(");
        s.append(listClassNames(m.getParameterTypes(), true));
        s.append(")");
        Class[] exs = m.getExceptionTypes();
        if (exs.length > 0) {
            s.append(" throws ");
            s.append(listClassNames(exs, false));
        }
        s.append(" {\n");
        if (returnType.equals("void") == false) {
            s.append("return ;");
        }
        s.append("}");
        return s.toString();
    }
    
    private static String listClassNames(Class[] classes, boolean inventNames) {
        StringBuilder s = new StringBuilder();
        for (Class c : classes) {
            String name = c.getName();
            if (s.length() > 0) {
                s.append(", ");
            }
            s.append(name.substring(name.lastIndexOf(".") + 1));
            if (inventNames) {
                s.append(getParameterNameFor(c));
            }
        }
        return s.toString();
    }
    
    private static String getParameterNameFor(Class c) {
        String name = c.getName();
        name = name.substring(name.lastIndexOf(".") + 1);
        if (name.endsWith("Event")) {
            return (" e");
        } else if (name.endsWith("Listener")) {
            return (" l");
        } else if (name.endsWith("Object")) {
            return (" o");
        } else if (name.endsWith("String")) {
            return (" s");
        } else if (c.isPrimitive()) {
            // Primitive names are keywords, so we can't use them directly.
            // Use 'b' for "boolean", 'c' for "char", et cetera.
            return (" " + name.substring(0, 1).toLowerCase());
        } else {
            return (" " + name.substring(0, 1).toLowerCase() + name.substring(1));
        }
    }
}
