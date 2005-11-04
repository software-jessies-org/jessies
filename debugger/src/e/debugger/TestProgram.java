package e.debugger;

import java.awt.*;

public class TestProgram {
    
    public static void main(String[] args) {
        new TestProgram();
    }
    
    public TestProgram() {
        Toolkit.getDefaultToolkit().beep();
        System.err.println("Test program launched.");
        callSomeMethods();
        startSomeThreads();
    }
    
    public void callSomeMethods() {
        thisMethod(1);
    }
    
    public void thisMethod(int a) {
        thatMethod(2, 3);
    }
    
    public void thatMethod(int b, int c) {
        Object o = new Object();
        theOtherMethod(o, new Object(), 4, 5, 6);
    }
    
    public void theOtherMethod(Object o, Object p, int d, int e, int f) {
        System.err.println("hello world");
    }
    
    public void startSomeThreads() {
        for (int i = 0; i < 5; i++) {
            new Thread(new Runnable() {
                public void run() {
                    System.err.println("Starting a thread...");
                    try {
                        Thread.sleep(5000);
                    } catch (InterruptedException ex) {
                        
                    }
                    System.err.println("...Stopping it again");
                }
            }).start();
            try {
                Thread.sleep(5000);
            } catch (InterruptedException ex) {
                
            }
        }
    }
    
    public class NestedType extends Object {
        
    }
}
