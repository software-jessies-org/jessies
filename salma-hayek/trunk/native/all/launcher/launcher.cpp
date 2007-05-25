#ifdef __CYGWIN__
#include <windows.h>
#endif

#include "DirectoryIterator.h"
#include "JniError.h"
#include "join.h"
#include "synchronizeWindowsEnvironment.h"
#include "WindowsDirectoryChange.h"

#include <jni.h>

#include <algorithm>
#include <deque>
#include <dlfcn.h>
#include <fstream>
#include <iostream>
#include <sstream>
#include <stdexcept>
#include <string>
#include <stdlib.h>
#include <stdio.h>
#include <unistd.h>
#include <vector>

struct UsageError : std::runtime_error {
  UsageError(const std::string& description)
  : std::runtime_error(description) {
  }
};

typedef std::deque<std::string> NativeArguments;

extern "C" {
  typedef jint JNICALL (*CreateJavaVM)(JavaVM**, void**, void*);
}

typedef void* SharedLibraryHandle;

SharedLibraryHandle openSharedLibrary(const std::string& sharedLibraryFilename) {
  void* sharedLibraryHandle = dlopen(sharedLibraryFilename.c_str(), RTLD_LAZY);
  if (sharedLibraryHandle == 0) {
    std::ostringstream os;
    os << "dlopen(\"" << sharedLibraryFilename << "\") failed with " << dlerror() << ".";
    throw UsageError(os.str());
  }
  return sharedLibraryHandle;
}

static std::string readFile(const std::string& path) {
  std::ifstream is(path.c_str());
  if (is.good() == false) {
    throw UsageError("Couldn't open \"" + path + "\".");
  }
  std::ostringstream contents;
  contents << is.rdbuf();
  return contents.str();
}

