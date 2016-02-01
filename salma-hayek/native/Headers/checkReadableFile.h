#ifndef CHECK_READABLE_FILE_H_included
#define CHECK_READABLE_FILE_H_included

#include <stdexcept>
#include <stdio.h>
#include <string>

void checkReadableFile(const char* description, const std::string& fileName) {
    FILE* fp = fopen(fileName.c_str(), "rb");
    if (fp == 0) {
        throw std::runtime_error(std::string("couldn't read ") + description + " from \"" + fileName + "\"");
    } else {
        fclose(fp);
    }
}

#endif
