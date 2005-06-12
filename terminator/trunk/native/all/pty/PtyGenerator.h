#ifndef PTY_GENERATOR_H_included
#define PTY_GENERATOR_H_included

#include <errno.h>
#include <fcntl.h>
#include <grp.h>
#include <sys/ioctl.h>
#include <sys/stat.h>
#include <sys/types.h>

#include <iostream>
#include <sstream>
#include <stdexcept>
#include <string>

class PtyGenerator {
    std::string ptyName;
    int masterFd;
    
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
            std::ostringstream oss;
            oss << "Failed to get ptysname for file descriptor " << ptmx_fd << errnoToString();
            throw std::runtime_error(oss.str());
        }
        pts_name = name;
        
        if (grantpt(ptmx_fd) != 0) {
            std::ostringstream oss;
            oss << "Failed to get grantpt for " << name << errnoToString();
            throw std::runtime_error(oss.str());
        }
        if (unlockpt(ptmx_fd) != 0) {
            std::ostringstream oss;
            oss << "Failed to get unlockpt for " << name << errnoToString();
            throw std::runtime_error(oss.str());
        }
        return ptmx_fd;
    }
    
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
            std::ostringstream oss;
            oss << "Failed to open " << cName << errnoToString();
            throw std::runtime_error(oss.str());
        }
        close(masterFd);
        return fds;
    }
};

#endif
