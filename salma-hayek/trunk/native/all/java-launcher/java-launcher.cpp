#ifdef __APPLE__
#include <pthread.h>
#include <sys/stat.h>
#endif

#ifdef __CYGWIN__
#include <windows.h>
#endif

#include "chomp.h"
#include "DirectoryIterator.h"
#include "HKEY.h"
#include "JniError.h"
#include "JniString.h"
#include "join.h"
#include "PortableJni.h"
#include "reportFatalErrorViaGui.h"
#include "synchronizeWindowsEnvironment.h"
#include "WindowsDirectoryChange.h"
#include "WindowsDllErrorModeChange.h"
#include "WindowsError.h"

#include <algorithm>
#include <deque>
#include <dlfcn.h>
#include <fstream>
#include <iostream>
#include <map>
#include <sstream>
#include <stdexcept>
#include <stdint.h>
#include <string>
#include <stdlib.h>
#include <stdio.h>
#include <unistd.h>
#include <vector>

struct UsageError : std::runtime_error {
    explicit UsageError(const std::string& description)
    : std::runtime_error(description) {
    }
};

typedef std::deque<std::string> NativeArguments;

extern "C" {
    typedef jint JNICALL (*CreateJavaVM)(JavaVM**, void**, void*);
}

struct ErrorReporter {
public:
    std::string ARGV0;
    NativeArguments launcherArguments;
    std::string supportAddress;
    std::ostringstream progressOStream;
    
private:
    std::string getUsage() const;
    void generateReport(const std::exception& ex, const std::string& usage) const;
        
public:
    ErrorReporter()
    : ARGV0("<unknown>") {
    }
    
    void reportUsageError(const UsageError& ex) const {
        generateReport(ex, getUsage());
    }
    
    void reportFatalException(const std::exception& ex) const {
        generateReport(ex, "");
    }
    
    void abortJvm() const {
        std::runtime_error ex("JVM aborted");
        reportFatalException(ex);
        // The JVM is about to call exit(1) in MSVCRT.
        // We need to exit via Cygwin if we're to return a failure.
        // abort() leaves a java-launcher.exe.stackdump file of dubious value.
        exit(1);
    }
};

static ErrorReporter errorReporter;

static void abortJvm() {
    errorReporter.abortJvm();
}

#ifdef __APPLE__

// Apple's Java launcher sample code:
// http://developer.apple.com/samplecode/simpleJavaLauncher/listing3.html
// For more on Apple-specific Java launching considerations, see:
// http://developer.apple.com/technotes/tn2005/tn2147.html

// Callback for dummy source used to make sure the CFRunLoop doesn't exit right away.
// This callback is called when the source has fired.
static void mac_dummyRunLoopCallBack(void* /*info*/) { }

static void mac_startCfRunLoop() {
    // Create a sourceContext to be used by our source that makes sure the CFRunLoop doesn't exit right away.
    CFRunLoopSourceContext sourceContext;
    sourceContext.version = 0;
    sourceContext.info = NULL;
    sourceContext.retain = NULL;
    sourceContext.release = NULL;
    sourceContext.copyDescription = NULL;
    sourceContext.equal = NULL;
    sourceContext.hash = NULL;
    sourceContext.schedule = NULL;
    sourceContext.cancel = NULL;
    sourceContext.perform = &mac_dummyRunLoopCallBack;
    
    // Create the Source from the sourceContext.
    CFRunLoopSourceRef sourceRef = CFRunLoopSourceCreate(NULL, 0, &sourceContext);
    
    // Use the constant kCFRunLoopCommonModes to add the source to the set of objects monitored by all the common modes.
    CFRunLoopAddSource(CFRunLoopGetCurrent(), sourceRef, kCFRunLoopCommonModes);
    
    // Park this thread in the run loop.
    CFRunLoopRun();
}

static int runJvm(const NativeArguments& launcherArguments);

static void* mac_startJvm(void* context) {
    NativeArguments* launcherArguments = reinterpret_cast<NativeArguments*>(context);
    int status = runJvm(*launcherArguments);
    exit(status);
    return NULL;
}

