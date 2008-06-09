#ifndef REPORT_ARG_VALUES_VIA_GUI_H_included
#define REPORT_ARG_VALUES_VIA_GUI_H_included

#include "reportFatalErrorViaGui.h"

#include <iomanip>
#include <sstream>

void reportArgValuesViaGui(char const* const* argValues) {
    const char* ARGV0 = *argValues;
    std::ostringstream os;
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
    reportFatalErrorViaGui(ARGV0, os.str());
}

#endif
