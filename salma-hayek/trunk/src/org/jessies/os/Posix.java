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
     * Returns the new file descriptor on success, -errno on error.
     * http://www.opengroup.org/onlinepubs/000095399/functions/dup.html
     */
    public static int dup(int oldFd) {
        return PosixJNI.dup(oldFd);
    }
    
    /**
     * Duplicates the open file descriptor 'oldFd' as 'newFd', closing 'newFd' first if necessary.
     * Returns the new file descriptor on success, -errno on error.
     * http://www.opengroup.org/onlinepubs/000095399/functions/dup2.html
     */
    public static int dup2(int oldFd, int newFd) {
        return PosixJNI.dup2(oldFd, newFd);
    }
    
    /**
     * Changes the permissions of 'fd'.
     * Returns 0 on success, -errno on error.
     * http://www.opengroup.org/onlinepubs/000095399/functions/fchmod.html
     */
    public static int fchmod(int fd, int mode) {
        return PosixJNI.fchmod(fd, mode);
    }
    
    /**
     * Changes the user and group ownership of 'fd' to 'uid' and 'gid'.
     * Returns 0 on success, -errno on error.
     * http://www.opengroup.org/onlinepubs/000095399/functions/fchown.html
     */
    public static int fchown(int fd, int uid, int gid) {
        return PosixJNI.fchown(fd, uid, gid);
    }
    
    /**
     * Gets the file status of 'fd'.
     * Returns 0 on success, -errno on error.
     * http://www.opengroup.org/onlinepubs/000095399/functions/fstat.html
     */
    public static int fstat(int fd, Stat stat) {
        return PosixJNI.fstat(fd, stat);
    }
    
    /**
     * Truncates 'fd' to 'length'.
     * Returns 0 on success, -errno on error.
     * http://www.opengroup.org/onlinepubs/000095399/functions/ftruncate.html
     */
    public static int ftruncate(int fd, long length) {
        return PosixJNI.ftruncate(fd, length);
    }
    
    /**
     * Returns the effective group id of the calling process.
     * This function is always successful and no return value is reserved to indicate an error.
     * http://www.opengroup.org/onlinepubs/000095399/functions/getegid.html
     */
    public static int getegid() {
        return PosixJNI.getegid();
    }
    
    /**
     * Returns the effective user id of the calling process.
     * This function is always successful and no return value is reserved to indicate an error.
     * http://www.opengroup.org/onlinepubs/000095399/functions/geteuid.html
     */
    public static int geteuid() {
        return PosixJNI.geteuid();
    }
    
    /**
     * Returns the real group id of the calling process.
     * This function is always successful and no return value is reserved to indicate an error.
     * http://www.opengroup.org/onlinepubs/000095399/functions/getgid.html
     */
    public static int getgid() {
        return PosixJNI.getgid();
    }
    
    /**
     * Returns the process group id of 'pid'.
     * Returns the process group on success, -errno on error.
     * http://www.opengroup.org/onlinepubs/000095399/functions/getpgid.html
     */
    public static int getpgid(int pid) {
        return PosixJNI.getpgid(pid);
    }
    
    /**
     * Returns the process group id of the calling process.
     * This function is always successful and no return value is reserved to indicate an error.
     * http://www.opengroup.org/onlinepubs/000095399/functions/getpgrp.html
     */
    public static int getpgrp() {
        return PosixJNI.getpgrp();
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
    
    /**
     * Returns the process group id of the process that's the session leader for 'pid'.
     * Returns 0 on success, -errno on error.
     * http://www.opengroup.org/onlinepubs/000095399/functions/getsid.html
     */
    public static int getsid(int pid) {
        return PosixJNI.getsid(pid);
    }
    
    /**
     * Returns the real user id of the calling process.
     * This function is always successful and no return value is reserved to indicate an error.
     * http://www.opengroup.org/onlinepubs/000095399/functions/getuid.html
     */
    public static int getuid() {
        return PosixJNI.getuid();
    }
    
    /**
     * Tests whether 'fd' is a terminal.
     * http://www.opengroup.org/onlinepubs/000095399/functions/isatty.html
     */
    public static boolean isatty(int fd) {
        return PosixJNI.isatty(fd);
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
     * Changes the user and group ownership of the symbolic link at 'path' to 'uid' and 'gid'.
     * Returns 0 on success, -errno on error.
     * http://www.opengroup.org/onlinepubs/000095399/functions/lchown.html
     */
    public static int lchown(String path, int uid, int gid) {
        return PosixJNI.lchown(path, uid, gid);
    }
    
    /**
     * Creates a new link 'newPath' to the existing file 'oldPath'.
     * Returns 0 on success, -errno on error.
     * http://www.opengroup.org/onlinepubs/000095399/functions/link.html
     */
    public static int link(String oldPath, String newPath) {
        return PosixJNI.link(oldPath, newPath);
    }
    
    /**
     * Moves the read/write offset for 'fd'.
     * Returns the offset from the beginning of the file on success, -errno on error.
     * http://www.opengroup.org/onlinepubs/000095399/functions/lseek.html
     */
    public static long lseek(int fd, long offset, int whence) {
        if (whence != SEEK_CUR && whence != SEEK_END && whence != SEEK_SET) {
            throw new IllegalArgumentException("'whence' must be one of SEEK_CUR, SEEK_END, or SEEK_SET; got " + whence);
        }
        return PosixJNI.lseek(fd, offset, whence);
    }
    
    /**
     * Gets the status of symbolic link 'path'.
     * Returns 0 on success, -errno on error.
     * http://www.opengroup.org/onlinepubs/000095399/functions/lstat.html
     */
    public static int lstat(String path, Stat stat) {
        return PosixJNI.lstat(path, stat);
    }
    
    /**
     * Creates a new directory with name 'path' and permissions 'mode'.
     * Returns 0 on success, -errno on error.
     * http://www.opengroup.org/onlinepubs/000095399/functions/mkdir.html
     */
    public static int mkdir(String path, int mode) {
        return PosixJNI.mkdir(path, mode);
    }
    
    /**
     * Creates a new FIFO with name 'path' and permissions 'mode'.
     * Returns 0 on success, -errno on error.
     * http://www.opengroup.org/onlinepubs/000095399/functions/mkfifo.html
     */
    public static int mkfifo(String path, int mode) {
        return PosixJNI.mkfifo(path, mode);
    }
    
    /**
     * Creates a new directory, special file or regular file with name 'path', mode 'mode', and device 'device'.
     * Returns 0 on success, -errno on error.
     * http://www.opengroup.org/onlinepubs/000095399/functions/mknod.html
     */
    public static int mknod(String path, int mode, long device) {
        return PosixJNI.mknod(path, mode, device);
    }
    
    /**
     * Opens a file.
     * Returns the new fd on success, -errno on error.
     * http://www.opengroup.org/onlinepubs/000095399/functions/open.html
     */
    public static int open(String path, int flags) {
        return PosixJNI.open(path, flags);
    }
    
    /**
     * Opens a file.
     * Returns the new fd on success, -errno on error.
     * http://www.opengroup.org/onlinepubs/000095399/functions/open.html
     */
    public static int open(String path, int flags, int mode) {
        return PosixJNI.open(path, flags, mode);
    }
    
    // FIXME: pread.
    
    // FIXME: pwrite.
    
    // FIXME: read.
    
    // FIXME: readlink. How do we express the String-or-int return type? Pass in a String[] and assign to element 0?
    
    /**
     * Removes the directory 'path'.
     * Returns 0 on success, -errno on error.
     * http://www.opengroup.org/onlinepubs/000095399/functions/rmdir.html
     */
    public static int rmdir(String path) {
        return PosixJNI.rmdir(path);
    }
    
    /**
     * Gets the status of 'path'.
     * Returns 0 on success, -errno on error.
     * http://www.opengroup.org/onlinepubs/000095399/functions/stat.html
     */
    public static int stat(String path, Stat stat) {
        return PosixJNI.stat(path, stat);
    }
    
    /**
     * Creates a symbolic link from 'newpath' to 'oldpath'.
     * Returns 0 on success, -errno on error.
     * http://www.opengroup.org/onlinepubs/000095399/functions/symlink.html
     */
    public static int symlink(String oldpath, String newpath) {
        return PosixJNI.symlink(oldpath, newpath);
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
     * Truncates the file 'path' to 'length'.
     * Returns 0 on success, -errno on error.
     * http://www.opengroup.org/onlinepubs/000095399/functions/truncate.html
     */
    public static int truncate(String path, long length) {
        return PosixJNI.truncate(path, length);
    }
    
    /**
     * Removes the directory entry 'path'.
     * Returns 0 on success, -errno on error.
     * http://www.opengroup.org/onlinepubs/000095399/functions/unlink.html
     */
    public static int unlink(String path) {
        return PosixJNI.rmdir(path);
    }
    
    // FIXME: write.
}
