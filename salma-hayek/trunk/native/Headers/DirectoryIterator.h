#ifndef DIRECTORY_ITERATOR_H_included
#define DIRECTORY_ITERATOR_H_included

#include "toString.h"
#include "unix_exception.h"

#include <dirent.h>
#include <errno.h>
#include <string>
#include <sys/types.h>

struct DirectoryEntry {
private:
  std::string name;
  
public:
  std::string getName() const {
    return name;
  }
  
  DirectoryEntry() {
  }
  DirectoryEntry(const dirent* cStyleEntry)
  : name(cStyleEntry->d_name) {
  }
};

struct DirectoryIterator {
private:
  DIR* handle;
  bool eof;
  DirectoryEntry entry;
  
private:
  void readOneEntry() {
    const dirent* cStyleEntry = readdir(handle);
    if (cStyleEntry != 0) {
      entry = cStyleEntry;
      return;
    }
    eof = true;
    if (errno != 0) {
      throw unix_exception(std::string("readdir(") + toString(handle) + ")");
    }
  }
  
public:
  DirectoryIterator(const char* directoryName)
  : handle(opendir(directoryName)), eof(false) {
    if (handle == 0) {
      throw unix_exception(std::string("opendir(") + directoryName + ")");
    }
    readOneEntry();
  }
  ~DirectoryIterator() {
    closedir(handle);
  }
  
  bool isValid() const {
    return eof == false;
  }
  
  const DirectoryEntry* operator->() const {
    return &entry;
  }
  
  DirectoryIterator& operator++() {
    readOneEntry();
    return *this;
  }
};

#endif
