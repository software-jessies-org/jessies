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
    
    static native int get_SIGABRT();
    static native int get_SIGALRM();
    static native int get_SIGBUS();
    static native int get_SIGCHLD();
    static native int get_SIGCONT();
    static native int get_SIGFPE();
    static native int get_SIGHUP();
    static native int get_SIGILL();
    static native int get_SIGINT();
    static native int get_SIGKILL();
    static native int get_SIGPIPE();
    static native int get_SIGQUIT();
    static native int get_SIGSEGV();
    static native int get_SIGSTOP();
    static native int get_SIGTERM();
    static native int get_SIGTSTP();
    static native int get_SIGTTIN();
    static native int get_SIGTTOU();
    static native int get_SIGUSR1();
    static native int get_SIGUSR2();
    static native int get_SIGPROF();
    static native int get_SIGSYS();
    static native int get_SIGTRAP();
    static native int get_SIGURG();
    static native int get_SIGXCPU();
    static native int get_SIGXFSZ();
    
    static native int access(String path, int accessMode);
    static native int chown(String path, int uid, int gid);
    static native int chmod(String path, int mode);
    static native int getpid();
    static native int getppid();
    static native int stat(String path, Stat stat);
    static native int lstat(String path, Stat stat);
}
