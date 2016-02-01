#include "updateLoginRecord.h"

#include "unix_exception.h"

#include <pwd.h>
#include <sstream>
#include <stdlib.h>
#include <string>
#include <string.h>
#include <sys/time.h>
#include <sys/types.h>
#include <unistd.h>
#include <utmpx.h>

void updateLoginRecord(int fd, pid_t pid, const std::string& slavePtyName) {
    utmpx ut;
    memset(&ut, 0, sizeof(ut));
    ut.ut_type = fd == -1 ? DEAD_PROCESS : USER_PROCESS;
    ut.ut_pid = pid;
    std::string line = slavePtyName.substr(strlen("/dev/"));
    strncpy(&ut.ut_line[0], &line[0], sizeof(ut.ut_line) - 1);
    // I get /dev/pts/<n> but this is what man pututxline suggests.
    // It seems consistent with what gnome-terminal leaves in /var/run/utmp.
    std::string id = slavePtyName.substr(strlen("/dev/tty"));
    strncpy(&ut.ut_id[0], &id[0], sizeof(ut.ut_id) - 1);
    const passwd* pw = getpwuid(getuid());
    if (pw != 0) {
        strncpy(&ut.ut_user[0], pw->pw_name, sizeof(ut.ut_user) - 1);
    }
    const char* display = getenv("DISPLAY");
    if (display != 0) {
        strncpy(&ut.ut_host[0], display, sizeof(ut.ut_host) - 1);
    }
    // Like http://pubs.opengroup.org/onlinepubs/7908799/xsh/utmpx.h.html,
    // Cygwin doesn't have ut_session, so I guess it isn't important.
    //ut.ut_session = pid;
    timeval tv;
    if (gettimeofday(&tv, 0) == -1) {
        throw unix_exception("gettimeofday(&tv, 0) failed");
    }
    ut.ut_tv.tv_sec = tv.tv_sec;
    ut.ut_tv.tv_usec = tv.tv_usec;
    setutxent();
    if (pututxline(&ut) == 0) {
        std::ostringstream os;
        os << "pututxline(" << ut.ut_type << ", " << ut.ut_pid << ", \"" << ut.ut_line << "\", \"" << ut.ut_id << "\", \"" << ut.ut_user << "\", \"" << ut.ut_host << "\", " << ut.ut_tv.tv_sec << ", " << ut.ut_tv.tv_usec << ") failed";
        throw unix_exception(os.str());
    }
    endutxent();
}
