package terminator.terminal;

import java.awt.*;
import java.io.*;

/**

@author Phil Norman
*/

public class PtyOutputStream extends OutputStream {
	private static final byte SIZE_ESCAPE = 0;
	private static final byte NOT_SIZE_ESCAPE = 1;
	private static final byte[] ESCAPED_ZERO = new byte[] { SIZE_ESCAPE, SIZE_ESCAPE };
	
	private OutputStream out;

	public PtyOutputStream(OutputStream out) {
		this.out = out;
	}
	
	public synchronized void sendResizeNotification(Dimension sizeInChars, Dimension sizeInPixels) throws IOException {
		byte[] values = new byte[10];
		values[0] = SIZE_ESCAPE;
		values[1] = NOT_SIZE_ESCAPE;
		writeValueAt(values, 2, sizeInChars.width);
		writeValueAt(values, 4, sizeInChars.height);
		writeValueAt(values, 6, sizeInPixels.width);
		writeValueAt(values, 8, sizeInPixels.height);
		out.write(values);
		out.flush();
	}
	
	private void writeValueAt(byte[] target, int offset, int value) {
		target[offset] = (byte) ((value >> 8) & 0xff);
		target[offset + 1] = (byte) (value & 0xff);
	}
	
	public synchronized void write(byte[] values) throws IOException {
		write(values, 0, values.length);
	}
	
	public synchronized void write(byte[] values, int start, int length) throws IOException {
		int outStart = start;
		for (int i = start; i < (start + length); i++) {
			if (values[i] == SIZE_ESCAPE) {
				out.write(values, outStart, i - outStart);
				out.write(ESCAPED_ZERO);
				outStart = i + 1;
			}
		}
		if (length > outStart) {
			out.write(values, outStart, length - outStart);
		}
	}
	
	public synchronized void write(int byteValue) throws IOException {
		if (byteValue == SIZE_ESCAPE) {
			out.write(ESCAPED_ZERO);
		} else {
			out.write(byteValue);
		}
	}
	
	public void close() throws IOException {
		out.close();
	}
	
	public void flush() throws IOException {
		out.flush();
	}
}
