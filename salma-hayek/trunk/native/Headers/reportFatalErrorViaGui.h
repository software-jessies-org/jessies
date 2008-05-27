#ifndef REPORT_FATAL_ERROR_VIA_GUI_H_included
#define REPORT_FATAL_ERROR_VIA_GUI_H_included

#ifdef __APPLE__
#include "ScopedAutoReleasePool.h"
#include <Cocoa/Cocoa.h>
#endif

#ifdef __CYGWIN__
#include <windows.h>
#endif

#include <iostream>
#include <sstream>

void reportFatalErrorViaGui(const std::string& programName, const std::string& applicationMessage) {
    std::ostringstream os;
#if defined(__CYGWIN__)
    os << "Please copy this message to the clipboard with Ctrl-C and mail it to software@jessies.org.";
    os << std::endl;
    os << "(Windows won't let you select the text but Ctrl-C works anyway.)";
    os << std::endl;
    os << std::endl;
#endif
    os << applicationMessage;
    std::string platformMessage(os.str());
#if defined(__CYGWIN__)
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

#endif
