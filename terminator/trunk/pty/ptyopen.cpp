#include "pty.h"

#ifdef __APPLE__

int ptym_open(std::string& pts_name) {
    for (char* ptr1 = "pqrstuvwxyzPQRST"; *ptr1 != 0; ++ptr1) {
        for (char* ptr2 = "0123456789abcdef"; *ptr2 != 0; ++ptr2) {
            pts_name = "/dev/pty";
            pts_name.append(1, *ptr1);
            pts_name.append(1, *ptr2);

            /* try to open master */
            int fdm = open(pts_name.c_str(), O_RDWR);
            if (fdm < 0) {
                if (errno == ENOENT) {  /* different from EIO */
                    return -1;          /* out of pty devices */
                } else {
                    continue;           /* try next pty device */
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
    return -1;        /* out of pty devices */
}

#else

int ptym_open(std::string& pts_name) {
    int ptmx_fd = open("/dev/ptmx", O_RDWR);
    if (ptmx_fd < 0) {
        return -1;
    }
    
    const char* name = ptsname(ptmx_fd);
    if (name == 0) {
        return -1;
    }
    pts_name = name;
    
    if (grantpt(ptmx_fd) != 0) {
        return -1;
    }
    if (unlockpt(ptmx_fd) != 0) {
        return -1;
    }
    
    return ptmx_fd;
}

#endif

int ptys_open(int fdm, const char* pts_name) {
    struct group* grptr;
    int gid, fds;

    if ( (grptr = getgrnam("tty")) != NULL)
        gid = grptr->gr_gid;
    else
        gid = -1;        /* group tty is not in the group file */

            /* following two functions don't work unless we're root */
    chown(pts_name, getuid(), gid);
    chmod(pts_name, S_IRUSR | S_IWUSR | S_IWGRP);

    if ( (fds = open(pts_name, O_RDWR)) < 0) {
        close(fdm);
        return(-1);
    }
    return(fds);
}
