#include "checkReadableFile.h"
#include "HKEY.h"
#include "join.h"
#include "reportArgValues.h"
#include "reportFatalErrorViaGui.h"
#include "toString.h"
#include "unix_exception.h"
#include "WindowsDllErrorModeChange.h"
#include "WindowsError.h"

#include <deque>
#include <stdlib.h>
#include <string>
#include <unistd.h>
#include <vector>
#include <windows.h>

std::string quote(const std::string& argument) {
    return std::string("\"") + argument + ("\"");
}

struct RegistryEntry {
private:
    const char* keyName;
    const char* valueName;
    
public:
    RegistryEntry(const char* keyName0, const char* valueName0)
    : keyName(keyName0), valueName(valueName0) {
    }
    
    std::string read(HKEY hive) const {
        HKEY keyHandle;
        // RegGetValue would let us replace most of this function with a single line but "Requires Windows Vista or Windows XP Professional x64 Edition".
        LONG errorCode = RegOpenKey(hive, keyName, &keyHandle);
        if (errorCode != ERROR_SUCCESS) {
            throw WindowsError("RegOpenKey(" + toString(hive) + ", " + keyName + ", &keyHandle)", errorCode);
        }
        // MAX_PATH seems likely to be enough.
        std::vector<BYTE> buffer(MAX_PATH);
        DWORD type;
        DWORD size = buffer.size();
        // FIXME: We could make some effort towards Unicode support.
        errorCode = RegQueryValueEx(keyHandle, valueName, 0, &type, &buffer[0], &size);
        if (errorCode != ERROR_SUCCESS) {
            throw WindowsError("RegQueryValueEx(" + toString(keyHandle) + " (\"" + keyName + "\"), \"" + valueName + "\", 0, &type, &buffer[0], &size)", errorCode);
        }
        if (type != REG_SZ) {
            throw std::runtime_error("The type of registry entry hive " + toString(hive) + ", key " + keyName + ", value " + valueName + " was not REG_SZ (" + toString(REG_SZ) + ") as expected but " + toString(type));
        }
        // MSDN's article on RegQueryValueEx explains that the value may or may not include the null terminator.
        if (size != 0 && buffer[size - 1] == 0) {
            -- size;
        }
        return std::string(reinterpret_cast<const char*>(&buffer[0]), size);
    }
};

std::string findCygwinBin(const RegistryEntry& registryEntry) {
    std::ostringstream os;
    typedef std::deque<HKEY> Hives;
    Hives hives;
    hives.push_back(HKEY_CURRENT_USER);
    hives.push_back(HKEY_LOCAL_MACHINE);
    for (Hives::const_iterator it = hives.begin(), en = hives.end(); it != en; ++ it) {
        HKEY hive = *it;
        try {
            std::string cygwinRoot = registryEntry.read(hive);
            std::string cygwinBin = cygwinRoot + "\\bin";
            return cygwinBin;
        } catch (const std::exception& ex) {
            os << ex.what();
            os << std::endl;
        }
    }
    throw std::runtime_error(os.str());
}

std::string findCygwinBin() {
    std::ostringstream os;
    os << "We failed to find Cygwin, the errors were:\n";
    typedef std::deque<RegistryEntry> RegistryEntries;
    RegistryEntries registryEntries;
    // Prefer Cygwin 1.7 over Cygwin 1.5.
    registryEntries.push_back(RegistryEntry("Software\\Cygwin\\Setup", "rootdir"));
    registryEntries.push_back(RegistryEntry("Software\\Cygnus Solutions\\Cygwin\\mounts v2\\/", "native"));
    for (RegistryEntries::const_iterator it = registryEntries.begin(), en = registryEntries.end(); it != en; ++ it) {
        RegistryEntry registryEntry = *it;
        try {
            return findCygwinBin(registryEntry);
        } catch (const std::exception& ex) {
            os << ex.what();
            os << std::endl;
        }
    }
    throw std::runtime_error(os.str());
}

void launchCygwin(char** argValues) {
    std::string ARGV0 = *argValues;
    ++ argValues;
    size_t lastBackslash = ARGV0.rfind('\\');
    std::string directoryPrefix;
    if (lastBackslash != std::string::npos) {
        directoryPrefix = ARGV0.substr(0, lastBackslash + 1);
    }
    
    std::string cygwinBin = findCygwinBin();
    
    const char* oldPath = getenv("PATH");
    if (oldPath == 0) {
        throw std::runtime_error("getenv(\"PATH\") implausibly returned null");
    }
    // Windows doesn't support setenv but does support putenv.
    // We need cygwin1.dll to be on the PATH.
    std::string putenvArgument = std::string("PATH=") + oldPath + ";" + cygwinBin;
    if (putenv(putenvArgument.c_str()) == -1) {
        throw unix_exception(std::string("putenv(\"") + putenvArgument + "\") failed");
    }
    checkReadableFile("Cygwin DLL", cygwinBin + "\\cygwin1.dll");
    
    // Windows requires that we quote the arguments ourselves.
    typedef std::vector<std::string> Arguments;
    Arguments arguments;
    std::string program = directoryPrefix + "ruby-launcher.exe";
    // We mustn't quote the program argument but we must quote argv[0].
    arguments.push_back(quote(program));
    // Make sure we invoke the Cygwin rubyw, not any native version that might be ahead of it on the PATH.
    std::string rubyInterpreter = cygwinBin + "\\rubyw.exe";
    checkReadableFile("Cygwin Rubyw (Ruby with no console window)", rubyInterpreter);
    arguments.push_back(quote(rubyInterpreter));
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
    throw unix_exception(std::string("execv(\"") + program + "\", [" + join(", ", arguments) + "]) failed");
}

int main(int, char** argValues) {
    const char* ARGV0 = *argValues;
    try {
        // It would seem that we can, at the moment, but I expect it'll rust.
        //throw std::runtime_error("can we report an early failure?");
        launchCygwin(argValues);
    } catch (const std::exception& ex) {
        std::ostringstream os;
        os << "This program requires Cygwin and Cygwin Ruby.\n";
        os << "https://code.google.com/p/jessies/wiki/CygwinSetup might help.\n";
        os << "\n";
        os << "The rest of this message is only relevant if you have Cygwin and Cygwin Ruby installed.\n";
        os << "\n";
        os << "Error: ";
        os << ex.what();
        os << std::endl;
        os << "Usage: ";
        os << ARGV0;
        os << " <Cygwin program name> <arguments>...";
        os << std::endl;
        os << std::endl;
        reportArgValues(os, argValues);
        reportFatalErrorViaGui("Cygwin Launcher", os.str());
        return 1;
    }
}
