.PHONY: all
all: window wselect

window_SRCS:=window.cpp
window: Makefile $(window_SRCS)
	@$(CC) -Wall -Werror -o $@ $(window_SRCS) -lstdc++ -lX11

wselect_SRCS:=wselect.cpp
wselect: Makefile $(wselect_SRCS)
	@$(CC) -Wall -Werror -o $@ $(wselect_SRCS) -lstdc++ -lX11
