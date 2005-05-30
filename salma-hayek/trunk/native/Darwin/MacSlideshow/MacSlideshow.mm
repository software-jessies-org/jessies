#import <AppKit/AppKit.h>
#import "PrivateFrameworks/Slideshow.h"
#import "ScopedAutoReleasePool.h"

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
    ScopedAutoReleasePool pool;

    MyDelegate* delegate = [[MyDelegate alloc] init];
    (void) argCount;
    while (*++argValues) {
        [delegate addFilename: *argValues];
    }

    NSApplication* application = [NSApplication sharedApplication];
    [application setDelegate: delegate];
    [application run];

    return 0;
}
