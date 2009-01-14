#ifndef HKEY_H_included
#define HKEY_H_included

#include <iostream>

#ifndef __CYGWIN__
struct HKey;
typedef HKey* HKEY;
static HKEY HKEY_CURRENT_USER;
static HKEY HKEY_LOCAL_MACHINE;
#endif

std::ostream& operator<<(std::ostream& os, HKEY hive) {
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
