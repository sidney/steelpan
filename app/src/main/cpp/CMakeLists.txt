cmake_minimum_required(VERSION 3.22.1)
project("steelpan")

# Find required libraries
find_library(log-lib log)
find_library(aaudio-lib aaudio)

# Create shared library
add_library(steelpan SHARED steelpan.cpp)

# Link libraries
target_link_libraries(steelpan
    ${log-lib}
    ${aaudio-lib})

# Set C++ standard
set_target_properties(steelpan PROPERTIES
    CXX_STANDARD 17
    CXX_STANDARD_REQUIRED ON)
