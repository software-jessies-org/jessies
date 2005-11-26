#import <AppKit/AppKit.h>
#import <iostream>
#import <string>
#import "ScopedAutoReleasePool.h"

/**
 * Allows NSString to be output like std::string or a const char*.
 */
std::ostream& operator<<(std::ostream& os, NSString* rhs) {
    os << [rhs UTF8String];
    return os;
}

@interface Ispell : NSObject
+ (void) ispellRunLoop:(id) unusedParameter;
@end

@implementation Ispell

NSSpellChecker* checker;

+ (void) showSuggestionsForWord:(NSString*) word {
    ScopedAutoReleasePool pool;
    std::ostream& os = std::cout;
    
    NSArray* guesses = [checker guessesForWord:word];
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

+ (bool) isCorrect:(NSString*) word {
    ScopedAutoReleasePool pool;
    NSRange range = [checker checkSpellingOfString:word startingAt:0];
    bool isCorrect = (range.length == 0);
    return isCorrect;
}

+ (void) ispellRunLoop:(id) unusedParameter {
    (void) unusedParameter;
    
    // Each thread needs its own pool, and we don't get one for free.
    ScopedAutoReleasePool pool;
    checker = [NSSpellChecker sharedSpellChecker];
    
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
            if ([Ispell isCorrect:string] == false) {
                [Ispell showSuggestionsForWord:string];
            }
            os << std::endl;
        } else if (line[0] == '*') {
            std::string word(line.substr(1));
            // FIXME: insert word into dictionary.
        } else {
            abort();
        }
    }
}

@end

int main(int /*argc*/, char* /*argv*/[]) {
    ScopedAutoReleasePool pool;
    
    // Pretend to be ispell in a new thread, so we can enter the normal event
    // loop and prevent Spin Control from mistakenly diagnosing us as a hung
    // GUI application.
    [NSThread detachNewThreadSelector:@selector(ispellRunLoop:)
                             toTarget:[Ispell class]
                           withObject:nil];
    
    [[NSApplication sharedApplication] run];
}
