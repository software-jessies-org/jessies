#ifndef ERRNO_TO_STRING_H_included
#define ERRNO_TO_STRING_H_included

#include <string>

/** Converts the current value of 'errno' to a string. */
std::string errnoToString();

#endif
