#include <stdlib.h>
#include <string>
#include <unistd.h>
#include <vector>
#include <windows.h>

#include "checkReadableFile.h"
#include "join.h"
#include "reportFatalErrorViaGui.h"
#include "toString.h"
#include "unix_exception.h"
#include "WindowsDllErrorModeChange.h"

std::string quote(const std::string& argument) {
    return std::string("\"") + argument + ("\"");
}

void launchCygwin(char** argValues) {
    std::string ARGV0 = *argValues;
    ++ argValues;
    size_t lastBackslash = ARGV0.rfind('\\');
    if (lastBackslash == std::string::npos) {
        throw std::runtime_error(std::string("couldn't determine directory from which ") + ARGV0 + " was run");
    }
    std::string directory = ARGV0.substr(0, lastBackslash);
    if (*argValues == 0) {
        throw std::runtime_error("missing Cygwin bin directory");
    }
    std::string cygwinBin = *argValues;
    ++ argValues;
    const char* oldPath = getenv("PATH");
    if (oldPath == 0) {
        throw std::runtime_error("getenv(\"PATH\") implausibly returned null");
    }
    // Windows doesn't support setenv but does support putenv.
    // We need cygwin1.dll to be on the PATH.
    std::string putenvArgument = std::string("PATH=") + oldPath + ";" + cygwinBin;
    if (putenv(putenvArgument.c_str()) == -1) {
        throw unix_exception(std::string("putenv(\"") + putenvArgument + "\")");
    }
    // Windows requires that we quote the arguments ourselves.
    typedef std::vector<std::string> Arguments;
    Arguments arguments;
    std::string program = directory + "\\ruby-launcher";
    // We mustn't quote the program argument but we must quote argv[0].
    arguments.push_back(quote(program));
    // Make sure we invoke the Cygwin rubyw, not any native version that might be ahead of it on the PATH.
    // cygwinBin does not include a trailing backslash.
    checkReadableFile("Cygwin DLL", cygwinBin + "\\cygwin1.dll");
    arguments.push_back(quote(cygwinBin + "\\rubyw"));
    while (*argValues != 0) {
        const char* argument = *argValues;
        arguments.push_back(quote(argument));
        ++ argValues;
    }
    typedef std::vector<char*> ArgValues;
    ArgValues childArgValues;
    for (Arguments::iterator it = arguments.begin(), en = arguments.end(); it != en; ++ it) {
        std::string& argument = *it;
        childArgValues.push_back(&argument[0]);
    }
    childArgValues.push_back(0);
    WindowsDllErrorModeChange windowsDllErrorModeChange;
    execv(program.c_str(), &childArgValues[0]);
    throw unix_exception(std::string("execv(\"") + program + "\", [" + join(", ", arguments) + "])");
}

int main(int, char** argValues) {
    const char* ARGV0 = *argValues;
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
        os << " <Cygwin program name> <arguments>...";
        os << std::endl;
        reportFatalErrorViaGui("Cygwin Launcher", os.str());
        return 1;
    }
}
