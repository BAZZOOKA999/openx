cmake_minimum_required(VERSION 3.22.1)

project("llama-android")

# Configuration for native llama.cpp Android build
# When building in an Android NDK environment:
# 1. Clone llama.cpp into this directory or configure external project:
#    git clone https://github.com/ggerganov/llama.cpp
# 2. Add add_subdirectory(llama.cpp)
# 3. Target link llama and ggml

set(CMAKE_CXX_STANDARD 17)
set(CMAKE_CXX_STANDARD_REQUIRED ON)

# Source files for JNI bridge
add_library(
    llama-android
    SHARED
    llama-jni.cpp
)

# In full NDK build with llama.cpp submodule:
# target_link_libraries(
#     llama-android
#     llama
#     ggml
#     android
#     log
# )

find_library(log-lib log)
find_library(android-lib android)

target_link_libraries(
    llama-android
    ${log-lib}
    ${android-lib}
)
