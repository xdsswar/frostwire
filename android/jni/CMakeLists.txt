cmake_minimum_required(VERSION 3.4.1)
set(APP_PLATFORM "android-19")
include(${CMAKE_CURRENT_SOURCE_DIR}/functions.txt)

check_include_folders()

include_directories($ENV{JAVA_HOME}/include)
include_directories($ENV{JAVA_HOME}/include/darwin)

include_directories($ENV{ANDROID_TOOLCHAIN}/include/python2.7)
set(CMAKE_INSTALL_RPATH_USE_LINK_PATH TRUE)
link_directories($ENV{ANDROID_TOOLCHAIN}/lib64)
link_directories($ENV{ANDROID_TOOLCHAIN}/lib)

add_library(python_bridge SHARED python_bridge.c)
target_link_libraries(python_bridge dl python2.7)

add_executable(python_test python_test.c)
target_link_libraries(python_test python_bridge)
