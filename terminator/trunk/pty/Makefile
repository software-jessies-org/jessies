SRCS=$(wildcard *.cpp)

.PHONY: pty
pty:	$(SRCS)
	g++ -g -W -Wall -o pty-`uname`-`arch` $(SRCS)

clean:
	rm -f *.o pty-`uname`-`arch`
