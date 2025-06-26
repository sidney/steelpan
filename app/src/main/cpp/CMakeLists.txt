cmake_minimum_required(VERSION 3.18.1)
project("steelpan")

find_library(log-lib log)
find_library(aaudio-lib aaudio)

add_library(steelpan SHARED steelpan.cpp)

target_link_libraries(steelpan
                      ${log-lib}
                      ${aaudio-lib})
