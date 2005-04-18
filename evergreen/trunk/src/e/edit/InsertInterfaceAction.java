package e.edit;

import java.awt.event.*;
import java.lang.reflect.*;
import java.util.regex.*;

public class InsertInterfaceAction extends ETextAction {
    public static final String ACTION_NAME = "Insert Java Interface";

    public InsertInterfaceAction() {
        super(ACTION_NAME);
    }

    public void actionPerformed(ActionEvent e) {
        insertSourceCode(getTextArea());
    }
    
    public static String extractClassNameFrom(String text) {
        Matcher newMatcher = JavaResearcher.NEW_PATTERN.matcher(text);
        if (newMatcher.find()) {
            return newMatcher.group(1);
        }
        return null;
    }
    
    public static void insertSourceCode(ETextArea target) {
        String className = extractClassNameFrom(Edit.getAdvisor().getLookupString());
        if (className == null) {
            return;
        }
        Class[] classes = JavaDoc.getClasses(className);
        if (classes.length > 1) {
            return;
        }
        String src = getSourceCodeFor(classes[0]);
        if (src.length() == 0) {
            return;
        }
        
        // FIXME
        /*
        CompoundEdit entireEdit = new CompoundEdit();
        target.getUndoManager().addEdit(entireEdit);
        try {
            int dot = target.getCaretPosition();
            Document doc = target.getDocument();
            if (doc.getText(dot - 1, 1).equals("(") == false) {
                src = "(" + src;
            }
            doc.insertString(dot, src, null);
            int secondBrace = src.indexOf("{", src.indexOf("{") + 1);
            if (secondBrace != -1) {
                doc.insertString(dot + secondBrace + 1, "\n", null);
            }
            
            int first = target.getLineOfOffset(dot);
            int last = target.getLineOfOffset(dot + src.length()) + 1;
            target.setCaretPosition(dot);
            for (int i = first; i < last; i++) {
                target.correctIndentation();
            }
            
            // Position the caret inside the first method
            target.setCaretPosition(target.getLineEndOffsetBeforeTerminator(first + 3));
        } catch (BadLocationException ex) {
            ex.printStackTrace();
        } finally {
            entireEdit.end();
        }
        */
    }
    
    /**
    * Makes Java source code to implement all the public abstract methods in a class.
    */
    private static String getSourceCodeFor(Class c) {
        StringBuffer s = new StringBuffer(") {");
        if (c.getName().endsWith("Adapter")) {
            s.append(implementMethods(c.getDeclaredMethods(), false));
        } else {
            s.append(implementMethods(c.getMethods(), true));
        }
        s.append("\n}\n");
        return s.toString();
    }
    
    private static String implementMethods(Method[] methods, boolean implementAbstractMethods) {
        StringBuffer s = new StringBuffer();
        for (int i = 0; i < methods.length; i++) {
            Method m = methods[i];
            int mods = m.getModifiers();
            if (Modifier.isPublic(mods) && Modifier.isAbstract(mods) == implementAbstractMethods) {
                s.append("\n/**  */\npublic ");
                s.append(getSourceCodeFor(m));
            }
        }
        return s.toString();
    }
    
    private static String getSourceCodeFor(Method m) {
        StringBuffer s = new StringBuffer();
        String rtype = m.getReturnType().getName();
        rtype = rtype.substring(rtype.lastIndexOf(".") + 1);
        s.append(rtype);
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
        s.append(" {\n}");
        return s.toString();
    }
    
    private static String listClassNames(Class[] classes, boolean inventNames) {
        StringBuffer s = new StringBuffer();
        for (int i = 0; i < classes.length; i++) {
            Class c = classes[i];
            String name = c.getName();
            s.append(name.substring(name.lastIndexOf(".") + 1));
            if (inventNames) {
                s.append(getParameterNameFor(c));
            }
            if (i < classes.length - 1) {
                s.append(", ");
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
            return (" " + name.substring(0, 1).toLowerCase()); // Primitive names are keywords, so we can't use them directly.
        } else {
            return (" " + name.substring(0, 1).toLowerCase() + name.substring(1));
        }
    }
}
