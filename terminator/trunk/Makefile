PROJECT_NAME=terminator

BINDIST_FILES += doc pty terminator
SUBDIRS += pty

# Variables should come before the include but rules (apart from the default
# one) should come afterwards.
# Variables used on either side of the colon in rules are evaluated on the first
# pass.  By the time you get to the other side of the include, you're too late
# to override them.
# ?= assigns default values if the variable hasn't already been set.
include ../salma-hayek/java.make
