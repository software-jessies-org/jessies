#ifndef PTY_GENERATOR_H_included
#define PTY_GENERATOR_H_included

#include "DirectoryIterator.h"
#include "toString.h"
#include "unix_exception.h"

#include <errno.h>
#include <fcntl.h>
#include <grp.h>
#include <signal.h>
#include <stdlib.h>
#include <sys/ioctl.h>
#include <sys/stat.h>
#include <sys/time.h>
#include <sys/types.h>
#include <termios.h>
#include <unistd.h>

#if defined(__sun__)
// The POSIX STREAMS interface.
#include <stropts.h>
#endif

#include <deque>
#include <iostream>
#include <stdexcept>
#include <string>

class PtyGenerator {
    std::string slavePtyName;
    int masterFd;
    
public:
    PtyGenerator() : masterFd(-1) {
    }
    
    virtual ~PtyGenerator() {
    }
    
    std::string getSlavePtyName() {
        return slavePtyName;
    }
    
    int openMaster() {
        masterFd = posix_openpt(O_RDWR | O_NOCTTY);
        if (masterFd == -1) {
            throw unix_exception("posix_openpt(O_RDWR | O_NOCTTY) failed");
        }
        
        const char* name = ptsname(masterFd);
        if (name == 0) {
            throw unix_exception("ptsname(" + toString(masterFd) + ") failed");
        }
        slavePtyName = name;
        
        if (grantpt(masterFd) != 0) {
            throw unix_exception("grantpt(\"" + slavePtyName + "\") failed");
        }
        if (unlockpt(masterFd) != 0) {
            throw unix_exception("unlockpt(\"" + slavePtyName + "\") failed");
        }
        
        // Check that we can open the slave.
        // That's the one failure that in the slave that we cannot otherwise report.
        // When Cygwin runs out of ptys, the slave open failing is the first we get to know about it.
        //close(openSlave());
        
        return masterFd;
    }
    
    pid_t forkAndExec(const std::string& term, const std::string& executable, char * const *argv, const std::string& workingDirectory) {
        pid_t pid = fork();
        if (pid < 0) {
            throw unix_exception("fork() failed");
        } else if (pid == 0) {
            try {
                runChild(term, executable, argv, workingDirectory, *this);  // Should never return.
            } catch (const std::exception& ex) {
                // reportFatalErrorViaGui would have a better chance of being seen.
                fprintf(stderr, "%s\n", ex.what());
            }
            exit(1); // We're only exit()ing the child, not the VM.
        } else {
            return pid;
        }
    }
    
private:
    class child_exception : public unix_exception {
    public:
        explicit child_exception(const std::string& message)
        : unix_exception("Error from child: " + message) {
        }
    };
    
    class child_exception_via_pipe : child_exception {
    public:
        child_exception_via_pipe(int pipeFd, const std::string& message)
        : child_exception(message) {
            // Take special measures to ensure that the error message is displayed,
            // given that std::cerr may not be working at this point.
            FILE* tunnel = fdopen(pipeFd, "w");
            if (tunnel != 0) {
                fprintf(tunnel, "%s\n", what());
            }
        }
    };
    
    int openSlave() {
        // The first terminal opened by a System V process becomes its controlling terminal.
        int slaveFd = open(slavePtyName.c_str(), O_RDWR);
        if (slaveFd == -1) {
            throw unix_exception("open(\"" + slavePtyName + "\", O_RDWR) failed - did you run out of pseudo-terminals?");
        }
        return slaveFd;
    }
    
