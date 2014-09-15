#ifndef UPDATE_LOGIN_RECORD_H_included
#define UPDATE_LOGIN_RECORD_H_included

#include <string>
#include <sys/types.h>

void updateLoginRecord(int fd, pid_t pid, const std::string& slavePtyName);

#endif
