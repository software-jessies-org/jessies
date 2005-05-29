package e.edit;

import java.awt.*;
import java.awt.geom.*;
import java.text.*;

// We're not allowed forward references in an enum's list of values, and the
// list of values has to come first. We also can't statically import these
// shapes because the class definition has to occur after import statements.
abstract class TagShapes {
    static final Shape CIRCLE = new java.awt.geom.Ellipse2D.Float(1, 1, 8, 8);
    static final Shape SQUARE = new Rectangle(1, 2, 7, 7);
    static final Shape TRIANGLE = new Polygon(new int[] { 0, 4, 8 }, new int[] { 8, 1, 8 }, 3);
}

public enum TagType {
    CLASS("class", TagShapes.CIRCLE, true, "{1} {0}"),
    CONSTRUCTOR("constructor", TagShapes.CIRCLE, false, "{0}()"),
    DESTRUCTOR("destructor", TagShapes.CIRCLE, false, "{0}()"),
    ENUM("enum", TagShapes.CIRCLE, true, "{1} {0}"),
    ENUMERATOR("enumerator", null, false, null),
    EXTERN("extern", null, false, "{1} {0}"),
    FIELD("field", TagShapes.TRIANGLE, false, null),
    INTERFACE("interface", TagShapes.CIRCLE, true, "{1} {0}"),
    MACRO("macro", null, false, null),
    METHOD("method", TagShapes.SQUARE, false, "{0}()"),
    MODULE("module", null, true, "{1} {0}"),
    NAMESPACE("namespace", null, true, "{1} {0}"),
    PACKAGE("package", null, false, "{1} {0}"),
    PROTOTYPE("prototype", TagShapes.SQUARE, false, "{0}() {1}"),
    STRUCT("struct", null, true, "{1} {0}"),
    TYPEDEF("typedef", null, false, "{1} {0}"),
    UNION("union", null, false, "{1} {0}"),
    VARIABLE("variable", null, false, null),
    
    UNKNOWN("unknown", null, false, null),
    ;
    
    private final String name;
    private final Shape shape;
    private final boolean isContainer;
    private final MessageFormat formatter;
    
    TagType(String name, Shape shape, boolean isContainer, String formatString) {
        this.name = name;
        this.shape = shape;
        this.isContainer = isContainer;
        this.formatter = (formatString != null) ? new MessageFormat(formatString) : null;
    }
    
    public Shape getShape() {
        return shape;
    }
    
    public boolean isContainer() {
        return isContainer;
    }
    
    public String describe(String identifier) {
        if (formatter != null) {
            return formatter.format(new String[] { identifier, name });
        } else {
            return identifier;
        }
    }
    
    public static TagType fromChar(char typeChar) {
        switch (typeChar) {
        case 'c':
            return CLASS;
        case 'C':
            return CONSTRUCTOR;
        case 'd':
            return MACRO;
        case 'D':
            return DESTRUCTOR;
        case 'e':
            return ENUMERATOR;
        case 'f':
            return FIELD;
        case 'g':
            return ENUM;
        case 'i':
            return INTERFACE;
        case 'm':
            return METHOD;
        case 'M':
            return MODULE;
        case 'n':
            return NAMESPACE;
        case 'p':
            return PACKAGE;
        case 'P':
            return PROTOTYPE;
        case 's':
            return STRUCT;
        case 't':
            return TYPEDEF;
        case 'u':
            return UNION;
        case 'v':
            return VARIABLE;
        case 'x':
            return EXTERN;
        default:
            return UNKNOWN;
        }
    }
}
