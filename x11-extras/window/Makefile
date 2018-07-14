.PHONY: all
all: window wselect

window_SRCS:=window.cpp
window: Makefile $(window_SRCS)
	@$(CXX) -Wall -Werror -o $@ $(window_SRCS) -lX11

wselect_SRCS:=wselect.cpp
wselect: Makefile $(wselect_SRCS)
	@$(CXX) -Wall -Werror -o $@ $(wselect_SRCS) -lX11
