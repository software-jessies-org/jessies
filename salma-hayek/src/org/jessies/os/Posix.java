package org.jessies.os;

/**
 * Selected POSIX API.
 * 
 * This is deliberately a very thin wrapper. Where possible, all policy should reside in Java code.
 */
public class Posix {
    /** Successful termination for System.exit. Must be 0. */
    public static final int EXIT_SUCCESS = 0;
    /** Unsuccessful termination for System.exit; evaluates to a non-zero value. */
    public static final int EXIT_FAILURE = PosixJNI.get_EXIT_FAILURE();
    
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
    
    /** Posix.open flag to create file if it does not exist. */
    public static final int O_CREAT = PosixJNI.get_O_CREAT();
    /** Posix.open exclusive use flag. */
    public static final int O_EXCL = PosixJNI.get_O_EXCL();
    /** Posix.open flag to not assign controlling terminal. */
    public static final int O_NOCTTY = PosixJNI.get_O_NOCTTY();
    /** Posix.open flag to truncate file. */
    public static final int O_TRUNC = PosixJNI.get_O_TRUNC();
    
    /** Posix.open flag to set append mode. */
    public static final int O_APPEND = PosixJNI.get_O_APPEND();
    
    /** Posix.open flag to open for reading only. */
    public static final int O_RDONLY = PosixJNI.get_O_RDONLY();
    /** Posix.open flag to open for reading and writing. */
    public static final int O_RDWR = PosixJNI.get_O_RDWR();
    /** Posix.open flag to open for writing only. */
    public static final int O_WRONLY = PosixJNI.get_O_WRONLY();
    
    /** Posix.waitpid flag to return if a stopped child has received SIGCONT. */
    public static final int WCONTINUED = PosixJNI.get_WCONTINUED();
    /** Posix.waitpid flag to not block if no status is yet available. */
    public static final int WNOHANG = PosixJNI.get_WNOHANG();
    /** Posix.waitpid flag to also return if a child is merely stopped. */
    public static final int WUNTRACED = PosixJNI.get_WUNTRACED();
    
    /**
     * Closes the file descriptor 'fd'.
     * Returns 0 on success, -errno on error.
     * http://www.opengroup.org/onlinepubs/000095399/functions/close.html
     */
    public static int close(int fd) {
        return PosixJNI.close(fd);
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
     * Sends 'signal' to a process or group of processes.
     * Returns 0 on success, -errno on error.
     * http://www.opengroup.org/onlinepubs/000095399/functions/kill.html
     */
    public static int kill(int pid, int signal) {
        return PosixJNI.kill(pid, signal);
    }
    
    /**
     * Sends 'signal' to process group 'group'.
     * Returns 0 on success, -errno on error.
     * http://www.opengroup.org/onlinepubs/000095399/functions/killpg.html
     */
    public static int killpg(int group, int signal) {
        return PosixJNI.killpg(group, signal);
    }
    
    /**
     * Reads 'byteCount' bytes from file descriptor 'fd' into 'buffer' at 'bufferOffset'.
     * Returns the number of bytes read, -errno on error.
     * http://www.opengroup.org/onlinepubs/000095399/functions/read.html
     */
    public static int read(int fd, byte[] buffer, int bufferOffset, int byteCount) {
        checkBufferArgs(buffer, bufferOffset, byteCount);
        return PosixJNI.read(fd, buffer, bufferOffset, byteCount);
    }
    
    /**
     * Returns the foreground process group id for tty 'fd'.
     * Returns the process group id on success, -errno on error.
     * http://www.opengroup.org/onlinepubs/000095399/functions/tcgetpgrp.html
     */
    public static int tcgetpgrp(int fd) {
        return PosixJNI.tcgetpgrp(fd);
    }
    
    /**
     * Waits for a child process to stop or terminate.
     * Returns >= 0 on success (see documentation), -errno on error.
     * http://www.opengroup.org/onlinepubs/000095399/functions/waitpid.html
     * 
     * FIXME: implement waitid(2) instead, and deprecate waitpid(2)?
     */
    public static int waitpid(int pid, WaitStatus status, int flags) {
        return PosixJNI.waitpid(pid, status, flags);
    }
    
    /**
     * Writes 'byteCount' bytes from 'bufferOffset' in 'buffer' to file descriptor 'fd'.
     * Returns the number of bytes written, -errno on error.
     * http://www.opengroup.org/onlinepubs/000095399/functions/write.html
     */
    public static int write(int fd, byte[] buffer, int bufferOffset, int byteCount) {
        checkBufferArgs(buffer, bufferOffset, byteCount);
        return PosixJNI.write(fd, buffer, bufferOffset, byteCount);
    }
    
    private static void checkBufferArgs(byte[] buffer, int bufferOffset, int byteCount) {
        if (buffer == null) {
            throw new NullPointerException("buffer == null");
        }
        if (bufferOffset < 0 || byteCount < 0) {
            throw new IllegalArgumentException("arguments must be non-negative; bufferOffset=" + bufferOffset + ", byteCount=" + byteCount);
        }
        if (bufferOffset > buffer.length || bufferOffset + byteCount > buffer.length) {
            throw new IllegalArgumentException("write out of bounds; buffer.length=" + buffer.length + ", bufferOffset=" + bufferOffset + ", byteCount=" + byteCount);
        }
    }
}
