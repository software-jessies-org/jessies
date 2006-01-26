#include "unix_exception.h"

#include "toString.h"

unix_exception::unix_exception(const std::string& message)
: std::runtime_error(message + (errno ? ": (" + toString(errno) + ")" : "")) {
}
