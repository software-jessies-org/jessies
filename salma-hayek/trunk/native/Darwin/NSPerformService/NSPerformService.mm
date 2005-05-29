#import <Cocoa/Cocoa.h>
#import <iostream>

void doService(const std::string& service, const std::string& text) {
  NSPasteboard* pb = [NSPasteboard pasteboardWithUniqueName];
  [pb declareTypes:[NSArray arrayWithObject:NSStringPboardType]
             owner:nil];
  [pb setString:[NSString stringWithUTF8String:text.c_str()]
        forType:NSStringPboardType];

  NSString* serviceString = [NSString stringWithUTF8String:service.c_str()];
  BOOL success = NSPerformService(serviceString, pb);
  if (success == NO) {
    NSLog(@"NSPerformService failed.");
    exit(1);
  }
  //NSLog(@"pb='%@'", [pb stringForType:NSStringPboardType]);
}

static void usage(std::ostream& os, const std::string& name) {
  os << "Usage: " << name << " <service> <text>" << std::endl;
  os << "Examples:" << std::endl;
  os << "       " << name << " Spotlight blackberry" << std::endl;
  os << "       " << name << " 'Mail/Send To' root@localhost" << std::endl;
}

int main(int argCount, char* args[]) {
  if (--argCount != 2) {
    usage(std::cerr, args[0]);
    exit(1);
  }

  NSAutoreleasePool* pool = [[NSAutoreleasePool alloc] init];
  [NSApplication sharedApplication];
  doService(args[1], args[2]);
  [pool release];
  return 0;
}