static std::string readRegistryFile(const std::string& path) {
  std::string contents = readFile(path);
  // Cygwin's representation of REG_SZ keys seems to include the null terminator.
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
    // (We have some reason to believe that the UTF-16 Win32 file API doesn't support interchangable slashes
    // and we'd like Cygwin to be UTF-16 below so it could present UTF-8 above.)
    std::string jreBin = javaHome + pathFromJavaHomeToJre + "\\bin\\";
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

struct JvmLocation {
private:
  std::string jvmDirectory;
  
public:
  typedef std::vector<JvmRegistryKey> JvmRegistryKeys;
  
  bool isUnreasonableVersion(std::ostream& os, const std::string& version) const {
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
    if (version < "1.5") {
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
  
  void findVersionsInRegistry(std::ostream& os, JvmRegistryKeys& jvmRegistryKeys, const std::string& registryPath, const char* pathFromJavaHomeToJre) const {
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
  
  SharedLibraryHandle openJvmLibraryUsingRegistry(const char* javaVendor, const char* jdkName, const char* jreName) const {
    const std::string registryPrefix = std::string("/proc/registry/HKEY_LOCAL_MACHINE/SOFTWARE/") + javaVendor + "/";
    const std::string jreRegistryPath = registryPrefix + jreName;
    const std::string jdkRegistryPath = registryPrefix + jdkName;
    std::ostringstream os;
    JvmRegistryKeys jvmRegistryKeys;
    findVersionsInRegistry(os, jvmRegistryKeys, jreRegistryPath, "");
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
    throw UsageError(os.str());
  }
  
  // Once we've successfully opened a shared library, I think we're committed to trying to use it
  // or else who knows what it's DLL entry point has done.
  // Until we've successfully opened it, though, we can keep trying alternatives.
  SharedLibraryHandle openWin32JvmLibrary() const {
    std::ostringstream os;
    os << "Couldn't find jvm.dll - please install a 1.5 or newer JRE or JDK.";
    os << std::endl;
    os << "Error messages were:";
    os << std::endl;
    try {
      return openJvmLibraryUsingRegistry("JavaSoft", "Java Development Kit", "Java Runtime Environment");
    } catch (const std::exception& ex) {
      os << ex.what();
      os << std::endl;
    }
    try {
      // My Sun JDK key says:
      // "JavaHome"="C:\\Program Files\\Java\\jdk1.5.0_06"
      // Jesse Kriss's IBM JDK has an appended "jre" component:
      // "JavaHome"="C:\\Program Files\\IBM\\Java50\\jre"
      return openJvmLibraryUsingRegistry("IBM", "Java Development Kit", "Java2 Runtime Environment");
    } catch (const std::exception& ex) {
      os << ex.what();
      os << std::endl;
    }
    throw UsageError(os.str());
  }
  
  SharedLibraryHandle openJvmLibrary() const {
#if defined(__CYGWIN__)
    return openWin32JvmLibrary();
#else
    // This only works on Linux if LD_LIBRARY_PATH is already set up to include something like:
    // "$JAVA_HOME/jre/lib/$ARCH/" + jvmDirectory
    // "$JAVA_HOME/jre/lib/$ARCH"
    // "$JAVA_HOME/jre/../lib/$ARCH"
    // Where $ARCH is "i386" rather than `arch`.
    return openSharedLibrary("libjvm.so");
#endif
  }
  
  void setClientClass() {
    jvmDirectory = "client";
  }
  void setServerClass() {
    jvmDirectory = "server";
  }
  
  JvmLocation() {
    setClientClass();
  }
};

struct JavaInvocation {
private:
  JavaVM* vm;
  JNIEnv* env;
  
private:
  static CreateJavaVM findCreateJavaVM(SharedLibraryHandle sharedLibraryHandle) {
    // Work around:
    // warning: ISO C++ forbids casting between pointer-to-function and pointer-to-object
    CreateJavaVM createJavaVM = reinterpret_cast<CreateJavaVM> (reinterpret_cast<long> (dlsym(sharedLibraryHandle, "JNI_CreateJavaVM")));
    if (createJavaVM == 0) {
      std::ostringstream os;
      // Hopefully our caller will report something more useful than the shared library handle.
      os << "dlsym(" << sharedLibraryHandle << ", JNI_CreateJavaVM) failed with " << dlerror() << ".";
      throw UsageError(os.str());
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
      os << "FindClass(\"" << canonicalName << "\") failed.";
      throw UsageError(os.str());
    }
    return javaClass;
  }
  
  jmethodID findMainMethod(jclass mainClass) {
    jmethodID method = env->GetStaticMethodID(mainClass, "main", "([Ljava/lang/String;)V");
    if (method == 0) {
      throw UsageError("GetStaticMethodID(\"main\") failed.");
    }
    return method;
  }
  
  jstring makeJavaString(const char* nativeString) {
    jstring javaString = env->NewStringUTF(nativeString);
    if (javaString == 0) {
      std::ostringstream os;
      os << "NewStringUTF(\"" << nativeString << "\") failed.";
      throw UsageError(os.str());
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
      throw UsageError(os.str());
    }
    for (size_t index = 0; index != nativeArguments.size(); ++ index) {
      std::string nativeArgument = nativeArguments[index];
      jstring javaArgument = makeJavaString(nativeArgument.c_str());
      env->SetObjectArrayElement(javaArguments, index, javaArgument);
    }
    return javaArguments;
  }
  
public:
  JavaInvocation(SharedLibraryHandle jvmLibraryHandle, const NativeArguments& jvmArguments) {
    CreateJavaVM createJavaVM = findCreateJavaVM(jvmLibraryHandle);
    
    typedef std::vector<JavaVMOption> JavaVMOptions; // Required to be contiguous.
    JavaVMOptions javaVMOptions(jvmArguments.size());
    for (size_t ii = 0; ii != jvmArguments.size(); ++ ii) {
      // I'm sure the JVM doesn't actually write to its options.
      javaVMOptions[ii].optionString = const_cast<char*>(jvmArguments[ii].c_str());
    }
    
    JavaVMInitArgs javaVMInitArgs;
    javaVMInitArgs.version = JNI_VERSION_1_2;
    javaVMInitArgs.options = &javaVMOptions[0];
    javaVMInitArgs.nOptions = javaVMOptions.size();
    javaVMInitArgs.ignoreUnrecognized = false;
    
    int result = createJavaVM(&vm, reinterpret_cast<void**>(&env), &javaVMInitArgs);
    if (result < 0) {
      std::ostringstream os;
      os << "JNI_CreateJavaVM(options=[";
      for (size_t i = 0; i < javaVMOptions.size(); ++i) {
        os << (i > 0 ? ", " : "") << '"' << javaVMOptions[i].optionString << '"';
      }
      os << "]) failed with " << JniError(result) << ".";
      throw UsageError(os.str());
    }
  }
  
  ~JavaInvocation() {
    // If you attempt to destroy the VM with a pending JNI exception,
    // the VM crashes with an "internal error" and good luck to you finding
    // any reference to it on google.
    if (env->ExceptionOccurred()) {
      env->ExceptionDescribe();
    }
    
    // The non-obvious thing about DestroyJavaVM is that you have to call this
    // in order to wait for all the Java threads to quit - even if you don't
    // care about "leaking" the VM.
    // Deliberately ignore the error code, as the documentation says we must.
    vm->DestroyJavaVM();
  }
  
  int invokeMain(const std::string& className, const NativeArguments& nativeArguments) {
    jclass javaClass = findClass(className);
    jmethodID javaMethod = findMainMethod(javaClass);
    jobjectArray javaArguments = convertArguments(nativeArguments);
    env->CallStaticVoidMethod(javaClass, javaMethod, javaArguments);
    return (env->ExceptionOccurred() ? 1 : 0);
  }
};

struct LauncherArgumentParser {
private:
  JvmLocation jvmLocation;
  NativeArguments jvmArguments;
  std::string className;
  NativeArguments mainArguments;
  
private:
  static bool beginsWith(const std::string& st, const std::string& prefix) {
    return st.substr(0, prefix.size()) == prefix;
  }
  
public:
  LauncherArgumentParser(const NativeArguments& launcherArguments) {
    NativeArguments::const_iterator it = launcherArguments.begin();
    NativeArguments::const_iterator end = launcherArguments.end();
    while (it != end && beginsWith(*it, "-")) {
      std::string option = *it++;
      if (option == "-cp" || option == "-classpath") {
        if (it == end) {
          throw UsageError(option + " requires an argument.");
        }
        // Translate to a form the JVM understands.
        std::string classPath = *it++;
        jvmArguments.push_back("-Djava.class.path=" + classPath);
      } else if (option == "-client") {
        jvmLocation.setClientClass();
      } else if (option == "-server") {
        jvmLocation.setServerClass();
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
  }
  
  SharedLibraryHandle openJvmLibrary() const {
    return jvmLocation.openJvmLibrary();
  }
  NativeArguments getJvmArguments() const {
    return jvmArguments;
  }
  std::string getClassName() const {
    return className;
  }
  NativeArguments getMainArguments() const {
    return mainArguments;
  }
};

int main(int, char** argv) {
  synchronizeWindowsEnvironment();
  const char* programName = *argv;
  ++ argv;
  NativeArguments launcherArguments;
  while (*argv != 0) {
    launcherArguments.push_back(*argv);
    ++ argv;
  }
  try {
    LauncherArgumentParser parser(launcherArguments);
    JavaInvocation javaInvocation(parser.openJvmLibrary(), parser.getJvmArguments());
    return javaInvocation.invokeMain(parser.getClassName(), parser.getMainArguments());
  } catch (const UsageError& usageError) {
    std::ostringstream os;
    os << "Error: " << usageError.what() << std::endl;
    os << std::endl;
    os << "Usage: " << programName << " [options] class [args...]" << std::endl;
    os << "where options are:" << std::endl;
    os << "  -client - use client VM" << std::endl;
    os << "  -server - use server VM" << std::endl;
    os << "  -cp <path> | -classpath <path> - set the class search path" << std::endl;
    os << "  -D<name>=<value> - set a system property" << std::endl;
    os << "  -verbose[:class|gc|jni] - enable verbose output" << std::endl;
    // FIXME: If we know which version of JVM we've selected here, we could say so.
    os << "or any option implemented internally by the chosen JVM." << std::endl;
    os << std::endl;
    os << "Command line was:";
    os << std::endl;
    os << programName << " ";
    os << join(" ", launcherArguments);
    os << std::endl;
    std::cerr << os.str();
#ifdef __CYGWIN__
    std::string message = "If you don't have Java installed, please download and install JDK 6 from http://java.sun.com/javase/downloads/ and try again.\n";
    message += "\n";
    message += "Otherwise, please copy this message to the clipboard with Ctrl-C and mail it to software@jessies.org.\n";
    message += "(Windows won't let you select the text but Ctrl-C works anyway.)\n";
    message += "\n";
    message += os.str();
    MessageBox(GetActiveWindow(), message.c_str(), "Launcher", MB_OK);
#endif
    return 1;
  }
}
