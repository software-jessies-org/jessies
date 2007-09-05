#ifndef SCOPED_AUTO_RELEASE_POOL_H_included
#define SCOPED_AUTO_RELEASE_POOL_H_included

#include <AppKit/AppKit.h>

/**
 * Creates and releases an NSAutoreleasePool in its scope. Having to manually
 * release an autorelease pool on every exit point from a method is silly, but
 * Objective-C++ lets us fix this.
 */
class ScopedAutoReleasePool {
public:
    ScopedAutoReleasePool() {
        m_pool =  [[NSAutoreleasePool alloc] init];
    }
    
    ~ScopedAutoReleasePool() {
        [m_pool release];
    }
    
private:
    NSAutoreleasePool* m_pool;
};

#endif