static int mac_runJvm(NativeArguments& launcherArguments) {
    // Find the primordial pthread's stack size.
    rlimit limit;
    int rc = getrlimit(RLIMIT_STACK, &limit);
    if (rc != 0) {
        throw unix_exception("getrlimit failed");
    }
    size_t stack_size = size_t(limit.rlim_cur);
    
    // Create a new pthread copying the stack size of the primordial pthread.
    pthread_attr_t threadAttributes;
    pthread_attr_init(&threadAttributes);
    pthread_attr_setscope(&threadAttributes, PTHREAD_SCOPE_SYSTEM);
    pthread_attr_setdetachstate(&threadAttributes, PTHREAD_CREATE_DETACHED);
    if (stack_size > 0) {
        pthread_attr_setstacksize(&threadAttributes, stack_size);
    }
    
    // Start the thread that we will start the JVM on.
    pthread_t vmThread;
    pthread_create(&vmThread, &threadAttributes, mac_startJvm, &launcherArguments);
    pthread_attr_destroy(&threadAttributes);
    
    // Park the primordial pthread.
    mac_startCfRunLoop();
    return 0;
}

#endif

typedef void* SharedLibraryHandle;

SharedLibraryHandle openSharedLibrary(const std::string& sharedLibraryFilename) {
    // Try to persuade Windows to pop-up a box complaining about unresolved symbols because we don't get anything more informative from dlerror than ENOENT.
    // This could cause a problem if we try to load an amd64 DLL before going on to try to load an i386 DLL.
    // At least it would be an overt problem rather than the silent failure we got when MSVCR71.DLL wasn't in the current directory and wasn't on the PATH.
    WindowsDllErrorModeChange windowsDllErrorModeChange;
    std::ostringstream os;
#if defined(__CYGWIN__)
    // As of winsup/cygwin/dlfcn.cc revision 1.41, dlopen uses LoadLibraryW.
    // The code to generate wide character filenames prepends \\?\.
    // This wasn't done before, when dlopen was using LoadLibrary.
    // (This lets Cygwin load libraries from long and MBCS paths.)
    // The JVM deduces the location of rt.jar from the filename via which it was loaded.
    // If loaded with a name that starts \\?\, the JVM fails to open rt.jar.
    // It's the prefix that causes this, not the LibraryW.
    // It's not clear whether that's due to a path parsing infelicity in the JVM
    // or due to a deficiency in some Windows API call.
    // According to "File Names, Paths, And Namespaces (Windows)" on MSDN
    // 'Many but not all file APIs support "\\?\"'
    // There's no list of broken ones.
    // Fortunately, we don't require any of dlopen's functionality.
    // We don't need Cygwin paths translating or symlinks following,
    // nor do we need an LD_LIBRARY_PATH search.
    // dlopen doesn't currently do anything clever with the returned value.
    void* sharedLibraryHandle = LoadLibrary(sharedLibraryFilename.c_str());
    if (sharedLibraryHandle == 0) {
        DWORD lastError = GetLastError();
#else
    void* sharedLibraryHandle = dlopen(sharedLibraryFilename.c_str(), RTLD_LAZY);
    if (sharedLibraryHandle == 0) {
        os << "dlopen(\"" << sharedLibraryFilename << "\") failed with " << dlerror() << ".";
#endif
        os << std::endl;
        os << "If you can't otherwise explain why this call failed, consider whether all of the shared libraries";
        os << " used by this shared library can be found.";
        os << std::endl;
#if defined(__CYGWIN__)
        os << "This command's output may help:";
        os << std::endl;
        os << "objdump -p \"" << sharedLibraryFilename << "\" | grep DLL";
        os << std::endl;
        os << "LoadLibrary(\"" << sharedLibraryFilename << "\")";
        throw WindowsError(os.str(), lastError);
#else
        throw std::runtime_error(os.str());
#endif
    }
    return sharedLibraryHandle;
}

static std::string readFile(const std::string& path) {
    std::ifstream is(path.c_str());
    if (is.good() == false) {
        throw std::runtime_error("Couldn't open \"" + path + "\".");
    }
    std::ostringstream contents;
    contents << is.rdbuf();
    return contents.str();
}

static std::string readRegistryFile(const std::string& path) {
    std::string contents = readFile(path);
    // MSDN's article on RegQueryValueEx explains that the value may or may not include the null terminator.
    if (contents.empty() == false && contents[contents.size() - 1] == '\0') {
        return contents.substr(0, contents.size() - 1);
    }
    return contents;
}

struct JvmRegistryKey {
public:
    typedef JvmRegistryKey self;
    
private:
    std::string version;
    std::string registryPath;
    std::string pathFromJavaHomeToJre;
    
public:
    JvmRegistryKey(const std::string& registryRoot, const std::string& version0, const std::string& pathFromJavaHomeToJre0)
    : version(version0), registryPath(registryRoot + "/" + version + "/JavaHome"), pathFromJavaHomeToJre(pathFromJavaHomeToJre0) {
    }
    
    std::string readJreBin() const {
        // The path we get from the registry is a Windows path.
        // We depend on this in WindowsDirectoryChange.
        std::string javaHome = readRegistryFile(registryPath);
        // It feels safer if we append the rest of the path in Windows format,
        // though we got away for years with a Windows path containing forward slashes.
        // (We have some reason to believe that the UTF-16 Windows file API doesn't support interchangeable slashes
        // and we'd like Cygwin to be UTF-16 below so it could present UTF-8 above.)
        std::string jreBin = javaHome + pathFromJavaHomeToJre + "\\bin";
        return jreBin;
    }
    
    bool operator<(const self& rhs) const {
        return version < rhs.version;
    }
    
    void dumpTo(std::ostream& os) const {
        os << registryPath;
    }
    
    friend std::ostream& operator<<(std::ostream& os, const self& rhs) {
        rhs.dumpTo(os);
        return os;
    }
};

typedef std::vector<JvmRegistryKey> JvmRegistryKeys;

bool isUnreasonableVersion(std::ostream& os, const std::string& version) {
    if (version.empty() || isdigit(version[0]) == false) {
        // Avoid "CurrentVersion", "BrowserJavaVersion", or anything else Sun might think of.
        // The registry keys and Sun's installer's behavior regarding them is documented at:
        // http://java.sun.com/j2se/1.5.0/runtime_windows.html
        // The first claim on that page (that "CurrentVersion" is the highest-numbered version ever installed) appears to be the true claim.
        os << "\"";
        os << version;
        os << "\" is not a number";
        os << std::endl;
        return true;
    }
    if (version < "1.6") {
        os << version;
        os << " is too old";
        os << std::endl;
        return true;
    }
    static const char* endLimit = getenv("ORG_JESSIES_LAUNCHER_JVM_LIMIT");
    if (endLimit != 0 && version >= endLimit) {
        os << version;
        os << " is too new";
        os << std::endl;
        return true;
    }
    return false;
}

void findVersionsInRegistry(std::ostream& os, JvmRegistryKeys& jvmRegistryKeys, const std::string& registryPath, const char* pathFromJavaHomeToJre) {
    os << "Looking for registered JVMs under \"";
    os << registryPath;
    os << "\" [";
    os << std::endl;
    try {
        for (DirectoryIterator it(registryPath); it.isValid(); ++ it) {
            std::string version = it->getName();
            if (isUnreasonableVersion(os, version)) {
                continue;
            }
            JvmRegistryKey jvmRegistryKey(registryPath, version, pathFromJavaHomeToJre);
            jvmRegistryKeys.push_back(jvmRegistryKey);
        }
    } catch (const std::exception& ex) {
        os << ex.what();
        os << std::endl;
    }
    os << "]";
    os << std::endl;
}

SharedLibraryHandle tryVersions(const char* jvmDirectory, HKEY hive, const char* javaVendor, const char* jdkName, const char* jreName) {
    const std::string registryPrefix = "/proc/registry/" + toString(hive) + "/SOFTWARE/" + javaVendor + "/";
    const std::string jreRegistryPath = registryPrefix + jreName;
    const std::string jdkRegistryPath = registryPrefix + jdkName;
    std::ostream& os = errorReporter.progressOStream;
    JvmRegistryKeys jvmRegistryKeys;
    findVersionsInRegistry(os, jvmRegistryKeys, jreRegistryPath, "");
    // My Sun JDK key says:
    // "JavaHome"="C:\\Program Files\\Java\\jdk1.5.0_06"
    // Jesse Kriss's IBM JDK has an appended "jre" component:
    // "JavaHome"="C:\\Program Files\\IBM\\Java50\\jre"
    findVersionsInRegistry(os, jvmRegistryKeys, jdkRegistryPath, "\\jre");
    std::sort(jvmRegistryKeys.begin(), jvmRegistryKeys.end());
    while (jvmRegistryKeys.empty() == false) {
        JvmRegistryKey jvmRegistryKey = jvmRegistryKeys.back();
        jvmRegistryKeys.pop_back();
        os << "Trying \"";
        os << jvmRegistryKey;
        os << "\" [";
        os << std::endl;
        try {
            std::string jreBin = jvmRegistryKey.readJreBin();
            // JRE 6 distributes msvcr71.dll in the same directory as java.exe.
            // At least one installation of Windows XP doesn't have this DLL in its %SystemDir%.
            // That looks like the way of the future.
            // Windows looks for DLLs in the current directory (among other places).
            // Modern Cygwin's chdir doesn't change the Windows current directory.
            // We need to put the current directory back afterwards otherwise we break javahpp
            // and any other Java applications which care about the current directory.
            WindowsDirectoryChange windowsDirectoryChange(jreBin);
            std::string jvmLocation = jreBin + "\\" + jvmDirectory + "\\jvm.dll";
            return openSharedLibrary(jvmLocation);
        } catch (const std::exception& ex) {
            os << ex.what();
            os << std::endl;
        }
        os << "]";
        os << std::endl;
    }
    std::ostringstream errorOStream;
    errorOStream << "tryVersions(\"" << jvmDirectory << "\", " << hive << ", " << javaVendor << ", " << jdkName << ", " << jreName << ") failed";
    throw std::runtime_error(errorOStream.str());
}

SharedLibraryHandle tryHives(const char* jvmDirectory, const char* javaVendor, const char* jdkName, const char* jreName) {
    typedef std::deque<HKEY> Hives;
    Hives hives;
    hives.push_back(HKEY_CURRENT_USER);
    hives.push_back(HKEY_LOCAL_MACHINE);
    for (Hives::const_iterator it = hives.begin(), en = hives.end(); it != en; ++ it) {
        HKEY hive = *it;
        try {
            return tryVersions(jvmDirectory, hive, javaVendor, jdkName, jreName);
        } catch (const std::exception& ex) {
            std::ostream& os = errorReporter.progressOStream;
            os << ex.what();
            os << std::endl;
        }
    }
    std::ostringstream os;
    os << "tryHives(\"" << jvmDirectory << "\", " << javaVendor << ", " << jdkName << ", " << jreName << ") failed";
    throw std::runtime_error(os.str());
}

struct JavaVendorRegistryLocation {
    const char* javaVendor;
    const char* jdkName;
    const char* jreName;
    
    JavaVendorRegistryLocation(const char* javaVendor0, const char* jdkName0, const char* jreName0)
    : javaVendor(javaVendor0), jdkName(jdkName0), jreName(jreName0) {
    }
};

SharedLibraryHandle tryVendors(const char* jvmDirectory) {
    typedef std::deque<JavaVendorRegistryLocation> JavaVendorRegistryLocations;
    JavaVendorRegistryLocations javaVendorRegistryLocations;
    javaVendorRegistryLocations.push_back(JavaVendorRegistryLocation("JavaSoft", "Java Development Kit", "Java Runtime Environment"));
    javaVendorRegistryLocations.push_back(JavaVendorRegistryLocation("IBM", "Java Development Kit", "Java2 Runtime Environment"));
    for (JavaVendorRegistryLocations::const_iterator it = javaVendorRegistryLocations.begin(); it != javaVendorRegistryLocations.end(); ++ it) {
        try {
            return tryHives(jvmDirectory, it->javaVendor, it->jdkName, it->jreName);
        } catch (const std::exception& ex) {
            std::ostream& os = errorReporter.progressOStream;
            os << ex.what();
            os << std::endl;
        }
    }
    std::ostringstream os;
    os << "tryVendors(\"" << jvmDirectory << "\") failed";
    throw std::runtime_error(os.str());
}

SharedLibraryHandle tryDirectories(bool isClient, bool isServer) {
    typedef std::deque<const char*> JvmDirectories;
    JvmDirectories jvmDirectories;
    if (isClient == false && isServer == false) {
        isClient = true;
        isServer = true;
    }
    if (isClient) {
        jvmDirectories.push_back("client");
    }
    if (isServer) {
        jvmDirectories.push_back("server");
    }
    for (JvmDirectories::const_iterator it = jvmDirectories.begin(), en = jvmDirectories.end(); it != en; ++ it) {
        const char* jvmDirectory = *it;
        try {
            return tryVendors(jvmDirectory);
        } catch (const std::exception& ex) {
            std::ostream& os = errorReporter.progressOStream;
            os << ex.what();
            os << std::endl;
        }
    }
    std::ostringstream os;
    os << "tryDirectories(" << isClient << ", " << isServer << ") failed";
    throw std::runtime_error(os.str());
}

// Once we've successfully opened a shared library, I think we're committed to trying to use it
// or else who knows what its DLL entry point has done.
// Until we've successfully opened it, though, we can keep trying alternatives.
SharedLibraryHandle openWindowsJvmLibrary(bool isClient, bool isServer) {
    std::ostream& os = errorReporter.progressOStream;
    os << "Trying to find ";
    os << sizeof(void*) * 8;
    os << " bit jvm.dll - we need a 1.6 or newer JRE or JDK.";
    os << std::endl;
    os << "Error messages were:";
    os << std::endl;
    return tryDirectories(isClient, isServer);
}

SharedLibraryHandle openJvmLibrary(bool isClient, bool isServer) {
#if defined(__APPLE__)
    (void) isClient;
    (void) isServer;
    // FIXME: we might want to try examining the "Versions" directory instead, rather than assume this is suitable.
    return openSharedLibrary("/System/Library/Frameworks/JavaVM.framework/Versions/A/JavaVM");
#elif defined(__CYGWIN__)
    return openWindowsJvmLibrary(isClient, isServer);
#else
    (void) isClient;
    (void) isServer;
    // This only works on Linux if LD_LIBRARY_PATH is already set up to include something like:
    // "$JAVA_HOME/jre/lib/$ARCH/" + jvmDirectory
    // "$JAVA_HOME/jre/lib/$ARCH"
    // "$JAVA_HOME/jre/../lib/$ARCH"
    // Where $ARCH is "i386" rather than `arch`.
    return openSharedLibrary("libjvm.so");
#endif
}

bool startsWith(const std::string& st, const std::string& prefix) {
    return st.substr(0, prefix.size()) == prefix;
}

struct Properties : public std::map<std::string, std::string> {
    void parse(const NativeArguments& arguments) {
        for (NativeArguments::const_iterator it = arguments.begin(), en = arguments.end(); it != en; ++ it) {
            std::string option = *it;
            if (startsWith(option, "-D") == false) {
                continue;
            }
            size_t offset = option.find('=');
            if (offset == std::string::npos) {
                continue;
            }
            std::string name = option.substr(2, offset - 2);
            std::string value = option.substr(offset + 1);
            (*this)[name] = value;
        }
    }
};

class LauncherArgumentParser {
private:
    Properties properties;
    bool isClient;
    bool isServer;
    NativeArguments jvmArguments;
    std::string className;
    NativeArguments mainArguments;
    
public:
    explicit LauncherArgumentParser(const NativeArguments& launcherArguments) {
        properties.parse(launcherArguments);
        // Try to set the mailing list address before reporting errors.
        errorReporter.supportAddress = properties["e.gui.HelpMenu.supportAddress"];
        isClient = false;
        isServer = false;
        NativeArguments::const_iterator it = launcherArguments.begin();
        NativeArguments::const_iterator end = launcherArguments.end();
        while (it != end && startsWith(*it, "-")) {
            std::string option = *it++;
            if (option == "-cp" || option == "-classpath") {
                if (it == end) {
                    throw UsageError(option + " requires an argument.");
                }
                // Translate to a form the JVM understands.
                std::string classPath = *it++;
                jvmArguments.push_back("-Djava.class.path=" + classPath);
            } else if (option == "-client") {
                isClient = true;
            } else if (option == "-server") {
                isServer = true;
            } else if (startsWith(option, "-Xdock:name=")) {
                setAppVariableFromOption("NAME", option);
            } else if (startsWith(option, "-Xdock:icon=")) {
                setAppVariableFromOption("ICON", option);
            } else {
                jvmArguments.push_back(option);
            }
        }
        if (it == end) {
            throw UsageError("No class specified.");
        }
        className = *it++;
        while (it != end) {
            mainArguments.push_back(*it++);
        }
        try {
            std::string launcherOsVersion = chomp(readFile("/proc/version"));
            jvmArguments.push_back("-De.util.Log.launcherOsVersion=" + launcherOsVersion);
        } catch (const std::exception&) {
            // We lived without this for years.
        }
    }
    
    void setAppVariableFromOption(const char* key, const std::string& option) {
        size_t offset = option.find('=');
        if (offset == std::string::npos) {
            return;
        }
        std::string value = option.substr(offset + 1);
        std::ostringstream oss;
        oss << "APP_" << key << "_" << getpid();
        setenv(oss.str().c_str(), value.c_str(), 1);
    }
        
    SharedLibraryHandle openJvmLibrary() const {
        const char* jvmSharedLibrary = getenv("ORG_JESSIES_LAUNCHER_JVM_SHARED_LIBRARY");
        if (jvmSharedLibrary != 0) {
            return openSharedLibrary(jvmSharedLibrary);
        }
        return ::openJvmLibrary(isClient, isServer);
    }
    
    NativeArguments getJvmArguments() const {
        return jvmArguments;
    }
    
    std::string getMainClassName() const {
        return className;
    }
    
    NativeArguments getMainArguments() const {
        return mainArguments;
    }
};

template <class ExtraInfo>
struct JvmOption : JavaVMOption {
    JvmOption(const char* optionString0, ExtraInfo extraInfo0) {
        // I'm sure the JVM doesn't actually write to its options.
        optionString = const_cast<char*>(optionString0);
        extraInfo = const_cast<void*>(reinterpret_cast<const void*>(reinterpret_cast<uintptr_t>(extraInfo0)));
    }
};

template <class ExtraInfo>
JvmOption<ExtraInfo> makeJvmOption(const char* optionString, ExtraInfo extraInfo) {
    return JvmOption<ExtraInfo>(optionString, extraInfo);
}

JvmOption<void*> makeJvmOption(const char* optionString) {
    void* extraInfo = 0;
    return JvmOption<void*>(optionString, extraInfo);
}

struct JavaInvocation {
private:
    JavaVM* vm;
    JNIEnv* env;
    LauncherArgumentParser& launcherArguments;
    
private:
    CreateJavaVM findCreateJavaVM() {
        SharedLibraryHandle sharedLibraryHandle = launcherArguments.openJvmLibrary();
        // Work around:
        // warning: ISO C++ forbids casting between pointer-to-function and pointer-to-object
        CreateJavaVM createJavaVM = reinterpret_cast<CreateJavaVM>(reinterpret_cast<uintptr_t>(dlsym(sharedLibraryHandle, "JNI_CreateJavaVM")));
        if (createJavaVM == 0) {
            std::ostringstream os;
            // Hopefully our caller will report something more useful than the shared library handle.
            os << "dlsym(" << sharedLibraryHandle << ", JNI_CreateJavaVM) failed with " << dlerror() << ".";
            throw std::runtime_error(os.str());
        }
        return createJavaVM;
    }
    
    jclass findClass(const std::string& className) {
        // Internally, the JVM tends to use '/'-separated class names.
        // Externally, '.'-separated class names are more common.
        // '.' is never valid in a class name, so we can unambiguously translate.
        std::string canonicalName(className);
        for (std::string::iterator it = canonicalName.begin(), end = canonicalName.end(); it != end; ++it) {
            if (*it == '.') {
                *it = '/';
            }
        }
        
        jclass javaClass = env->FindClass(canonicalName.c_str());
        if (javaClass == 0) {
            std::ostringstream os;
            reportAnyJavaException(os);
            os << "FindClass(\"" << canonicalName << "\") failed (FindClass loads and initializes the class as well as finding it).";
            throw std::runtime_error(os.str());
        }
        return javaClass;
    }
    
    jmethodID findMainMethod(jclass mainClass) {
        jmethodID method = env->GetStaticMethodID(mainClass, "main", "([Ljava/lang/String;)V");
        if (method == 0) {
            throw std::runtime_error("GetStaticMethodID(\"main\") failed.");
        }
        return method;
    }
    
    jstring makeJavaString(const char* nativeString) {
        jstring javaString = env->NewStringUTF(nativeString);
        if (javaString == 0) {
            std::ostringstream os;
            os << "NewStringUTF(\"" << nativeString << "\") failed.";
            throw std::runtime_error(os.str());
        }
        return javaString;
    }
    
    jobjectArray convertArguments(const NativeArguments& nativeArguments) {
        jclass jstringClass = findClass("java/lang/String");
        jstring defaultArgument = makeJavaString("");
        jobjectArray javaArguments = env->NewObjectArray(nativeArguments.size(), jstringClass, defaultArgument);
        if (javaArguments == 0) {
            std::ostringstream os;
            os << "NewObjectArray(" << nativeArguments.size() << ") failed.";
            throw std::runtime_error(os.str());
        }
        for (size_t index = 0; index != nativeArguments.size(); ++ index) {
            std::string nativeArgument = nativeArguments[index];
            jstring javaArgument = makeJavaString(nativeArgument.c_str());
            env->SetObjectArrayElement(javaArguments, index, javaArgument);
        }
        return javaArguments;
    }
    
public:
    explicit JavaInvocation(LauncherArgumentParser& launcherArguments0)
    : launcherArguments(launcherArguments0)
    {
        typedef std::vector<JavaVMOption> JavaVMOptions; // Required to be contiguous.
        const NativeArguments& jvmArguments = launcherArguments.getJvmArguments();
        JavaVMOptions javaVMOptions;
        for (size_t i = 0; i != jvmArguments.size(); ++i) {
            javaVMOptions.push_back(makeJvmOption(jvmArguments[i].c_str()));
        }
        // createJavaVM calls this if, for example, it can't find rt.jar.
        // We are a Cygwin process and, if we don't exit via a Cygwin function, it assumes that we succeeded.
        javaVMOptions.push_back(makeJvmOption("abort", &abortJvm));
        // Redirecting output into another C runtime's stdio implementation, well, crashes.
        //javaVMOptions.push_back(makeJvmOption("vfprintf", &vfprintf));
        // I think that System.exit() simply returns from the invocation of main, so we don't need to hook this.
        //javaVMOptions.push_back(makeJvmOption("exit", &exit));
        
        JavaVMInitArgs javaVMInitArgs;
        // Note that JNI version does not directly specify a JVM version, but needs to be at least 1.4 on Mac OS to get a modern JVM.
        javaVMInitArgs.version = JNI_VERSION_1_4;
        javaVMInitArgs.options = &javaVMOptions[0];
        javaVMInitArgs.nOptions = javaVMOptions.size();
        javaVMInitArgs.ignoreUnrecognized = false;
        
        CreateJavaVM createJavaVM = findCreateJavaVM();
        int result = createJavaVM(&vm, reinterpret_cast<void**>(&env), &javaVMInitArgs);
        if (result < 0) {
            std::ostringstream os;
            os << "JNI_CreateJavaVM(options=[";
            for (size_t i = 0; i < javaVMOptions.size(); ++i) {
                os << (i > 0 ? ", " : "") << '"' << javaVMOptions[i].optionString << '"';
            }
            os << "]) failed with " << JniError(result) << ".";
            throw std::runtime_error(os.str());
        }
    }
    
    void reportAnyJavaException(std::ostream& os) {
        jthrowable javaException = env->ExceptionOccurred();
        if (javaException == 0) {
            return;
        }
        // Report it via stderr first, in case we fail later and overwrite the pending exception.
        env->ExceptionDescribe();
        os << "A Java exception occurred.";
        os << std::endl;
        jclass stringUtilitiesClass = env->FindClass("e/util/StringUtilities");
        if (stringUtilitiesClass == 0) {
            os << "FindClass(\"e/util/StringUtilities\") failed.";
            os << std::endl;
            os << "Run with salma-hayek/.generated/classes/ on the classpath for GUI reporting of Java exceptions.";
            os << std::endl;
            return;
        }
        jmethodID stackTraceFromThrowable = env->GetStaticMethodID(stringUtilitiesClass, "stackTraceFromThrowable", "(Ljava/lang/Throwable;)Ljava/lang/String;");
        if (stackTraceFromThrowable == 0) {
            os << "GetStaticMethodID(e.util.StringUtilities.class(), \"stackTraceFromThrowable\") failed.";
            os << std::endl;
            return;
        }
        jstring report = static_cast<jstring>(env->CallStaticObjectMethod(stringUtilitiesClass, stackTraceFromThrowable, javaException));
        os << JniString(env, report);
    }
    
    ~JavaInvocation() {
        // If you attempt to destroy the VM with a pending JNI exception,
        // the VM crashes with an "internal error" and good luck to you finding
        // any reference to it on the web.
        if (env->ExceptionCheck()) {
            env->ExceptionDescribe();
        }
        
        // The non-obvious thing about DestroyJavaVM is that you have to call this
        // in order to wait for all the Java threads to quit - even if you don't
        // care about "leaking" the VM.
        // Deliberately ignore the error code, as the documentation says we must.
        vm->DestroyJavaVM();
    }
    
    int invokeMain() {
        jclass javaClass = findClass(launcherArguments.getMainClassName());
        jmethodID javaMethod = findMainMethod(javaClass);
        jobjectArray javaArguments = convertArguments(launcherArguments.getMainArguments());
        env->CallStaticVoidMethod(javaClass, javaMethod, javaArguments);
        if (env->ExceptionCheck() == false) {
            return 0;
        }
        std::ostringstream os;
        reportAnyJavaException(os);
        os << launcherArguments.getMainClassName() << ".main(...) failed.";
        throw std::runtime_error(os.str());
    }
};

std::string ErrorReporter::getUsage() const {
    std::ostringstream os;
    os << "Usage: " << ARGV0 << " [options] class [args...]" << std::endl;
    os << "where options are:" << std::endl;
    os << "  -client - use client VM" << std::endl;
    os << "  -server - use server VM" << std::endl;
    os << "  -cp <path> | -classpath <path> - set the class search path" << std::endl;
#ifdef __CYGWIN__
    os << "   (As the JVM is a native Windows program, the class path must use Windows directory names separated by semicolons.)" << std::endl;
#endif
    os << "  -D<name>=<value> - set a system property" << std::endl;
    os << "  -verbose[:class|gc|jni] - enable verbose output" << std::endl;
#ifdef __APPLE__
    os << "-Xdock:name=<name> - override default application name in dock" << std::endl;
    os << "-Xdock:icon=<filename> - override default icon in dock" << std::endl;
#endif
    // FIXME: If we know which version of JVM we've selected here, we could say so.
    os << "or any option implemented internally by the chosen JVM." << std::endl;
    os << std::endl;
    return os.str();
}

void ErrorReporter::generateReport(const std::exception& ex, const std::string& usage) const {
    std::ostringstream os;
#ifdef __CYGWIN__
    // As mentioned in the Terminator FAQ, the Windows JRE installer doesn't install Lucida Sans Typewriter by default.
    // The JDK installation does, but it's a much bigger download and the web page for choosing the download is hard to navigate.
    // The latter point is especially true for the target audience of this error message.
    os << "If you don't have Java installed, download it from http://java.com/, then try again." << std::endl;
    os << std::endl;
#endif
    os << "Error: " << ex.what() << std::endl;
    os << std::endl;
    
    os << "JVM selection:" << std::endl;
    os << progressOStream.str();
    os << std::endl;
    
    os << usage;
    
    os << "Command line was:";
    os << std::endl;
    os << ARGV0 << " ";
    os << join(" ", launcherArguments);
    os << std::endl;
    
    reportFatalErrorViaGui("Java Launcher", os.str(), supportAddress);
}

#if defined(__CYGWIN__) && defined(__x86_64__)

LONG CALLBACK handleVectoredException(PEXCEPTION_POINTERS) {
    return EXCEPTION_CONTINUE_SEARCH;
}

static void deferToHotSpotExceptionHandler() {
    // All the documentation says this means that the handler will be executed last.
    // So why does it have to be like this to subvert Cygwin?
    ULONG firstHandler = 1;
    PVOID handle = AddVectoredContinueHandler(firstHandler, &handleVectoredException);
    if (handle == 0) {
        // No mention is made of GetLastError() at the MSDN page du jour.
        throw std::runtime_error("AddVectoredContinueHandler failed");
    }
    // The handle of the next handler seems to be at our handle.
    // The previous handle follows.
    // The rest of the structure remained opaque to me, though I didn't try RtlDecodePointer.
    void* cygwinHandle = *reinterpret_cast<void**>(handle);
    ULONG rc = RemoveVectoredContinueHandler(cygwinHandle);
    if (rc == 0) {
        throw std::runtime_error("RemoveVectoredContinueHandler(cygwinHandle) failed");
    }
    rc = RemoveVectoredContinueHandler(handle);
    if (rc == 0) {
        throw std::runtime_error("RemoveVectoredContinueHandler(handle) failed");
    }
}

#else

static void deferToHotSpotExceptionHandler() {
}

#endif

static int runJvm(const NativeArguments& launcherArguments) {
    try {
        LauncherArgumentParser parser(launcherArguments);
        deferToHotSpotExceptionHandler();
        JavaInvocation javaInvocation(parser);
        return javaInvocation.invokeMain();
    } catch (const UsageError& ex) {
        errorReporter.reportUsageError(ex);
        return 1;
    } catch (const std::exception& ex) {
        errorReporter.reportFatalException(ex);
        return 1;
    }
}

int main(int, char* argv[]) {
    synchronizeWindowsEnvironment();
    
    NativeArguments launcherArguments;
    errorReporter.ARGV0 = *argv++;
    while (*argv != 0) {
        launcherArguments.push_back(*argv++);
    }
    errorReporter.launcherArguments = launcherArguments;
    
#ifdef __APPLE__
    return mac_runJvm(launcherArguments);
#else
    return runJvm(launcherArguments);
#endif
}