    static void runChild(const std::string& term, const std::string& executable, char * const *argv, const std::string& workingDirectory, PtyGenerator& ptyGenerator) {
        if (workingDirectory.length() != 0) {
            if (chdir(workingDirectory.c_str()) == -1) {
                throw child_exception("chdir(\"" + workingDirectory + "\")");
            }
        }
        
        // A process relinquishes its controlling terminal when it creates a new session with the setsid(2) function.
        if (setsid() == -1) {
            throw child_exception("setsid()");
        }
        
        int childFd = ptyGenerator.openSlave();
        close(ptyGenerator.masterFd);

#if defined(TIOCSCTTY) && !defined(__sun__) && !defined(__CYGWIN__)
        // The BSD approach is that the controlling terminal for a session is allocated by the session leader by issuing the TIOCSCTTY ioctl.
        // Solaris' termios.h 1.42 now includes a TIOCSCTTY definition, resulting in inappropriate ioctl errors.
        // Once upon a time, there was a suggestion in a comment that earlier SunOS versions might need to avoid this too.
        // This code is believed unnecessary (now we get O_NOCTTY right) but harmless on Linux.
        // We need to use this code on Mac OS, or we get an inappropriate ioctl error from the immediately following tcgetpgrp.
        // APUE says that FreeBSD needs it too.
        if (ioctl(childFd, TIOCSCTTY, 0) == -1) {
            throw child_exception_via_pipe(childFd, "ioctl(" + toString(childFd) + ", TIOCSCTTY, 0)");
        }
#endif
        pid_t terminalProcessGroup = tcgetpgrp(childFd);
        if (terminalProcessGroup == -1) {
            throw child_exception_via_pipe(childFd, "tcgetpgrp(" + toString(childFd) + ")");
        }
        if (terminalProcessGroup != getpid()) {
            errno = 0; // We're abusing unix_exception here.
            throw child_exception_via_pipe(childFd, "tcgetpgrp(" + toString(childFd) + ") (" + toString(terminalProcessGroup) + ") != getpid() (" + toString(getpid())+ ")");
        }

#if defined(__sun__)
        // This seems to be necessary on Solaris to make STREAMS behave.
        ioctl(childFd, I_PUSH, "ptem");
        ioctl(childFd, I_PUSH, "ldterm");
        ioctl(childFd, I_PUSH, "ttcompat");
#endif
        
        termios terminalAttributes;
        if (tcgetattr(childFd, &terminalAttributes) != 0) {
            throw child_exception_via_pipe(childFd, "tcgetattr(" + toString(childFd) + ", &terminalAttributes)");
        }
        // Humans don't need XON/XOFF flow control of output, and it only serves to confuse those who accidentally hit ^S or ^Q, so turn it off.
        terminalAttributes.c_iflag &= ~IXON;
#if defined(IUTF8)
        // Assume input is UTF-8; this allows character-erase to be correctly performed in cooked mode.
        terminalAttributes.c_iflag |= IUTF8;
#endif
        
        // The equivalent of stty erase ^? for the benefit of Cygwin-1.5, which defaults to ^H.
        // Our belief is that every other platform we care about, including Cygwin-1.7, uses ^?.
        // We used to send ^H on Windows.
        // That worked OK with ssh in Cygwin-1.5 because ssh passes stty erase to the remote system.
        // Only "OK" because it hid the Emacs help, which someone once complained about to the mailing list.
        // It didn't work so well with Cygwin telnet, which neither translated ^H to ^?, nor passed stty erase.
        // One reason I appear to have thought it necessary to send ^H is for the benefit of native Windows applications, writing:
        // "Windows's ReadConsoleInput function always provides applications, like jdb, with ^H, so that's what they expect."
        // However, ReadConsoleInput seems never to return when a Cygwin pty is providing the input, even in 1.5, so I now think that's irrelevant.
        // Search the change log for "backspace" for more information.
        terminalAttributes.c_cc[VERASE] = 127;
        
        if (tcsetattr(childFd, TCSANOW, &terminalAttributes) != 0) {
            throw child_exception_via_pipe(childFd, "tcsetattr(" + toString(childFd) + ", TCSANOW, &terminalAttributes) with IXON cleared");
        }
        
        // Slave becomes stdin/stdout/stderr of child.
        if (childFd != STDIN_FILENO && dup2(childFd, STDIN_FILENO) != STDIN_FILENO) {
            throw child_exception_via_pipe(childFd, "dup2(" + toString(childFd) + ", STDIN_FILENO)");
        }
        if (childFd != STDOUT_FILENO && dup2(childFd, STDOUT_FILENO) != STDOUT_FILENO) {
            throw child_exception_via_pipe(childFd, "dup2(" + toString(childFd) + ", STDOUT_FILENO)");
        }
        if (childFd != STDERR_FILENO && dup2(childFd, STDERR_FILENO) != STDERR_FILENO) {
            throw child_exception_via_pipe(childFd, "dup2(" + toString(childFd) + ", STDERR_FILENO)");
        }
        closeFileDescriptors();
        fixEnvironment(term);
        
        // rxvt resets these signal handlers, and we'll do the same, because it magically
        // fixes the bug where ^c doesn't work if we're launched from KDE or Gnome's
        // launcher program.  I don't quite understand why - maybe bash reads the existing
        // SIGINT setting, and if it's set to something other than DFL it lets the parent process
        // take care of job control.
        
        // David Korn asks us to consider the case where...
        // ...a process has SIGCHLD set to SIG_IGN and then execs a new
        // process.  A conforming application would not set  SIGCHLD to SIG_IGN
        // since the standard leaves this behavior unspecified.  An application
        // that does set SIGCHLD to SIG_IGN  should set it back to SIG_DFL
        // before the call to exec.
        // http://www.pasc.org/interps/unofficial/db/p1003.1/pasc-1003.1-132.html
        signal(SIGINT, SIG_DFL);
        signal(SIGQUIT, SIG_DFL);
        signal(SIGCHLD, SIG_DFL);
        
        execvp(executable.c_str(), argv);
        throw unix_exception("Can't execute \"" + executable + "\"");
    }
    
