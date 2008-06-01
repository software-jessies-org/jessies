#ifndef STRERROR_R_H_included
#define STRERROR_R_H_included

#include <string.h>

// The Microsoft runtime doesn't provide strerror_r.
// We only have one mingw program and it's single-threaded.
#ifdef __MINGW32__
inline char* strerror_r(int errnum, char*, size_t) {
    return strerror(errnum);
}
#endif

#endif
