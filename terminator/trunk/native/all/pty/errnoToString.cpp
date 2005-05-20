#include "errnoToString.h"

#include <errno.h>
#include <sstream>
#include <string.h>

int gnuCompatibleStrerror(int (*strerror_r)(int, char*, size_t), int errorNumber, char* messageBuffer, size_t bufferSize) {
  return strerror_r(errorNumber, messageBuffer, bufferSize);
}

int gnuCompatibleStrerror(char* (*strerror_r)(int, char*, size_t), int errorNumber, char* messageBuffer, size_t bufferSize) {
  const char* destination = strerror_r(errorNumber, messageBuffer, bufferSize);
  // strcpy doesn't support copying over oneself.
  if (destination != messageBuffer) {
    // The string is guaranteed to be zero-terminated.
    strcpy(messageBuffer, destination);
  }
  return 0;
}

std::string errnoToString(int errorNumber) {
  char messageBuffer [1024];
  if (gnuCompatibleStrerror(&strerror_r, errorNumber, &messageBuffer[0], sizeof(messageBuffer)) == -1) {
    int decodingError = errno;
    std::ostringstream os;
    switch (decodingError) {
    case EINVAL:
      os << "The value " << errorNumber << " is not a valid error number.";
      break;
    case ERANGE:
      os << sizeof(messageBuffer) << " bytes was not enough to contain the error description string for error number " << errorNumber << ".";
      break;
    default:
      os << "Decoding error number " << errorNumber << " produced error " << decodingError << ".";
      break;
    }
    return os.str();
  } else {
    return messageBuffer;
  }
}
