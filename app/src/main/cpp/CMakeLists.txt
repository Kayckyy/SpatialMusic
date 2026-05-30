cmake_minimum_required(VERSION 3.22)
project(openmind)

set(CMAKE_CXX_STANDARD 17)

# Oboe via prefab (Android Studio 4.1+)
find_package(oboe REQUIRED CONFIG)

add_library(openmind SHARED
    ConvolutionEngine.cpp
    IrLoader.cpp
    AudioPlayer.cpp
    # JNI bridge — próximo arquivo
    # OpenMindJni.cpp
)

target_link_libraries(openmind
    oboe::oboe
    android
    log
)

# otimizações ARM NEON (Moto E40 é aarch64)
target_compile_options(openmind PRIVATE
    -O3
    -ffast-math
    -march=armv8-a
)
