#ifndef CHOMP_H_included
#define CHOMP_H_included

#include <string>

inline std::string chomp(const std::string& input) {
    if (input.empty() == false && input[input.size() - 1] == '\n') {
        return input.substr(0, input.size() - 1);
    }
    return input;
}

#endif
