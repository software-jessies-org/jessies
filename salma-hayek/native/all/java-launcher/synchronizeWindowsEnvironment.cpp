#include "synchronizeWindowsEnvironment.h"

#ifdef __CYGWIN__

#include "unix_exception.h"

#include <sys/cygwin.h>

void synchronizeWindowsEnvironment() {
    // There is a limit (perhaps 32KiB) on the amount of native Windows environment that a process can have.
    // To side-step this limit for Cygwin programs, when a Cygwin program starts another Cygwin program,
    // it passes the environment through a non-native mechanism, leaving the native environment as it found it.
    // Windows Java, perhaps in java.dll, seems to get its copy of the environment using the Windows call
    // GetEnvironmentStrings (rather than using any MSVCRT data, although that would be accessible to it).
    // So we need to synchronize the Windows environment with the Cygwin one, like cygcheck does.
    // More information, including mailing list links, is in the revision history.
    if (cygwin_internal(CW_SYNC_WINENV) == -1UL) {
        throw unix_exception("cygwin_internal(CW_SYNC_WINENV) failed - are you running a version of Cygwin older than 1.5.20?");
    }
}

#else

void synchronizeWindowsEnvironment() {
}

#endif
