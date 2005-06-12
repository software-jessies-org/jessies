#ifndef PTY_GENERATOR_H_included
#define PTY_GENERATOR_H_included

#include "toString.h"
#include "UnixException.h"

#include <errno.h>
#include <fcntl.h>
#include <grp.h>
#include <signal.h>
#include <stdlib.h>
#include <sys/ioctl.h>
#include <sys/stat.h>
#include <sys/types.h>

#include <iostream>
#include <stdexcept>
#include <string>

class PtyGenerator {
    std::string ptyName;
    int masterFd;
    
public:
    PtyGenerator() : masterFd(-1) {
    }
    
    virtual ~PtyGenerator() {
    }
    
    int openMaster() {
        masterFd = ptym_open(ptyName);
        return masterFd;
    }
    
    int openSlaveAndCloseMaster() {
        struct group* grptr = getgrnam("tty");
        gid_t gid = (grptr != NULL) ? grptr->gr_gid : gid_t(-1);
        
        const char *cName = ptyName.c_str();
        chown(cName, getuid(), gid);
        chmod(cName, S_IRUSR | S_IWUSR | S_IWGRP);
        
        int fds = open(cName, O_RDWR);
        if (fds < 0) {
            throw UnixException("open(" + toString(cName) + ", O_RDWR)");
        }
        close(masterFd);
        return fds;
    }
    
    pid_t forkAndExec(char * const *cmd) {
        pid_t pid = fork();
        if (pid < 0) {
            return -1;
        } else if (pid == 0) {
            try {
                runChild(cmd, *this);  // Should never return.
            } catch (const std::exception& ex) {
                std::cerr << ex.what() << std::endl;
            }
            exit(1); // We're only exit()ing the child, not the VM.
        } else {
            return pid;
        }
    }
    
private:
    int search_for_pty(std::string& pts_name) {
        for (char* ptr1 = "pqrstuvwxyzPQRST"; *ptr1 != 0; ++ptr1) {
            for (char* ptr2 = "0123456789abcdef"; *ptr2 != 0; ++ptr2) {
                pts_name = "/dev/pty";
                pts_name.append(1, *ptr1);
                pts_name.append(1, *ptr2);
                
                /* try to open master */
                int fdm = open(pts_name.c_str(), O_RDWR);
                if (fdm < 0) {
                    if (errno == ENOENT) {
                        /* Different from EIO. */
                        throw std::runtime_error("Out of pseudo-terminal devices");
                    } else {
                        /* Try next pty device. */
                        continue;
                    }
                }
                
                /* Return name of slave. */
                pts_name = "/dev/tty";
                pts_name.append(1, *ptr1);
                pts_name.append(1, *ptr2);
                
                /* Return fd of master. */
                return fdm;
            }
        }
        throw std::runtime_error("Out of pseudo-terminal devices");
    }
    
    int ptym_open(std::string& pts_name) {
        int ptmx_fd = open("/dev/ptmx", O_RDWR);
        if (ptmx_fd < 0) {
            return search_for_pty(pts_name);
        }
        
        const char* name = ptsname(ptmx_fd);
        if (name == 0) {
            throw UnixException("ptsname(" + toString(ptmx_fd) + ")");
        }
        pts_name = name;
        
        if (grantpt(ptmx_fd) != 0) {
            throw UnixException("grantpt(" + toString(name) + ")");
        }
        if (unlockpt(ptmx_fd) != 0) {
            throw UnixException("unlockpt(" + toString(name) + ")");
        }
        return ptmx_fd;
    }
    
    class ChildException : public UnixException {
    public:
        ChildException(const std::string& message)
        : UnixException("Error from child: " + message) {
        }
    };
    
    class ChildExceptionViaPipe : ChildException {
    public:
        ChildExceptionViaPipe(int pipeFd, const std::string& message)
        : ChildException(message) {
            // Take special measures to ensure that the error message is displayed,
            // given that std::err may not be working at this point.
            FILE* tunnel = fdopen(pipeFd, "w");
            fprintf(tunnel, "%s\n", what());
        }
    };
    
    static void runChild(char * const *cmd, PtyGenerator& ptyGenerator) {
        if (setsid() < 0) {
            throw ChildException("setsid()");
        }
        int childFd = ptyGenerator.openSlaveAndCloseMaster();
#if defined(TIOCSCTTY) && !defined(CIBAUD)
        /* 44BSD way to acquire controlling terminal */
        /* !CIBAUD to avoid doing this under SunOS */
        if (ioctl(childFd, TIOCSCTTY, 0) < 0) {
            throw ChildExceptionViaPipe(childFd, "ioctl(" + toString(childFd) + ", TIOCSCTTY, 0)");
        }
#endif
        /* slave becomes stdin/stdout/stderr of child */
        if (dup2(childFd, STDIN_FILENO) != STDIN_FILENO) {
            throw ChildExceptionViaPipe(childFd, "dup2(" + toString(childFd) + ", STDIN_FILENO)");
        }
        if (dup2(childFd, STDOUT_FILENO) != STDOUT_FILENO) {
            throw ChildExceptionViaPipe(childFd, "dup2(" + toString(childFd) + ", STDOUT_FILENO)");
        }
        if (dup2(childFd, STDERR_FILENO) != STDERR_FILENO) {
            throw ChildExceptionViaPipe(childFd, "dup2(" + toString(childFd) + ", STDERR_FILENO)");
        }
        if (childFd > STDERR_FILENO) {
            close(childFd);
        }
        putenv("TERM=terminator");
        
        /*
         * rxvt resets these signal handlers, and we'll do the same, because it magically
         * fixes the bug where ^c doesn't work if we're launched from KDE or Gnome's
         * launcher program.  I don't quite understand why - maybe bash reads the existing
         * SIGINT setting, and if it's set to something other than DFL it lets the parent process
         * take care of job control.
         */
        signal(SIGINT, SIG_DFL);
        signal(SIGQUIT, SIG_DFL);
        signal(SIGCHLD, SIG_DFL);
        
        execvp(cmd[0], cmd);
        throw ChildException("Can't execute '" + toString(cmd[0]) + "'");
    }
};

#endif
