
# cmake . && make

cmake_minimum_required(VERSION 3.6)

project(x11-extras)

find_package(X11 REQUIRED)
find_package(Freetype REQUIRED)
include_directories(${X11_INCLUDE_DIR} ${FREETYPE_INCLUDE_DIRS} ${X11_Xrandr_INCLUDE_PATH})
link_libraries(${X11_LIBRARIES} ${X11_Xft_LIB} ${FREETYPE_LIBRARIES} ${X11_Xrandr_LIB})

add_executable(clock clock.cpp)
#add_executable(lock lock.cpp)
add_executable(menu menu.cpp)
add_executable(speckeysd speckeysd.cpp)
add_executable(window window.cpp)
add_executable(wselect wselect.cpp)
add_executable(x11-reaper x11-reaper.cpp)
