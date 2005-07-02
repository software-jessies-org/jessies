#include <dlfcn.h>
#include <iostream>
#include <stdexcept>
#include <string>
#include <boost/static_assert.hpp>
#include <boost/type_traits/function_traits.hpp>

/**
 * A dynamically-loaded library. Useful only as a parameter to
 * DynamicFunction's constructor.
 */
class DynamicLibrary {
private:
    void* m_handle;

public:
    /**
     * Constructs a DynamicLibrary representing the named system library.
     * See dlopen(3) for details of the search process. If this causes the
     * library to be loaded and linked, the library's initializer functions
     * will be run as a side-effect.
     */
    explicit DynamicLibrary(const std::string& name) {
        m_handle = ::dlopen(name.c_str(), 0);
        if (m_handle == 0) {
            throwDynamicLinkError();
        }
    }

    /**
     * Releases our reference to the dynamic library. The dynamic library
     * will only be removed from our address space if this was the last
     * reference to it.
     */
    ~DynamicLibrary() {
        int result = ::dlclose(m_handle);
        if (result == -1) {
            throwDynamicLinkError();
        }
    }

    static void demo();

private:
    /**
     * Returns the address of the named symbol (which may be code or data).
     * The symbol name must not be prepended with an underscore.
     */
    void* lookUpSymbol(const std::string& name) const {
        void* result = ::dlsym(m_handle, name.c_str());
        if (result == 0) {
            throwDynamicLinkError();
        }
        return result;
    }

    void throwDynamicLinkError() const {
        throw std::runtime_error(::dlerror());
    }

    template <typename FunctionType> friend class DynamicFunction;
};

/**
 * A function object for a dynamically-loaded function.
 *
 *      DynamicLibrary library("libSystem.dylib");
 *      DynamicFunction<int (const char*)> puts_fn(library, "puts");
 *      int puts_result = puts_fn("hello");
 *
 * You're protected by compile-time errors against invoking the function
 * with a different number of arguments than you declared it to take.
 *
 * You're protected by compile-time warnings (so use -Werror) against
 * invoking the function with types that require an invalid conversion
 * to get to the declared type. A gotcha here is the use of 0 to mean,
 * for example, the null char*. This will be considered an invalid
 * conversion from "int" to "char*" unless you use an explicit cast.
 *
 * You're not protected against declaring your function with a type that
 * doesn't correspond to the symbol itself. Even if you give a C++ mangled
 * name, no interpretation of that mangling is done to deduce the function
 * type.
 *
 * Similarly, you're not protected against using a symbol that corresponds
 * to data rather than code.
 */
template <typename FunctionType>
class DynamicFunction {
private:
    void* m_fn;
    typedef typename boost::function_traits<FunctionType> FunctionTraits;
    typedef typename FunctionTraits::result_type ReturnType;

public:
    explicit DynamicFunction(const DynamicLibrary& library, const std::string& name) {
        m_fn = library.lookUpSymbol(name);
    }

    ReturnType operator()() const {
        BOOST_STATIC_ASSERT(FunctionTraits::arity == 0);
        typedef ReturnType (*FunctionPointer0)();
        return ((FunctionPointer0) m_fn)();
    }

    template <typename T>
    ReturnType operator()(T a1) const {
        BOOST_STATIC_ASSERT(FunctionTraits::arity == 1);
        typedef ReturnType (*FunctionPointer1)(typename FunctionTraits::arg1_type);
        return ((FunctionPointer1) m_fn)(a1);
    }

    template <typename T, typename U>
    ReturnType operator()(T a1, U a2) const {
        BOOST_STATIC_ASSERT(FunctionTraits::arity == 2);
        typedef ReturnType (*FunctionPointer2)(typename FunctionTraits::arg1_type, typename FunctionTraits::arg2_type);
        return ((FunctionPointer2) m_fn)(a1, a2);
    }
};

inline void DynamicLibrary::demo() {
    try {
        DynamicLibrary library("libSystem.dylib");

        DynamicFunction<int (const char*)> puts_fn(library, "puts");
        int puts_result = puts_fn("hello");
        std::cout << "puts_result=" << puts_result << std::endl;

        DynamicFunction<int (const char*, const char*)> printf_fn(library, "printf");
        int printf_result = printf_fn("%s\n", "hello");
        std::cout << "printf_result=" << printf_result << std::endl;

        DynamicFunction<pid_t ()> getpid_fn(library, "getpid");
        std::cout << "pid=" << getpid_fn() << std::endl;

        DynamicFunction<char* (char*)> getcwd_fn(library, "getcwd");
        char* cwd = getcwd_fn((char*) 0);
        std::cout << "cwd=" << cwd << std::endl;
        free(cwd);

        DynamicFunction<int (pid_t, int)> kill_fn(library, "kill");
        kill_fn(getpid_fn(), SIGINT);
    } catch (std::exception& ex) {
        std::cerr << "uncaught exception: " << ex.what() << std::endl;
    }
}

#ifdef TEST_DYNAMIC_LIBRARY_H
int main() {
    DynamicLibrary::demo();
    return 0;
}
#endif
