package org.jessies.blindvnc;

public class KeyCodeTranslator {
    public int getJavaKeyCode(int vncCode) {
        System.err.printf("Got VNC key code: %x", vncCode);
        return vncCode;
    }
}
