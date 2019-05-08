cmake_minimum_required(VERSION 3.4.1)
set(APP_PLATFORM "android-19")
include(${CMAKE_CURRENT_SOURCE_DIR}/functions.txt)

check_include_folders()
      
include_directories($ENV{JAVA_HOME}/include)
include_directories($ENV{JAVA_HOME}/include/darwin)

find_package(PythonLibs)
include_directories($ENV{ANDROID_TOOLCHAIN}/include/python2.7)

add_library(python_bridge SHARED python_bridge.c)

target_link_libraries(python_bridge python2.7)
