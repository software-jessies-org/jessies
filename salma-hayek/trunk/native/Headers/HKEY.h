#ifndef HKEY_H_included
#define HKEY_H_included

#include <iostream>

#if defined(__CYGWIN__) || defined(__MINGW32__)
#include <windows.h>
#else
struct HKey;
typedef HKey* HKEY;
static HKEY HKEY_CURRENT_USER;
static HKEY HKEY_LOCAL_MACHINE;
#endif

inline std::ostream& operator<<(std::ostream& os, HKEY hive) {
    if (hive == HKEY_CURRENT_USER) {
        return os << "HKEY_CURRENT_USER";
    }
    if (hive == HKEY_LOCAL_MACHINE) {
        return os << "HKEY_LOCAL_MACHINE";
    }
    void* pointer = hive;
    return os << "(unrecognized HKEY)" << pointer;
}

#endif
