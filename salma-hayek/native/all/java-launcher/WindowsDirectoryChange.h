#ifndef WINDOWS_DIRECTORY_CHANGE_H_included
#define WINDOWS_DIRECTORY_CHANGE_H_included

#include <string>

struct WindowsDirectoryChange {
private:
    std::string previousDirectory;
    
public:
    explicit WindowsDirectoryChange(const std::string& targetDirectory);
    ~WindowsDirectoryChange();
};

#endif
