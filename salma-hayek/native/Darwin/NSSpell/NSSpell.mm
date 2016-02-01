#import <AppKit/AppKit.h>
#import <iostream>
#import <string>
#import "ScopedAutoReleasePool.h"

/**
 * Ensures that all users of NSSpellChecker check for the case where it returns nil.
 * We deliberately output the error on stdout rather than stderr because this program is meant to be run in a pipe, and stdout is more likely to be seen by the user.
 */
static NSSpellChecker* sharedSpellingChecker() {
    NSSpellChecker* checker = [NSSpellChecker sharedSpellChecker];
    if (checker == nil) {
        std::cout << "[NSSpellChecker sharedSpellChecker] returned nil." << std::endl;
        exit(2);
    }
    return checker;
}

/**
 * Allows NSString to be output like std::string or a const char*.
 */
static std::ostream& operator<<(std::ostream& os, NSString* rhs) {
    os << [rhs UTF8String];
    return os;
}

/**
 * Tests whether 'word' is correctly spelled.
 */
static bool isCorrect(NSString* word) {
    ScopedAutoReleasePool pool;
    NSRange range = [sharedSpellingChecker() checkSpellingOfString:word startingAt:0];
    // We only check the NSRange's length field because the location field appears to be set to an uninitialized value for incorrectly-spelled words.
    return (range.length == 0);
}

/**
 * Outputs suggested corrections for the misspelled word 'word' in ispell(1) format.
 */
static void showSuggestionsForWord(NSString* word) {
    ScopedAutoReleasePool pool;
    std::ostream& os = std::cout;
    
    NSArray* guesses = [sharedSpellingChecker() guessesForWord:word];
    if ([guesses count] == 0) {
        os << "# " << word << " 0" << std::endl;
        return;
    }
    
    os << "& " << word << " " << [guesses count] << " 0: ";
    for (size_t i = 0; i < [guesses count]; ++i) {
        if (i > 0) {
            os << ", ";
        }
        NSString* guess = [guesses objectAtIndex:i];
        os << guess;
    }
    os << std::endl;
}

static void ispellLoop() {
    std::ostream& os(std::cout);
    os << "@(#) International Ispell 3.1.20 (but really NSSpellChecker)" << std::endl;
    while (true) {
        ScopedAutoReleasePool pool;
        std::string line;
        getline(std::cin, line);
        if (line.length() == 0) {
            // We're done.
            [[NSApplication sharedApplication] terminate:nil];
        } else if (line == "!") {
            // set terse mode; ignore.
        } else if (line[0] == '^') {
            std::string word(line.substr(1));
            NSString* string = [NSString stringWithUTF8String:word.c_str()];
            if (isCorrect(string) == false) {
                showSuggestionsForWord(string);
            }
            os << std::endl;
        } else if (line[0] == '*') {
            std::string word(line.substr(1));
            // FIXME: insert word into dictionary.
            // Note that this is not actually needed for salma-hayek's purposes
            // anymore, since it now keeps its own record of those spellings
            // the user has accepted.
        } else {
            abort();
        }
    }
}

@interface Ispell : NSObject
- (void) ispellRunLoop:(id) unusedParameter;
@end

@implementation Ispell
- (void) ispellRunLoop:(id) unusedParameter {
    (void) unusedParameter;
    // Each thread needs its own pool, and we don't get one for free.
    ScopedAutoReleasePool pool;
    ispellLoop();
}
@end

int main(int /*argc*/, char* /*argv*/[]) {
    ScopedAutoReleasePool pool;
    
    // Initialize NSApplication before detaching our worker thread.
    // Otherwise NSSpellChecker sharedSpellChecker will return nil.
    [NSApplication sharedApplication];
    
    // Pretend to be ispell in a new thread, so we can enter the normal event
    // loop and prevent Spin Control from mistakenly diagnosing us as a hung
    // GUI application.
    // It seems to be important that we invoke an instance method rather than
    // a class method.
    Ispell* ispell = [[Ispell alloc] init];
    [NSThread detachNewThreadSelector:@selector(ispellRunLoop:) toTarget:ispell withObject:nil];
    
    [NSApp run];
}
