package org.jessies.os;

/**
 * Selected POSIX API.
 * 
 * This is deliberately a very thin wrapper. Where possible, all policy should reside in Java code.
 */
public class Posix {
    /** Posix.access mode bit to test for read permission. */
    public static final int R_OK = PosixJNI.get_R_OK();
    /** Posix.access mode bit to test for write permission. */
    public static final int W_OK = PosixJNI.get_W_OK();
    /** Posix.access mode bit to test for execute (search) permission. */
    public static final int X_OK = PosixJNI.get_X_OK();
    /** Posix.access mode bit to test for existence of file. */
    public static final int F_OK = PosixJNI.get_F_OK();
    
    /**
     * Returns true if the requested access is permitted, false otherwise.
     * The 'accessMode' should be a bitwise or of the R_OK, W_OK, X_OK, and F_OK constants.
     * Returns 0 on success, -errno on error.
     * http://www.opengroup.org/onlinepubs/000095399/functions/access.html
     */
    public static int access(String path, int accessMode) {
        return PosixJNI.access(path, accessMode);
    }
    
    /**
     * Changes the user and group ownership of 'path' to 'uid' and 'gid'.
     * Returns 0 on success, -errno on error.
     * http://www.opengroup.org/onlinepubs/000095399/functions/chown.html
     */
    public static int chown(String path, int uid, int gid) {
        return PosixJNI.chown(path, uid, gid);
    }
    
    /**
     * Changes the permissions of 'path'.
     * Returns 0 on success, -errno on error.
     * http://www.opengroup.org/onlinepubs/000095399/functions/chmod.html
     */
    public static int chmod(String path, int mode) {
        return PosixJNI.chmod(path, mode);
    }
    
    /** Type of file. */
    public static final int S_IFMT = PosixJNI.get_S_IFMT();
    /** Block special. */
    public static final int S_IFBLK = PosixJNI.get_S_IFBLK();
    /** Character special. */
    public static final int S_IFCHR = PosixJNI.get_S_IFCHR();
    /** FIFO special. */
    public static final int S_IFIFO = PosixJNI.get_S_IFIFO();
    /** Regular. */
    public static final int S_IFREG = PosixJNI.get_S_IFREG();
    /** Directory. */
    public static final int S_IFDIR = PosixJNI.get_S_IFDIR();
    /** Symbolic link. */
    public static final int S_IFLNK = PosixJNI.get_S_IFLNK();
    /** Socket. */
    public static final int S_IFSOCK = PosixJNI.get_S_IFSOCK();
    
    /** Set-user-ID on execution. */
    public static final int S_ISUID = PosixJNI.get_S_ISUID();
    /** Set-group-ID on execution. */
    public static final int S_ISGID = PosixJNI.get_S_ISGID();
    
    /**
     * Returns 0 on success, -errno on error.
     * http://www.opengroup.org/onlinepubs/000095399/functions/lstat.html
     */
    public static int lstat(String path, Stat stat) {
        return PosixJNI.lstat(path, stat);
    }
    
    /**
     * Returns 0 on success, -errno on error.
     * http://www.opengroup.org/onlinepubs/000095399/functions/stat.html
     */
    public static int stat(String path, Stat stat) {
        return PosixJNI.stat(path, stat);
    }
}
