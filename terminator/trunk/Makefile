PROJECT_NAME=TerminatorN

.PHONY: default
default: build build.pty

BINDIST_FILES += pty terminator

# Variables should come before the include but rules (apart from the default one)
# should come afterwards.
include ../salma-hayek/java.make

.PHONY: build.pty
build.pty:
	@$(MAKE) -C pty

bindist: build.pty
