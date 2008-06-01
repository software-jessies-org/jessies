#include <string>
#include <unistd.h>
#include <windows.h>

#include "reportFatalErrorViaGui.h"
#include "toString.h"
#include "unix_exception.h"
#include "WindowsDllErrorModeChange.h"

void launchCygwin(char** argValues) {
    // TODO: SetDllDirectory.
    if (*argValues == 0) {
        throw std::runtime_error("missing cygwin program name");
    }
    const char* program = *argValues;
    WindowsDllErrorModeChange windowsDllErrorModeChange;
    execvp(program, argValues);
    throw unix_exception(std::string("execvp(\"") + program + "\", ...)");
}

int main(int, char** argValues) {
    const char* ARGV0 = *argValues;
    ++ argValues;
    try {
        // It would seem that we can, at the moment, but I expect it'll rust.
        //throw std::runtime_error("can we report an early failure?");
        launchCygwin(argValues);
    } catch (const std::exception& ex) {
        std::ostringstream os;
        os << "Error: ";
        os << ex.what();
        os << std::endl;
        os << std::endl;
        os << "Usage: ";
        os << ARGV0;
        os << " <cygwin program name> <arguments>...";
        os << std::endl;
        reportFatalErrorViaGui("Cygwin Launcher", os.str());
        return 1;
    }
}
