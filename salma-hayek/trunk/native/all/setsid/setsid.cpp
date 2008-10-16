#include <iostream>
#include <sstream>
#include <stdio.h>
#include <stdlib.h>
#include <unistd.h>

static void ensureCallerIsProcessGroupLeaderAndHasNoControllingTerminal() {
  if (getpid() == getpgrp()) {
    // Thanks to a modification by John Fremlin, Debian's setsid(1) forks in this situation,
    // running the child in the background, which isn't what we'd want.
    // On most platforms, we don't currently run this program as a process group leader.
    // We may be hiding a different implementation of setsid, so have the grace to fail with an informative error message.
    // FIXME: This case always happens on Windows, which is very annoying if this setsid is first on Evergreen's PATH.
    std::cerr << "The software.jessies.org implementation of setsid does not work for process group leaders." << std::endl;
    std::cerr << "We are a process group leader, so that Evergreen's kill with a negative pid will work." << std::endl;
    std::cerr << "But we still have a controlling terminal, so ssh may try to ask for a password." << std::endl;
    std::cerr << "We can't fork and wait because then Evergreen's kill wouldn't kill the right process group." << std::endl;
    return;
    //exit(EXIT_FAILURE);
  }

  if (setsid() < 0) {
    perror("setsid()");
    exit(EXIT_FAILURE);
  }
}

int main(int, char* argv[]) {
  if (*++argv == 0) {
    std::cerr << "usage: setsid PROGRAM [ARGUMENTS...]" << std::endl;
    exit(EXIT_FAILURE);
  }

  // When Evergreen runs CheckInTool and CheckInTool runs a commit that uses ssh and ssh decides to ask for a password,
  // then we don't want the OS to suspend the whole process group with SIGTTIN.
  ensureCallerIsProcessGroupLeaderAndHasNoControllingTerminal();
  execvp(*argv, argv);

  // execvp(3) failed.
  std::ostringstream oss;
  oss << "execvp(\"" << *argv << "\")";
  perror(oss.str().c_str());
  exit(EXIT_FAILURE);
}
