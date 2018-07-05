SRCS:=lock.cpp
lock: Makefile $(SRCS)
	$(CC) -Wall -Werror -o $@ $(SRCS) -lstdc++ -lX11 -lXft `pkg-config --cflags freetype2` -lcrypt
