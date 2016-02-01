package e.util;

import java.io.*;

// TODO: Raise a bug with Sun, complaining that something with this name and behavior should be in the JDK.
public class NullOutputStream extends OutputStream {
  public void write(int b) {
  }
}
