#include "pty.h"

static int search_for_pty(std::string& pts_name) {
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
                    panic("out of pseudo-terminal devices");
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
    panic("out of pseudo-terminal devices");
}

int ptym_open(std::string& pts_name) {
    int ptmx_fd = open("/dev/ptmx", O_RDWR);
    if (ptmx_fd < 0) {
        return search_for_pty(pts_name);
    }

    const char* name = ptsname(ptmx_fd);
    if (name == 0) {
        std::ostringstream oss;
        oss << ptmx_fd;
        panic("failed to get ptsname for fd", oss.str().c_str());
    }
    pts_name = name;
    
    if (grantpt(ptmx_fd) != 0) {
        panic("failed to get grantpt", name);
    }
    if (unlockpt(ptmx_fd) != 0) {
        panic("failed to get unlockpt", name);
    }
    
    return ptmx_fd;
}

int ptys_open(const std::string& pts_name) {
    struct group* grptr = getgrnam("tty");
    gid_t gid = (grptr != NULL) ? grptr->gr_gid : gid_t(-1);
    
    const char* name = pts_name.c_str();
    chown(name, getuid(), gid);
    chmod(name, S_IRUSR | S_IWUSR | S_IWGRP);
    
    int fds = open(name, O_RDWR);
    if (fds < 0) {
        panic("failed to open", name);
    }
    return fds;
}
