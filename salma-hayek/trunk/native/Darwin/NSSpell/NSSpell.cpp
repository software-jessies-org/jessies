#include <iostream>
#include <string>

extern "C" void NSSpellChecker_showSuggestions(const char* word);
extern "C" bool NSSpellChecker_isCorrect(const char* word);

int main(int argc, char* argv[]) {
    std::ostream& os(std::cout);
    os << "@(#) International Ispell 3.1.20 (but really NSSpellChecker)\n";

    while (true) {
        std::string line;
	getline(std::cin, line);
        if (line.length() == 0) {
            // We're done.
            return 0;
        } else if (line == "!") {
            // set terse mode; ignore.
        } else if (line[0] == '^') {
            std::string word(line.substr(1));
            if (NSSpellChecker_isCorrect(word.c_str()) == false) {
                NSSpellChecker_showSuggestions(word.c_str());
            }
            os << std::endl;
        } else if (line[0] == '*') {
            std::string word(line.substr(1));
            // FIXME: insert word into dictionary.
        } else {
            os << "eh? " << line << std::endl;
        }
    }
}
