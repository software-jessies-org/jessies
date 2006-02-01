#ifdef __CYGWIN__
#include <windows.h>
#endif

#include "DirectoryIterator.h"
#include "join.h"

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

struct JvmLocation {
private:
  std::string jvmDirectory;
  
public:
  std::string findJvmLibraryUsingJavaHome() const {
    const char* javaHome = getenv("JAVA_HOME");
    if (javaHome == 0) {
      throw UsageError("please set $JAVA_HOME");
    }
    std::string pathToJre(javaHome);
    pathToJre.append("/jre");
    if (access(pathToJre.c_str(), X_OK) != 0) {
      pathToJre = javaHome;
    }
    return pathToJre + "/bin/" + jvmDirectory + "/jvm.dll";
  }
  
  static std::string readFile(const std::string& path) {
    std::ifstream is(path.c_str());
    if (is.good() == false) {
      throw UsageError("couldn't open \"" + path + "\"");
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
  
  // What should we do if this points to "client" when we want "server"?
  std::string findJvmLibraryUsingJreRegistry() const {
    const char* jreRegistryPath = "/proc/registry/HKEY_LOCAL_MACHINE/SOFTWARE/JavaSoft/Java Runtime Environment";
    std::vector<std::string> versions;
    for (DirectoryIterator it(jreRegistryPath); it.isValid(); ++ it) {
      std::string version = it->getName();
      if (version.empty() || version[0] != '1') {
        // Avoid "CurrentVersion", "BrowserJavaVersion", or anything else Sun might think of.
        // "CurrentVersion" didn't get updated when I installed JDK-1.5.0_06 (or the two prior versions by the look of it)..
        continue;
      }
      versions.push_back(version);
    }
    std::sort(versions.begin(), versions.end());
    if (versions.empty()) {
      throw UsageError("no JRE installed");
    }
    std::string version = versions.back();
    std::string jvmRegistryPath = std::string(jreRegistryPath) + "/" + version + "/RuntimeLib";
    return readRegistryFile(jvmRegistryPath);
  }
  
  std::string findJvmLibraryUsingJdkRegistry() const {
    // From the look of my registry, this key points to the latest (or most recently installed?) update.
    std::string javaHome = readRegistryFile("/proc/registry/HKEY_LOCAL_MACHINE/SOFTWARE/JavaSoft/Java Development Kit/1.5/JavaHome");
    return javaHome + "/jre/bin/client/jvm.dll";
  }
  
  std::string findWin32JvmLibrary() const {
    std::ostringstream os;
    os << "Couldn't find jvm.dll - please set $JAVA_HOME or install a JRE or JDK.";
    os << std::endl;
    os << "Error messages were:";
    os << std::endl;
    try {
      return findJvmLibraryUsingJavaHome();
    } catch (const std::exception& ex) {
      os << "  ";
      os << ex.what();
      os << std::endl;
    }
    try {
      return findJvmLibraryUsingJdkRegistry();
    } catch (const std::exception& ex) {
      os << "  ";
      os << ex.what();
      os << std::endl;
    }
    try {
      return findJvmLibraryUsingJreRegistry();
    } catch (const std::exception& ex) {
      os << "  ";
      os << ex.what();
      os << std::endl;
    }
    throw UsageError(os.str());
  }
  
  std::string findJvmLibraryFilename() const {
#if defined(__CYGWIN__)
    return findWin32JvmLibrary();
#else
    // This only works on Linux if LD_LIBRARY_PATH is already set up to include something like:
    // "$JAVA_HOME/jre/lib/$ARCH/" + jvmDirectory
    // "$JAVA_HOME/jre/lib/$ARCH"
    // "$JAVA_HOME/jre/../lib/$ARCH"
    // Where $ARCH is "i386" rather than `arch`.
    return "libjvm.so";
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
  static CreateJavaVM findCreateJavaVM(const char* sharedLibraryFilename) {
    void* sharedLibraryHandle = dlopen(sharedLibraryFilename, RTLD_LAZY);
    if (sharedLibraryHandle == 0) {
      std::ostringstream os;
      os << "dlopen(\"" << sharedLibraryFilename << "\") failed with " << dlerror();
      throw UsageError(os.str());
    }
    // Work around:
    // warning: ISO C++ forbids casting between pointer-to-function and pointer-to-object
    CreateJavaVM createJavaVM = reinterpret_cast<CreateJavaVM> (reinterpret_cast<long> (dlsym(sharedLibraryHandle, "JNI_CreateJavaVM")));
    if (createJavaVM == 0) {
      std::ostringstream os;
      os << "dlsym(\"" << sharedLibraryFilename << "\", JNI_CreateJavaVM) failed with " << dlerror();
      throw UsageError(os.str());
    }
    return createJavaVM;
  }
  
  jclass findClass(const std::string& className) {
    jclass javaClass = env->FindClass(className.c_str());
    if (javaClass == 0) {
      std::ostringstream os;
      os << "FindClass(\"" << className << "\") failed";
      throw UsageError(os.str());
    }
    return javaClass;
  }
  
  jmethodID findMainMethod(jclass mainClass) {
    jmethodID method = env->GetStaticMethodID(mainClass, "main", "([Ljava/lang/String;)V");
    if (method == 0) {
      throw UsageError("GetStaticMethodID(\"main\") failed");
    }
    return method;
  }
  
  jstring makeJavaString(const char* nativeString) {
    jstring javaString = env->NewStringUTF(nativeString);
    if (javaString == 0) {
      std::ostringstream os;
      os << "NewStringUTF(\"" << nativeString << "\") failed";
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
      os << "NewObjectArray(" << nativeArguments.size() << ") failed";
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
  JavaInvocation(const std::string& jvmLibraryFilename, const NativeArguments& jvmArguments) {
    CreateJavaVM createJavaVM = findCreateJavaVM(jvmLibraryFilename.c_str());
    
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
      os << "JNI_CreateJavaVM(" << javaVMOptions.size() << " options) failed with " << result;
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
  
  void invokeMain(const std::string& className, const NativeArguments& nativeArguments) {
    jclass javaClass = findClass(className);
    jmethodID javaMethod = findMainMethod(javaClass);
    jobjectArray javaArguments = convertArguments(nativeArguments);
    env->CallStaticVoidMethod(javaClass, javaMethod, javaArguments);
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
      std::string option = *it;
      if (option == "-client") {
        jvmLocation.setClientClass();
      } else if (option == "-server") {
        jvmLocation.setServerClass();
      } else {
        jvmArguments.push_back(option);
      }
      ++ it;
    }
    if (it == end) {
      throw UsageError("no class specified");
    }
    className = *it;
    ++ it;
    while (it != end) {
      mainArguments.push_back(*it);
      ++ it;
    }
  }
  
  std::string getJvmLibraryFilename() const {
    return jvmLocation.findJvmLibraryFilename();
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
  const char* programName = *argv;
  ++ argv;
  NativeArguments launcherArguments;
  while (*argv != 0) {
    launcherArguments.push_back(*argv);
    ++ argv;
  }
  try {
    LauncherArgumentParser parser(launcherArguments);
    JavaInvocation javaInvocation(parser.getJvmLibraryFilename(), parser.getJvmArguments());
    javaInvocation.invokeMain(parser.getClassName(), parser.getMainArguments());
  } catch (const UsageError& usageError) {
    std::ostringstream os;
    os << usageError.what() << std::endl;
    os << "Usage: " << programName << " -options class [args...]" << std::endl;
    os << "where options are Java Virtual Machine options or:" << std::endl;
    os << "  -client";
    os << std::endl;
    os << "  -server ... to select a JVM";
    os << std::endl;
    os << "Command line was:";
    os << std::endl;
    os << programName << " ";
    os << join(" ", launcherArguments);
    os << std::endl;
    std::cerr << os.str();
#ifdef __CYGWIN__
    MessageBox(GetActiveWindow(), os.str().c_str(), "Launcher", MB_OK);
#endif
    return 1;
  }
}
