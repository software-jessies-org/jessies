#ifndef REPORT_ARG_VALUES_H_included
#define REPORT_ARG_VALUES_H_included

#include <iomanip>
#include <sstream>

void reportArgValues(std::ostream& os, char const* const* argValues) {
    os << "arguments [";
    os << std::endl;
    while (*argValues != 0) {
        os << "\"";
        os << *argValues;
        ++ argValues;
        os << "\"";
        os << std::endl;
    }
    os << "] arguments";
    os << std::endl;
}

#endif
