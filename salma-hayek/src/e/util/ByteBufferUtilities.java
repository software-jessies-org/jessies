package e.util;

import java.io.*;
import java.nio.*;

public final class ByteBufferUtilities {
    /**
     * Reads the contents of the given File into a ByteBuffer.
     * This ByteBuffer is always backed by a byte[] of exactly the file's length (at the time we started to read).
     */
    public static ByteBuffer readFile(File file) throws IOException {
        DataInputStream dataInputStream = null;
        try {
            // FIXME: this is broken for files larger than 4GiB.
            int byteCount = (int) file.length();
            
            // Always read the whole file in rather than using memory mapping.
            // Windows' file system semantics also mean that there's a period after a search finishes but before the buffer is actually unmapped where you can't write to the file (see Sun bug 6359560).
            // Being unable to manually unmap causes no functional problems but hurts performance on Unix (see Sun bug 4724038).
            // Testing in C (working on Ctags) shows that for typical source files (Linux 2.6.17 and JDK6), the performance benefit of mmap(2) is small anyway.
            // Evergreen actually searches both of those source trees faster with readFully than with map.
            // At the moment, then, there's no obvious situation where we should map the file.
            FileInputStream fileInputStream = new FileInputStream(file);
            dataInputStream = new DataInputStream(fileInputStream);
            final byte[] bytes = new byte[byteCount];
            dataInputStream.readFully(bytes);
            
            return ByteBuffer.wrap(bytes);
        } finally {
            FileUtilities.close(dataInputStream);
        }
    }
    
    /**
     * Checks the first 16 bytes of the given ByteBuffer for a 0 byte.
     * FIXME: this is a poor heuristic for anyone using UTF-16.
     */
    public static boolean isBinaryByteBuffer(ByteBuffer byteBuffer, final int byteCount) {
        final int end = Math.min(byteCount, 16);
        for (int i = 0; i < end; ++i) {
            if (byteBuffer.get(i) == 0) {
                return true;
            }
        }
        return false;
    }
    
    private ByteBufferUtilities() {
    }
}
