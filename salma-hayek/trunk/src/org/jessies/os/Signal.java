package org.jessies.os;

/**
 * POSIX signal numbers.
 * http://www.opengroup.org/onlinepubs/000095399/basedefs/signal.h.html
 */
public class Signal {
    public static final int SIGABRT = PosixJNI.get_SIGABRT();
    public static final int SIGALRM = PosixJNI.get_SIGALRM();
    public static final int SIGBUS = PosixJNI.get_SIGBUS();
    public static final int SIGCHLD = PosixJNI.get_SIGCHLD();
    public static final int SIGCONT = PosixJNI.get_SIGCONT();
    public static final int SIGFPE = PosixJNI.get_SIGFPE();
    public static final int SIGHUP = PosixJNI.get_SIGHUP();
    public static final int SIGILL = PosixJNI.get_SIGILL();
    public static final int SIGINT = PosixJNI.get_SIGINT();
    public static final int SIGKILL = PosixJNI.get_SIGKILL();
    public static final int SIGPIPE = PosixJNI.get_SIGPIPE();
    public static final int SIGQUIT = PosixJNI.get_SIGQUIT();
    public static final int SIGSEGV = PosixJNI.get_SIGSEGV();
    public static final int SIGSTOP = PosixJNI.get_SIGSTOP();
    public static final int SIGTERM = PosixJNI.get_SIGTERM();
    public static final int SIGTSTP = PosixJNI.get_SIGTSTP();
    public static final int SIGTTIN = PosixJNI.get_SIGTTIN();
    public static final int SIGTTOU = PosixJNI.get_SIGTTOU();
    public static final int SIGUSR1 = PosixJNI.get_SIGUSR1();
    public static final int SIGUSR2 = PosixJNI.get_SIGUSR2();
    public static final int SIGPROF = PosixJNI.get_SIGPROF();
    public static final int SIGSYS = PosixJNI.get_SIGSYS();
    public static final int SIGTRAP = PosixJNI.get_SIGTRAP();
    public static final int SIGURG = PosixJNI.get_SIGURG();
    public static final int SIGXCPU = PosixJNI.get_SIGXCPU();
    public static final int SIGXFSZ = PosixJNI.get_SIGXFSZ();
}
