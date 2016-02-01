#include <cstdlib>
#include <iostream>
#import <Carbon/Carbon.h>
#import "ScopedAutoReleasePool.h"

// Outputs the path to the applications corresponding to any bundle ids given as arguments.
int main(int, char* args[]) {
    ScopedAutoReleasePool pool;
    std::string argv0(*args++);
    for (; *args; ++args) {
        NSString* bundleId = [NSString stringWithUTF8String:*args];
        CFURLRef appUrl = 0;
        OSStatus err = LSFindApplicationForInfo(kLSUnknownCreator, (CFStringRef) bundleId, 0, 0, &appUrl);
        if (err == noErr) {
            NSString* appPath = [(NSURL*) appUrl path];
            std::cout << [appPath UTF8String] << std::endl;
            CFRelease(appUrl);
        } else {
            std::cerr << argv0 << ": failed to find application with bundle id \"" << [bundleId UTF8String] << "\": " << err << std::endl;
            return EXIT_FAILURE;
        }
    }
    return EXIT_SUCCESS;
}
