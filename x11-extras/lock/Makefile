SRCS:=lock.cpp
lock: Makefile $(SRCS)
	$(CXX) -Wall -Werror -o $@ $(SRCS) `pkg-config --cflags --libs x11 xft` -lcrypt
