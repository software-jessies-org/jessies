#ifndef REPORT_FATAL_ERROR_VIA_GUI_H_included
#define REPORT_FATAL_ERROR_VIA_GUI_H_included

#ifdef __APPLE__
#include "ScopedAutoReleasePool.h"
#include <Cocoa/Cocoa.h>
#endif

#if defined(__CYGWIN__) || defined(__MINGW32__)
#define USE_MESSAGE_BOX 1
#include <windows.h>
#else
#define USE_MESSAGE_BOX 0
#endif

#include <iostream>
#include <sstream>

inline void reportFatalErrorViaGui(const std::string& programName, const std::string& applicationMessage, std::string supportAddress) {
    if (supportAddress.empty()) {
        supportAddress = "jessies-software@googlegroups.com";
    }
    std::ostringstream os;
    os << applicationMessage;
#if USE_MESSAGE_BOX
    os << std::endl;
    os << "Please copy this message to the clipboard with Ctrl-C and mail it to " << supportAddress << ".";
    os << std::endl;
    os << "(Windows won't let you select the text but Ctrl-C works anyway.)";
    os << std::endl;
#endif
    std::string platformMessage(os.str());
#if USE_MESSAGE_BOX
    MessageBox(GetActiveWindow(), platformMessage.c_str(), programName.c_str(), MB_OK);
#elif defined(__APPLE__)
    ScopedAutoReleasePool pool;
    [NSApplication sharedApplication];
    NSRunInformationalAlertPanel([NSString stringWithUTF8String:programName.c_str()], [NSString stringWithUTF8String:platformMessage.c_str()], nil, nil, nil);
#else
    (void)programName;
#endif
    std::cerr << platformMessage;
}

inline void reportFatalErrorViaGui(const std::string& programName, const std::string& applicationMessage) {
    reportFatalErrorViaGui(programName, applicationMessage, "");
}

#endif
