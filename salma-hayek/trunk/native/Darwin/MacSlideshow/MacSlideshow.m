// gcc -o slideshow slideshow.m -framework Cocoa -F/System/Library/PrivateFrameworks -framework Slideshow && ./slideshow

#include <Cocoa/Cocoa.h>

#include "PrivateFrameworks/Slideshow.h"

@interface MyDelegate : NSObject {
  NSMutableArray* mFilenames;
}
- (void) addFilename:(const char*)utf8;
@end

@implementation MyDelegate
- (id)init {
    self = [super init];
    if (self != nil) {
        mFilenames = [[NSMutableArray alloc] init];
    }
    return self;
}

- (void)dealloc {
    [mFilenames dealloc];
    [super dealloc];
}

// Useful if you need to find out what interface you have to implement...
- (BOOL)respondsToSelector:(SEL)aSelector {
    //NSLog(@"respondsToSelector: %@", NSStringFromSelector(aSelector));
    return [super respondsToSelector: aSelector];
}

- (void)addFilename:(const char*)utf8 {
    [mFilenames addObject:[NSString stringWithUTF8String: utf8]];
}

- (int)numberOfObjectsInSlideshow {
    return [mFilenames count];
}

- (id)slideshowObjectAtIndex:(int)index {
    return [mFilenames objectAtIndex: index];
}

// Start the slideshow as soon as we're running.
- (void)applicationDidFinishLaunching:(NSNotification *)aNotification {
    (void) aNotification;

    Slideshow* slideshow = [Slideshow sharedSlideshow];
    [slideshow runSlideshowWithDataSource:self options:nil];

    if (false) {
    NSURL* pdfUrl = [NSURL fileURLWithPath:@"/Users/elliotth/Desktop/putfield.pdf"];
    [slideshow runSlideshowWithPDF:pdfUrl options:nil];
    }
}

// Tell Cocoa that when the slideshow ends, our work here is done.
- (BOOL)applicationShouldTerminateAfterLastWindowClosed:(NSApplication*)theApplication {
    (void)theApplication;
    return YES;
}

@end

int main(int argCount, char* argValues[]) {
    // These are required if we want Cocoa to work.
    static NSAutoreleasePool* global_pool;
    global_pool = [[NSAutoreleasePool alloc] init];
    static NSApplication* application;
    application = [NSApplication sharedApplication];

    MyDelegate* delegate = [[MyDelegate alloc] init];
    (void) argCount;
    while (*++argValues) {
      [delegate addFilename: *argValues];
    }
    [application setDelegate: delegate];
    [application run];

    return 0;
}
