/*
 * The following stub functions are just to allow libpty to compile properly on
 * MacOSX, which lacks these functions.
 */

char *ptsname(int fd) {
    (void) fd;
    return 0;
}

int grantpt(int fd) {
    (void) fd;
    return -1;
}

int unlockpt(int fd) {
    (void) fd;
    return -1;
}
