#ifndef PARSE_INTEGER_H_included
#define PARSE_INTEGER_H_included

#define __STDC_LIMIT_MACROS

#include <errno.h>
#include <inttypes.h>

template <class Integer>
inline bool parseInteger(const char* input, Integer& output) {
    char* end;
    uintmax_t rc = strtoumax(input, &end, 10);
    if (rc == UINTMAX_MAX && errno == ERANGE) {
        return false;
    }
    if (*end != 0) {
        return false;
    }
    output = rc;
    if (uintmax_t(output) != rc) {
        return false;
    }
    return true;
}

#endif
