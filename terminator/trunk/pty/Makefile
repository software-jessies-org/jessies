SRCS=$(wildcard *.cpp)
BINARY=$(shell echo pty-`uname`-`arch`)

$(BINARY):	$(SRCS)
	g++ -g -W -Wall -o $(BINARY) $(SRCS)

.PHONY: clean
clean:
	rm -f *.o pty-`uname`-`arch`
