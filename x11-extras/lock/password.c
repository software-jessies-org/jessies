#include    <stdlib.h>
#include    <string.h>

#include    <unistd.h>
#include    <pwd.h>
#include    <sys/types.h>

int
check_password(char * p)
{
    char * logname;
    struct passwd * spw;
    char salt[3];
    char * encrypted;

    if (p == 0)
        return 0;

    logname = getenv("LOGNAME");
    if (logname == 0)
        logname = getenv("USER");
    if (logname == 0)
        logname = getlogin();
    spw = getpwnam(logname);

    if (spw == 0 || spw->pw_passwd == 0)
        return 0;

    strncpy(salt, spw->pw_passwd, 2);
    salt[2] = 0;
    
    encrypted = crypt(p, salt);
    if (encrypted == 0)
        return 0;

    return !strcmp(spw->pw_passwd, encrypted);
}
