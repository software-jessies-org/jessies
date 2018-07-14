SRCS:=menu.cpp
menu: Makefile $(SRCS)
	$(CXX) -Wall -Werror -o $@ $(SRCS) `pkg-config --cflags --libs x11 xft`
