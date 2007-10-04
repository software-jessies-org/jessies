package e.edit;

import java.awt.*;
import java.awt.geom.*;

// We're not allowed forward references in an enum's list of values, and the
// list of values has to come first. We also can't statically import these
// shapes because the class definition has to occur after import statements.
abstract class TagShapes {
    static final Shape CIRCLE = new java.awt.geom.Ellipse2D.Float(1, 1, 8, 8);
    static final Shape SQUARE = new Rectangle(1, 2, 7, 7);
    static final Shape TRIANGLE = new Polygon(new int[] { 0, 4, 8 }, new int[] { 8, 1, 8 }, 3);
    static final Shape DIAMOND = new Polygon(new int[] { 0, 4, 8, 4 }, new int[] { 4, 0, 4, 8 }, 4);
    static final Shape HASH = makeHash();
    
    private static Shape makeHash() {
        GeneralPath path = new GeneralPath();
        // Two horizontal strokes.
        path.moveTo(1, 6);
        path.lineTo(8, 6);
        path.moveTo(1, 3);
        path.lineTo(8,3);
        // Two vertical strokes.
        path.moveTo(3, 1);
        path.lineTo(3, 8);
        path.moveTo(6, 1);
        path.lineTo(6, 8);
        return path;
    }
}

public enum TagType {
    CLASS("class", TagShapes.CIRCLE, true, "class %s"),
    CONSTRUCTOR("constructor", TagShapes.CIRCLE, false, "%s()"),
    DESTRUCTOR("destructor", TagShapes.CIRCLE, false, "%s()"),
    ENUM("enum", TagShapes.CIRCLE, true, "enum %s"),
    ENUM_CONSTANT("enum constant", null, false, "%s"),
    EXTERN("extern", null, false, "extern %s"),
    FIELD("field", TagShapes.TRIANGLE, false, "%s"),
    INTERFACE("interface", TagShapes.CIRCLE, true, "interface %s"),
    MACRO("macro", TagShapes.HASH, false, "%s"),
    METHOD("method", TagShapes.SQUARE, false, "%s()"),
    MODULE("module", null, true, "module %s"),
    NAMESPACE("namespace", null, true, "namespace %s"),
    PACKAGE("package", null, false, "package %s"),
    PROTOTYPE("prototype", TagShapes.SQUARE, false, "%s() prototype"),
    STRUCT("struct", TagShapes.CIRCLE, true, "struct %s"),
    TYPEDEF("typedef", null, false, "typedef %s"),
    UNION("union", TagShapes.CIRCLE, false, "union %s"),
    VARIABLE("variable", TagShapes.TRIANGLE, false, "%s"),
    
    UNKNOWN("unknown", null, false, "%s"),
    ;
    
    private final String name;
    private final Shape shape;
    private final boolean isContainer;
    private final String formatString;
    
    TagType(String name, Shape shape, boolean isContainer, String formatString) {
        this.name = name;
        this.shape = shape;
        this.isContainer = isContainer;
        this.formatString = formatString;
    }
    
    public Shape getShape() {
        return shape;
    }
    
    public boolean isContainer() {
        return isContainer;
    }
    
    public String describe(String identifier) {
        return formatString.replaceAll("%s", identifier);
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
            return ENUM_CONSTANT;
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
