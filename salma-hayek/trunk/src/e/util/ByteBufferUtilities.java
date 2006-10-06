package e.util;

import java.io.*;
import java.nio.*;
import java.nio.channels.*;

public final class ByteBufferUtilities {
    /**
     * Reads the contents of the given File into a ByteBuffer.
     */
    public static ByteBuffer readFile(File file) throws IOException {
        DataInputStream dataInputStream = null;
        FileChannel fileChannel = null;
        try {
            FileInputStream fileInputStream = new FileInputStream(file);
            ByteBuffer byteBuffer = null;
            
            // FIXME: we should measure where the best cut-off point is. Maybe always map and fall back to reading?
            int byteCount = (int) file.length();
            if (byteCount <= 4096) {
                // Read the whole file in.
                dataInputStream = new DataInputStream(fileInputStream);
                byteBuffer = ByteBuffer.wrap(new byte[byteCount]);
                dataInputStream.readFully(byteBuffer.array());
            } else {
                // Map the file into memory.
                fileChannel = fileInputStream.getChannel();
                byteBuffer = fileChannel.map(FileChannel.MapMode.READ_ONLY, 0, byteCount);
            }
            
            return byteBuffer;
        } finally {
            if (fileChannel != null) {
                fileChannel.close();
            }
            if (dataInputStream != null) {
                dataInputStream.close();
            }
        }
    }
    
    /**
     * Checks the first 16 bytes of the given ByteBuffer for a 0 byte.
     * FIXME: this is a poor heuristic for anyone using UTF-16.
     */
    public static boolean isBinaryByteBuffer(ByteBuffer byteBuffer, final int byteCount) {
        final int end = Math.min(byteCount, 16);
        for (int i = 0; i < end; i++) {
            if (byteBuffer.get(i) == 0) {
                return true;
            }
        }
        return false;
    }
    
    private ByteBufferUtilities() {
    }
}
