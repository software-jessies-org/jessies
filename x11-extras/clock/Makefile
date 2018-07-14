SRCS:=clock.cpp
clock: Makefile $(SRCS)
	$(CXX) -Wall -Werror -o $@ $(SRCS) `pkg-config --cflags --libs x11 xft`
