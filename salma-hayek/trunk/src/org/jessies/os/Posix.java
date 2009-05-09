package org.jessies.os;

/**
 * Selected POSIX API.
 * 
 * This is deliberately a very thin wrapper. Where possible, all policy should reside in Java code.
 */
public class Posix {
    /** Posix.lseek constant to set file offset to current offset plus 'offset'. */
    public static final int SEEK_CUR = PosixJNI.get_SEEK_CUR();
    /** Posix.lseek constant to set file offset to end of file plus 'offset'. */
    public static final int SEEK_END = PosixJNI.get_SEEK_END();
    /** Posix.lseek constant to set file offset to 'offset'. */
    public static final int SEEK_SET = PosixJNI.get_SEEK_SET();
    
    /** Posix.access mode bit to test for read permission. */
    public static final int R_OK = PosixJNI.get_R_OK();
    /** Posix.access mode bit to test for write permission. */
    public static final int W_OK = PosixJNI.get_W_OK();
    /** Posix.access mode bit to test for execute (search) permission. */
    public static final int X_OK = PosixJNI.get_X_OK();
    /** Posix.access mode bit to test for existence of file. */
    public static final int F_OK = PosixJNI.get_F_OK();
    
    /** File number of standard error. */
    public static final int STDERR_FILENO = 2;
    /** File number of standard in. */
    public static final int STDIN_FILENO = 0;
    /** File number of standard out. */
    public static final int STDOUT_FILENO = 1;
    
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
     * Changes the permissions of 'path'.
     * Returns 0 on success, -errno on error.
     * http://www.opengroup.org/onlinepubs/000095399/functions/chmod.html
     */
    public static int chmod(String path, int mode) {
        return PosixJNI.chmod(path, mode);
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
     * Closes the file descriptor 'fd'.
     * Returns 0 on success, -errno on error.
     * http://www.opengroup.org/onlinepubs/000095399/functions/close.html
     */
    public static int close(int fd) {
        return PosixJNI.close(fd);
    }
    
    /**
     * Duplicates the open file descriptor 'oldFd'.
     * Returns 0 on success, -errno on error.
     * http://www.opengroup.org/onlinepubs/000095399/functions/dup.html
     */
    public static int dup(int oldFd) {
        return PosixJNI.dup(oldFd);
    }
    
    /**
     * Duplicates the open file descriptor 'oldFd' as 'newFd', closing 'newFd' first if necessary.
     * Returns 0 on success, -errno on error.
     * http://www.opengroup.org/onlinepubs/000095399/functions/dup2.html
     */
    public static int dup(int oldFd, int newFd) {
        return PosixJNI.dup2(oldFd, newFd);
    }
    
    /**
     * Returns the process id of the calling process.
     * This function is always successful and no return value is reserved to indicate an error.
     * http://www.opengroup.org/onlinepubs/000095399/functions/getpid.html
     */
    public static int getpid() {
        return PosixJNI.getpid();
    }
    
    /**
     * Returns the process id of the calling process' parent.
     * This function is always successful and no return value is reserved to indicate an error.
     * http://www.opengroup.org/onlinepubs/000095399/functions/getppid.html
     */
    public static int getppid() {
        return PosixJNI.getppid();
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
