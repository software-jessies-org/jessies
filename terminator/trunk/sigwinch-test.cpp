#include <cstdio>
#include <signal.h>

/**
 * A simple test program so we can see if we're setting the tty size
 * correctly. Run in a terminal emulator, it should print "SIGWINCH"
 * as it's resized.
 */

void signal_handler(int) {
    fputs("SIGWINCH\n", stderr);
}

int main(void) {
    signal(SIGWINCH, signal_handler);
    while (true) {
        // Do nothing.
    }
    return 0;
}
