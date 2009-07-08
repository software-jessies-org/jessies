#ifndef TO_STRING_H_included
#define TO_STRING_H_included

#include <sstream>
#include <stdexcept>
#include <string>

// Based on code by Kevlin Henney, shown in "Exceptional C++ Style".
template <typename T>
inline std::string toString(const T& value) {
  std::stringstream interpreter;
  if (!(interpreter << value)) {
    throw std::runtime_error("bad lexical cast");
  }
  std::string result = interpreter.str();
  return result;
}

#endif
