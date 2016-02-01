#ifndef PORTABLE_JNI_H_included
#define PORTABLE_JNI_H_included

// http://cygwin.com/faq/faq.html#faq.programming.64bitporting says Cygwin64 is LP64.
// win32/jni_md.h assumes LLP64.
#if defined(__CYGWIN__) && defined(__LP64__)
#define jint original_jint
#include <jni_md.h>
#undef jint
typedef int jint;
#endif

#include <jni.h>

#endif
