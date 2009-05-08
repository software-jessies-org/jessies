package org.jessies.os;

class PosixJNI {
    static { e.util.FileUtilities.loadNativeLibrary("posix"); }
    
    static native int get_SEEK_CUR();
    static native int get_SEEK_END();
    static native int get_SEEK_SET();
    
    static native int get_R_OK();
    static native int get_W_OK();
    static native int get_X_OK();
    static native int get_F_OK();
    
    static native int get_S_IFMT();
    static native int get_S_IFBLK();
    static native int get_S_IFCHR();
    static native int get_S_IFIFO();
    static native int get_S_IFREG();
    static native int get_S_IFDIR();
    static native int get_S_IFLNK();
    static native int get_S_IFSOCK();
    
    static native int get_S_ISUID();
    static native int get_S_ISGID();
    
    static native int access(String path, int accessMode);
    static native int chown(String path, int uid, int gid);
    static native int chmod(String path, int mode);
    static native int getpid();
    static native int getppid();
    static native int stat(String path, Stat stat);
    static native int lstat(String path, Stat stat);
}
