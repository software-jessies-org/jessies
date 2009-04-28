package org.jessies.os;

import static org.jessies.os.Posix.*;

/**
 * File information returned by Posix.lstat and Posix.stat.
 * http://www.opengroup.org/onlinepubs/000095399/basedefs/sys/stat.h.html
 */
public class Stat {
    private long /*dev_t*/ st_dev;
    private long /*ino_t*/ st_ino;
    private int /*mode_t*/ st_mode;
    private long /*nlink_t*/ st_nlink;
    private int /*uid_t*/ st_uid;
    private int /*gid_t*/ st_gid;
    private long /*dev_t*/ st_rdev;
    private long /*off_t*/ st_size;
    private long /*time_t*/ st_atime;
    private long /*time_t*/ st_mtime;
    private long /*time_t*/ st_ctime;
    private long /*blksize_t*/ st_blksize;
    private long /*blkcnt_t*/ st_blocks;
    
    /** Device ID of device containing file. */
    public long st_dev() { return st_dev; }
    
    /** File serial number. */
    public long st_ino() { return st_ino; }
    
    /** Mode of file. */
    public int st_mode() { return st_mode; }
    
    /** Number of hard links to the file. */
    public long st_nlink() { return st_nlink; }
    
    /** User ID of file. */
    public int st_uid() { return st_uid; }
    
    /** Group ID of file. */
    public int st_gid() { return st_gid; }
    
    /** Device ID (if file is character or block special). */
    public long st_rdev() { return st_rdev; }
    
    /**
     * For regular files, the file size in bytes.
     * For symbolic links, the length in bytes of the pathname contained in the symbolic link.
     * For a shared memory object, the length in bytes.
     * For a typed memory object, the length in bytes.
     * For other file types, the use of this field is unspecified.
     */
    public long st_size() { return st_size; }
    
    /** Time of last access. */
    public long st_atime() { return st_atime; }
    
    /** Time of last data modification. */
    public long st_mtime() { return st_mtime; }
    
    /** Time of last status change. */
    public long st_ctime() { return st_ctime; }
    
    /**
     * A file system-specific preferred I/O block size for this object.
     * In some file system types, this may vary from file to file.
     */
    public long st_blksize() { return st_blksize; }
    
    /** Number of blocks allocated for this object. */
    public long st_blocks() { return st_blocks; }
    
    /** Test for a block special file. */
    public boolean isBlk() { return (st_mode() & S_IFMT) == S_IFBLK; }
    /** Test for a character special file. */
    public boolean isChr() { return (st_mode() & S_IFMT) == S_IFCHR; }
    /** Test for a directory. */
    public boolean isDirectory() { return (st_mode() & S_IFMT) == S_IFDIR; }
    /** Test for a pipe or FIFO special file. */
    public boolean isFIFO() { return (st_mode() & S_IFMT) == S_IFIFO; }
    /** Test for a regular file. */
    public boolean isRegular() { return (st_mode() & S_IFMT) == S_IFREG; }
    /** Test for a symbolic link. */
    public boolean isSymbolicLink() { return (st_mode() & S_IFMT) == S_IFLNK; }
    /** Test for a socket. */
    public boolean isSocket() { return (st_mode() & S_IFMT) == S_IFSOCK; }
    
    public Stat() {
    }
    
    private void set(long st_dev, long st_ino, int st_mode, long st_nlink, int st_uid, int st_gid, long st_rdev, long st_size, long st_atime, long st_mtime, long st_ctime, long st_blksize, long st_blocks) {
        this.st_dev = st_dev;
        this.st_ino = st_ino;
        this.st_mode = st_mode;
        this.st_nlink = st_nlink;
        this.st_uid = st_uid;
        this.st_gid = st_gid;
        this.st_rdev = st_rdev;
        this.st_size = st_size;
        this.st_atime = st_atime;
        this.st_mtime = st_mtime;
        this.st_ctime = st_ctime;
        this.st_blksize = st_blksize;
        this.st_blocks = st_blocks;
    }
}
