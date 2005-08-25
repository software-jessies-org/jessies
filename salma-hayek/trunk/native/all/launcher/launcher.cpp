#ifdef __CYGWIN__
#include <windows.h>
#endif

#include <jni.h>

#include <deque>
#include <dlfcn.h>
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

extern "C" typedef jint JNICALL (*CreateJavaVM)(JavaVM**, void**, void*);

struct JavaInvocation {
private:
  JavaVM* vm;
  JNIEnv* env;
  
private:
  static std::string findJvmLibraryFilename() {
#if defined(__CYGWIN__)
    const char* javaHome = getenv("JAVA_HOME");
    if (javaHome == 0) {
      throw UsageError("please set $JAVA_HOME");
    }
    std::string pathToJre(javaHome);
    pathToJre.append("/jre");
    if (access(pathToJre.c_str(), X_OK) != 0) {
      pathToJre = javaHome;
    }
    return pathToJre + "/bin/client/jvm.dll";
#else
    // This only works on Linux if LD_LIBRARY_PATH is already setup.
    return "libjvm.so";
#endif
  }
  
  static CreateJavaVM findCreateJavaVM(const char* sharedLibraryFilename) {
    void* sharedLibraryHandle = dlopen(sharedLibraryFilename, RTLD_LAZY);
    if (sharedLibraryHandle == 0) {
      std::ostringstream os;
      os << "dlopen(" << sharedLibraryFilename << ") failed with " << dlerror();
      throw UsageError(os.str());
    }
    // Work around:
    // warning: ISO C++ forbids casting between pointer-to-function and pointer-to-object
    CreateJavaVM createJavaVM = reinterpret_cast<CreateJavaVM> (reinterpret_cast<long> (dlsym(sharedLibraryHandle, "JNI_CreateJavaVM")));
    if (createJavaVM == 0) {
      std::ostringstream os;
      os << "dlsym(" << sharedLibraryFilename << ", JNI_CreateJavaVM) failed with " << dlerror();
      throw UsageError(os.str());
    }
    return createJavaVM;
  }
  
  jclass findClass(const std::string& className) {
    jclass javaClass = env->FindClass(className.c_str());
    if (javaClass == 0) {
      std::ostringstream os;
      os << "FindClass(" << className << ") failed";
      throw UsageError(os.str());
    }
    return javaClass;
  }
  
  jmethodID findMainMethod(jclass mainClass) {
    jmethodID method = env->GetStaticMethodID(mainClass, "main", "([Ljava/lang/String;)V");
    if (method == 0) {
      throw UsageError("GetStaticMethodID(main) failed");
    }
    return method;
  }
  
  jstring makeJavaString(const char* nativeString) {
    jstring javaString = env->NewStringUTF(nativeString);
    if (javaString == 0) {
      std::ostringstream os;
      os << "NewStringUTF(" << nativeString << ") failed";
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
  JavaInvocation(const NativeArguments& jvmArguments) {
    std::string jvmLibraryFilename = findJvmLibraryFilename();
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
      jvmArguments.push_back(*it);
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
    JavaInvocation javaInvocation(parser.getJvmArguments());
    javaInvocation.invokeMain(parser.getClassName(), parser.getMainArguments());
  } catch (const UsageError& usageError) {
    std::ostream& os = std::cerr;
    os << usageError.what() << std::endl;
    os << "Usage: " << programName << " -options class [args...]" << std::endl;
    os << "where options are Java Virtual Machine options:" << std::endl;
    return 1;
  }
}
