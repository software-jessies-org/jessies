PROJECT_NAME=terminator

.PHONY: default
default: build build.pty

BINDIST_FILES += doc pty terminator

# Variables should come before the include but rules (apart from the default
# one) should come afterwards.
# Variables used on either side of the colon in rules are evaluated on the first
# pass.  By the time you get to the other side of the include, you're too late
# to override them.
# ?= assigns default values if the variable hasn't already been set.
include ../salma-hayek/java.make

.PHONY: build.pty
build.pty:
	@$(MAKE) -C pty

bindist: build.pty
