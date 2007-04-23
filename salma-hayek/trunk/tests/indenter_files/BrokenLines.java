package foo;

import java.util.*;

/**
 * This is a dummy file, never meant to be compiled, which serves as a test
 * for our indentation support.
 * This file is a test of whether we correctly indent lines which have been
 * split over multiple lines.  We currently don't, which is why we include
 * the string ***EXPECTED TO FAIL*** somewhere in the file text (preferably
 * near the top).  Our test will notice this file, and hide away the failures
 * so we don't see them.
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
    
    public int countLettersOfType(char ch) {
        int result = 0;
        for (int i = 0; i < name.length(); i++) {
            if (name.charAt(i) ==
                ch) {
                result++;
            }
        }
        return result;
    }
    
    public int countLettersNotOfType(char ch) {
        int result = 0;
        int index = 0;
        while (index <
               name.length()) {
            if (name.charAt(index) == ch) {
                result++;
            }
        }
        return result;
    }
}
