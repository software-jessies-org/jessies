#include <AppKit/AppKit.h>

static NSSpellChecker* NSSpellChecker_init() {
    static NSSpellChecker* checker = 0;
    if (checker != 0) {
        return checker;
    }

    // These are required if we want Cocoa to work.
    static NSAutoreleasePool* global_pool;
    global_pool = [[NSAutoreleasePool alloc] init];
    static NSApplication* application;
    application = [NSApplication sharedApplication];

    // This is what we're really after.
    checker = [NSSpellChecker sharedSpellChecker];
    return checker;
}

void NSSpellChecker_showSuggestions(const char* word) {
    NSSpellChecker* checker = NSSpellChecker_init();

    NSAutoreleasePool* pool = [[NSAutoreleasePool alloc] init];

    NSString* string = [[NSString stringWithUTF8String:word] autorelease];
    NSArray* guesses = [checker guessesForWord:string];
    if ([guesses count] == 0) {
        printf("#\n");
        [pool release];
        return;
    }

    printf("& %s %i 0: ", word, [guesses count]);
    for (size_t i = 0; i < [guesses count]; ++i) {
        if (i > 0) {
            printf(", ");
        }
        NSString* guess = [guesses objectAtIndex:i];
        printf("%s", [guess UTF8String]);
    }
    printf("\n");
    [pool release];
}

bool NSSpellChecker_isCorrect(const char* word) {
    NSSpellChecker* checker = NSSpellChecker_init();

    bool isCorrect = false;
    NSAutoreleasePool* pool = [[NSAutoreleasePool alloc] init];
    {
        NSString* string = [NSString stringWithUTF8String:word];
        NSRange range = [checker checkSpellingOfString:string startingAt:0];
        isCorrect = (range.length == 0);
    }
    [pool release];
    return isCorrect;
}
