// log.h
//
// A simple C++ logging facility for generating log messages (to stderr),
// including support for conditional logging and different log levels.
//
// The functionality is generally accessed using a set of macros, which have
// the following form:
//
// LOGI(), LOGW(), LOGE(), LOGF() <= log unconditionally.
// LOGI_IF(cnd), LOGW_IF(cnd), LOGE_IF(cnd), LOGF_IF(cnd) <= log always.
//
// The only thing that really differentiates I, W and E levels (which stand for
// Info, Warning and Error) is that the first character of the log message is
// the corresponding letter.
//
// The F log level additionally terminates the program: the F stands for Fatal.
// Exit code 1 will be used
// In this case, an error code must be provided, which is used as the exit code
// of the program. Be careful to get your condition and errno in the right
// order in the case of LOGF_IF, otherwise you could end up with an
// unconditional but possibly successful exit.
//
// Note that this file was written with the intention of being #included into
// a 1-source-file program (such as menu.cpp, window.cpp etc); if used in a
// project involving multiple compilation units, it should be refactored into
// a header and a source file.

#ifndef LOG_H_included
#define LOG_H_included

#include <iostream>
#include <sstream>

#include <ctime>

#define LOGI() Log("I", __FILE__, __LINE__, 0, true)
#define LOGW() Log("W", __FILE__, __LINE__, 0, true)
#define LOGE() Log("E", __FILE__, __LINE__, 0, true)
#define LOGF() Log("F", __FILE__, __LINE__, 1, true)

#define LOGI_IF(cond) Log("I", __FILE__, __LINE__, 0, cond)
#define LOGW_IF(cond) Log("W", __FILE__, __LINE__, 0, cond)
#define LOGE_IF(cond) Log("E", __FILE__, __LINE__, 0, cond)
#define LOGF_IF(cond) Log("F", __FILE__, __LINE__, 1, cond)

class Log {
 public:
  // Use << Log::Errno(errno) into the Log object in order to pretty-print
  // a system error number.
  class Errno {
   public:
    explicit Errno(int n) : n_(n) {}
    int Num() const { return n_; }

   private:
    int n_;
  };

  // Use << Log::ExitCode(code) into the log object in order to change the exit
  // code from the default value of 1.
  // Note: if you use this on a non-fatal Log (eg one created with the LOGI
  // macro), it *will* effectively turn the log object into a fatal one, which
  // will exit the program with the provided exit code. However, the log
  // statement will still start with an 'I'. I do not recommend this.
  class ExitCode {
   public:
    explicit ExitCode(int c) : c_(c) {}
    int Code() const { return c_; }

   private:
    int c_;
  };

  Log& operator<<(const Errno& e) {
    const char* es = strerror(e.Num());
    buf_ << "errno=" << e.Num() << " (" << es << ")";
    return *this;
  }

  Log& operator<<(const ExitCode& e) {
    exit_code_ = e.Code();
    return *this;
  }

  template <typename T>
  Log& operator<<(T t) {
    buf_ << t;
    return *this;
  }

  // Don't use this constructor; use the macros below instead.
  Log(const char* level,
      const char* file,
      const int line,
      int exit_code,
      bool do_it)
      : exit_code_(exit_code), do_it_(do_it) {
    if (!do_it_) {
      return;
    }
    time_t t = time(nullptr);
    struct tm* tm = localtime(&t);
    char time_buf[100];
    strftime(time_buf, sizeof(time_buf), "%Y-%m-%d %H:%M:%S", tm);
    buf_ << level << " " << time_buf << " " << file << ":" << line << ": ";
  }

  ~Log() {
    if (!do_it_) {
      return;
    }
    std::cerr << buf_.str() << "\n";
    if (exit_code_) {
      exit(exit_code_);
    }
  }

 private:
  int errnum_;
  int exit_code_;
  bool do_it_;
  std::ostringstream buf_;
};

#endif
