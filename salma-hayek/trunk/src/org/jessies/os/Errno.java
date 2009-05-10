package org.jessies.os;

public class Errno {
    /**
     * Translates error 'errno' to a human-readable string.
     */
    public static String toString(int errno) {
        return PosixJNI.strerror(errno);
    }
    
    public static final int E2BIG = PosixJNI.get_E2BIG();
    public static final int EACCES = PosixJNI.get_EACCES();
    public static final int EADDRINUSE = PosixJNI.get_EADDRINUSE();
    public static final int EADDRNOTAVAIL = PosixJNI.get_EADDRNOTAVAIL();
    public static final int EAFNOSUPPORT = PosixJNI.get_EAFNOSUPPORT();
    public static final int EAGAIN = PosixJNI.get_EAGAIN();
    public static final int EALREADY = PosixJNI.get_EALREADY();
    public static final int EBADF = PosixJNI.get_EBADF();
    public static final int EBADMSG = PosixJNI.get_EBADMSG();
    public static final int EBUSY = PosixJNI.get_EBUSY();
    public static final int ECANCELED = PosixJNI.get_ECANCELED();
    public static final int ECHILD = PosixJNI.get_ECHILD();
    public static final int ECONNABORTED = PosixJNI.get_ECONNABORTED();
    public static final int ECONNREFUSED = PosixJNI.get_ECONNREFUSED();
    public static final int ECONNRESET = PosixJNI.get_ECONNRESET();
    public static final int EDEADLK = PosixJNI.get_EDEADLK();
    public static final int EDESTADDRREQ = PosixJNI.get_EDESTADDRREQ();
    public static final int EDOM = PosixJNI.get_EDOM();
    public static final int EDQUOT = PosixJNI.get_EDQUOT();
    public static final int EEXIST = PosixJNI.get_EEXIST();
    public static final int EFAULT = PosixJNI.get_EFAULT();
    public static final int EFBIG = PosixJNI.get_EFBIG();
    public static final int EHOSTUNREACH = PosixJNI.get_EHOSTUNREACH();
    public static final int EIDRM = PosixJNI.get_EIDRM();
    public static final int EILSEQ = PosixJNI.get_EILSEQ();
    public static final int EINPROGRESS = PosixJNI.get_EINPROGRESS();
    public static final int EINTR = PosixJNI.get_EINTR();
    public static final int EINVAL = PosixJNI.get_EINVAL();
    public static final int EIO = PosixJNI.get_EIO();
    public static final int EISCONN = PosixJNI.get_EISCONN();
    public static final int EISDIR = PosixJNI.get_EISDIR();
    public static final int ELOOP = PosixJNI.get_ELOOP();
    public static final int EMFILE = PosixJNI.get_EMFILE();
    public static final int EMLINK = PosixJNI.get_EMLINK();
    public static final int EMSGSIZE = PosixJNI.get_EMSGSIZE();
    public static final int EMULTIHOP = PosixJNI.get_EMULTIHOP();
    public static final int ENAMETOOLONG = PosixJNI.get_ENAMETOOLONG();
    public static final int ENETDOWN = PosixJNI.get_ENETDOWN();
    public static final int ENETRESET = PosixJNI.get_ENETRESET();
    public static final int ENETUNREACH = PosixJNI.get_ENETUNREACH();
    public static final int ENFILE = PosixJNI.get_ENFILE();
    public static final int ENOBUFS = PosixJNI.get_ENOBUFS();
    public static final int ENODEV = PosixJNI.get_ENODEV();
    public static final int ENOENT = PosixJNI.get_ENOENT();
    public static final int ENOEXEC = PosixJNI.get_ENOEXEC();
    public static final int ENOLCK = PosixJNI.get_ENOLCK();
    public static final int ENOLINK = PosixJNI.get_ENOLINK();
    public static final int ENOMEM = PosixJNI.get_ENOMEM();
    public static final int ENOMSG = PosixJNI.get_ENOMSG();
    public static final int ENOPROTOOPT = PosixJNI.get_ENOPROTOOPT();
    public static final int ENOSPC = PosixJNI.get_ENOSPC();
    public static final int ENOSYS = PosixJNI.get_ENOSYS();
    public static final int ENOTCONN = PosixJNI.get_ENOTCONN();
    public static final int ENOTDIR = PosixJNI.get_ENOTDIR();
    public static final int ENOTEMPTY = PosixJNI.get_ENOTEMPTY();
    public static final int ENOTSOCK = PosixJNI.get_ENOTSOCK();
    public static final int ENOTSUP = PosixJNI.get_ENOTSUP();
    public static final int ENOTTY = PosixJNI.get_ENOTTY();
    public static final int ENXIO = PosixJNI.get_ENXIO();
    public static final int EOPNOTSUPP = PosixJNI.get_EOPNOTSUPP();
    public static final int EOVERFLOW = PosixJNI.get_EOVERFLOW();
    public static final int EPERM = PosixJNI.get_EPERM();
    public static final int EPIPE = PosixJNI.get_EPIPE();
    public static final int EPROTO = PosixJNI.get_EPROTO();
    public static final int EPROTONOSUPPORT = PosixJNI.get_EPROTONOSUPPORT();
    public static final int EPROTOTYPE = PosixJNI.get_EPROTOTYPE();
    public static final int ERANGE = PosixJNI.get_ERANGE();
    public static final int EROFS = PosixJNI.get_EROFS();
    public static final int ESPIPE = PosixJNI.get_ESPIPE();
    public static final int ESRCH = PosixJNI.get_ESRCH();
    public static final int ESTALE = PosixJNI.get_ESTALE();
    public static final int ETIMEDOUT = PosixJNI.get_ETIMEDOUT();
    public static final int ETXTBSY = PosixJNI.get_ETXTBSY();
    public static final int EWOULDBLOCK = PosixJNI.get_EWOULDBLOCK();
    public static final int EXDEV = PosixJNI.get_EXDEV();
}