    static void fixEnvironment(const std::string& term) {
        // Tell the world which terminfo entry to use.
        setenv("TERM", term.c_str(), 1);
        // According to Thomas Dickey in the XTERM FAQ, some applications that don't use ncurses may need the environment variable $COLORTERM set to realize that they're on a color terminal.
        // Most of the other Unix terminals set it.
        setenv("COLORTERM", term.c_str(), 1);
        
        // X11 terminal emulators set this, but we can't reasonably do so, even on X11.
        // http://elliotth.blogspot.com/2005/12/why-terminator-doesnt-support-windowid.html
        unsetenv("WINDOWID");
        
        // The JVM sets LD_LIBRARY_PATH, but this upsets some applications.
        // We complained in 2005 (Sun bug 6354700), but there's no sign of progress.
        // FIXME: write the initial value to a system property in "invoke-java.rb" and set it back here?
        unsetenv("LD_LIBRARY_PATH");
        
#ifdef __APPLE__
        // Apple's Java launcher uses environment variables to implement the -Xdock options.
        pid_t ppid = getppid();
        unsetenv(("APP_ICON_" + toString(ppid)).c_str());
        unsetenv(("APP_NAME_" + toString(ppid)).c_str());
        unsetenv(("JAVA_MAIN_CLASS_" + toString(ppid)).c_str());
        
        // Apple's Terminal sets these, and some programs/scripts identify Terminal this way.
        // In real life, these shouldn't be set, but they will be if we're debugging and being run from Terminal.
        // It's always confusing when programs behave differently during debugging!
        unsetenv("TERM_PROGRAM");
        unsetenv("TERM_PROGRAM_VERSION");
#endif
    }
    
    /**
     * This allows the our server port socket to close when we quit
     * while a child is still running.
     * 
     * It also ensures that child processes don't have file descriptors for
     * files the Java VM has open (it typically has many).
     */
    static void closeFileDescriptors() {
        // A common idiom for closing the parent's file descriptors in a child is to close all possible file descriptors.
        // Sun 4843136 refers to this technique as a "stress test for the OS", pointing out that a system may have a high, or no, limit.
        // Sun 4413680 claims that the equivalent code in the JVM before 1.4.0_03 was a performance problem on Solaris.
        // Solaris offers closefrom(3), though none of our other platforms appears to.
        // BSD offers fcntl(F_CLOSEM) but none of our platforms appears to.
        
        // On Cygwin, Linux, and Solaris, a better solution iterates over "/proc/self/fd/".
        std::string fdDirectory("/proc/self/fd");
#ifdef __APPLE__
        // On Mac OS, there's "/dev/fd/" (which Linux seems to link to "/proc/self/fd/", but which on Solaris appears to be something quite different).
        fdDirectory = "/dev/fd";
#endif
        
        // There's no portable way to get the opendir(3) file descriptor, so we use two passes.
        // Pass 1: collect the fds to close.
        typedef std::deque<int> FdList;
        FdList fds;
        for (DirectoryIterator it(fdDirectory); it.isValid(); ++it) {
            int fd = strtoul(it->getName().c_str(), NULL, 10);
            if (fd > STDERR_FILENO) {
                fds.push_back(fd);
            }
        }
        
        // Pass 2: close the fds.
        for (FdList::const_iterator it = fds.begin(); it != fds.end(); ++it) {
            // The close of the opendir(3) file descriptor will fail, but we ignore that.
            close(*it);
        }
    }
    
#ifdef __CYGWIN__
    // Before 2006-07-18, Cygwin didn't have posix_openpt(3).
    // I'm not sure which release that first went into.
    // As of the 2008-06-12 release, it's still not exported from cygwin1.dll unlike, say, grantpt.
    // This is the same implementation as the one in the Cygwin source.
    int posix_openpt(int flags) {
        return open("/dev/ptmx", flags);
    }
#endif
};

#endif
