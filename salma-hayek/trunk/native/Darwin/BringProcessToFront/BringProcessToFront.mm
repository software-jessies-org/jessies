#include <Carbon/Carbon.h>
#include <iostream>

/*
 * On Mac OS, there's a strong notion of application. One consequence is that
 * toFront doesn't work on an application that isn't the one the user's
 * interacting with. Normally this is fine, if not preferable, but it's awkward
 * for a Java program that wants to be brought to the front via indirect user
 * request. For example, without this, it's impossible for the "evergreen" script
 * to bring Evergreen to the front on Mac OS.
 * 
 * (This file is Objective C++ simply so that the make rules add
 * "-framework Cocoa", which will drag in Carbon for us.)
 */

static void bringProcessToFront(pid_t pid) {
    ProcessSerialNumber psn;
    OSStatus status = GetProcessForPID(pid, &psn);
    if (status == noErr) {
        status = SetFrontProcess(&psn);
    }
    if (status != noErr) {
        std::cout << "pid " << pid << " = " << status << std::endl;
    }
}

int main(int, char*[]) {
    bringProcessToFront(getppid());
    return 0;
}
