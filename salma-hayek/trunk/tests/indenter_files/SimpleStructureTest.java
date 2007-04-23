package foo;

import java.util.*;

/**
 * This is a dummy file, never meant to be compiled, which serves as a test
 * for the simplest, most basic structures for indentation.
 * Note: the following line has a space after the '*'.  This is not nice, but sadly
 * unavoidable given our current way of indenting stuff.  Fix this in the future:
 * 
 * @author Phil Norman
 */

public class SimpleStructureTest {
    private String name;
    
    public SimpleStructureTest(String name) {
        this.name = name;
    }
    
    public void setName(String name) { this.name = name; }
    
    public String getName() {   // Put a comment in here to check things still indent below.
        return name;
    }
    
    public String toString()
    {
        return getName();
    }
    
    public int hashCode() { // Note to the casual reader: no, I don't normally write this kind of thing:
        final int result;
        (new Runnable() {
            public void run() {
                result = getName().hashCode();
            }
        }).run();
        return result;
    }
}
