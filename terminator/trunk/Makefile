PROJECT_NAME=terminator

.PHONY: default
default: build build.pty

BINDIST_FILES += doc pty terminator

# Variables should come before the include but rules (apart from the default
# one) should come afterwards.
include ../salma-hayek/java.make

DIST_SCP_DESTINATION=locke:terminator

.PHONY: build.pty
build.pty:
	@$(MAKE) -C pty

bindist: build.pty
