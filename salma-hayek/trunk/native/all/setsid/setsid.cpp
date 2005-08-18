#include <iostream>
#include <sstream>
#include <stdio.h>
#include <stdlib.h>
#include <unistd.h>
 
static void ensureCallerIsProcessGroupLeader() {
  if (getpid() == getpgrp()) {
    return;
  }

  if (setpgid(0, 0) < 0) {
    perror("setpgid(0, 0)");
    exit(EXIT_FAILURE);
  }
}
 
int main(int, char* argv[]) {
  if (*++argv == 0) {
    std::cerr << "usage: setsid PROGRAM [ARGUMENTS...]" << std::endl;
    exit(EXIT_FAILURE);
  }

  ensureCallerIsProcessGroupLeader();
  execvp(*argv, argv);

  // execvp(3) failed.
  std::ostringstream oss;
  oss << "execvp(\"" << *argv << "\")";
  perror(oss.str().c_str());
  exit(EXIT_FAILURE);
}
