#include "setup_win_environ.h"

#ifdef __CYGWIN__

#include <stdlib.h>
#include <string.h>
#include <sys/cygwin.h>
#include <unistd.h>
#include <windows.h>

// This is part of Michael Schaap's GPL code from this revision of cygstart.c:
// http://sources.redhat.com/cgi-bin/cvsweb.cgi/cygutils/src/cygstart/cygstart.c?rev=1.5&content-type=text/x-cvsweb-markup&cvsroot=cygwin-apps
// All I've done is commented-out the /*static*/.
// FIXME: cygwin_internal(CW_SYNC_WINENV) would be an alternative implementation which would avoid this duplication.
// Unfortunately, that will only be available from (the as-yet unreleased) Cygwin 1.5.20.

/* Copy cygwin environment variables to the Windows environment if they're not
 * already there. */
/*static*/ void setup_win_environ(void)
{
    char **envp = environ;
    char *var, *val;
    char curval[2];
    char *winpathlist;
    char winpath[MAX_PATH+1];

    while (envp && *envp) {
        var = strdup(*envp++);
        val = strchr(var, '=');
        *val++ = '\0';
        
        if (GetEnvironmentVariable(var, curval, 2) == 0
                    && GetLastError() == ERROR_ENVVAR_NOT_FOUND) {
            /* Convert POSIX to Win32 where necessary */
            if (!strcmp(var, "PATH") ||
                        !strcmp(var, "LD_LIBRARY_PATH")) {
                winpathlist = (char *)
                      malloc(cygwin_posix_to_win32_path_list_buf_size(val)+1);
                if (winpathlist) {
                    cygwin_posix_to_win32_path_list(val, winpathlist);
                    SetEnvironmentVariable(var, winpathlist);
                    free(winpathlist);
                }
            } else if (!strcmp(var, "HOME") ||
                        !strcmp(var, "TMPDIR") ||
                        !strcmp(var, "TMP") ||
                        !strcmp(var, "TEMP")) {
                cygwin_conv_to_win32_path(val, winpath);
                SetEnvironmentVariable(var, winpath);
            } else {
                SetEnvironmentVariable(var, val);
            }
        }

        free(var);
    }
}

#else

void setup_win_environ() {
}

#endif
