#import <AppKit/AppKit.h>
#import <iostream>
#import "ScopedAutoReleasePool.h"

int main(int argCount, char* args[]) {
  if (--argCount != 2) {
    std::cerr << "Usage: " << args[0] << " <title> <message>" << std::endl;
    exit(1);
  }
  
  ScopedAutoReleasePool pool;
  [NSApplication sharedApplication];
  NSString* titleString = [NSString stringWithUTF8String:args[1]];
  NSString* messageString = [NSString stringWithUTF8String:args[2]];
  return NSRunInformationalAlertPanel(titleString, messageString, 0, 0, 0);
}
