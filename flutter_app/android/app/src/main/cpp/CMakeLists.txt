cmake_minimum_required(VERSION 3.18)

project(kgtts_espeak LANGUAGES C CXX)

set(CMAKE_CXX_STANDARD 17)
set(CMAKE_CXX_STANDARD_REQUIRED ON)

include(ExternalProject)

set(PIPER_PHONEMIZE_DIR "${CMAKE_CURRENT_LIST_DIR}/piper-phonemize")
set(PIPER_PHONEMIZE_SRC "${PIPER_PHONEMIZE_DIR}/src")

set(ESPEAK_NG_SRC_DIR "${CMAKE_BINARY_DIR}/espeak-ng-build/src/espeak_ng_external")
set(ESPEAK_NG_LIB_DIR "${CMAKE_BINARY_DIR}/espeak-ng-build/src/espeak_ng_external-build/src/libespeak-ng")
set(ESPEAK_NG_INCLUDE_DIR "${ESPEAK_NG_SRC_DIR}/src/include")
set(ESPEAK_NG_URL "https://github.com/rhasspy/espeak-ng/archive/0f65aa301e0d6bae5e172cc74197d32a6182200f.zip")

set(AEC3_SRC_DIR "${CMAKE_BINARY_DIR}/aec3-build/src/aec3_external")
set(AEC3_BUILD_DIR "${CMAKE_BINARY_DIR}/aec3-build/src/aec3_external-build")
set(AEC3_LIB "${AEC3_BUILD_DIR}/libwebrtc_aec3.a")
set(AEC3_URL "https://github.com/khomin/AEC3_cmake/archive/d72e24514de16e92eee088abc8127dd2d86cb32c.zip")

ExternalProject_Add(
    espeak_ng_external
    PREFIX "${CMAKE_BINARY_DIR}/espeak-ng-build"
    URL "${ESPEAK_NG_URL}"
    SOURCE_DIR "${ESPEAK_NG_SRC_DIR}"
    PATCH_COMMAND
        ${CMAKE_COMMAND} -DROOT="${ESPEAK_NG_SRC_DIR}" -P "${CMAKE_CURRENT_LIST_DIR}/patch_espeak.cmake"
    BUILD_COMMAND
        ${CMAKE_COMMAND} --build . --target espeak-ng
    INSTALL_COMMAND ""
    CMAKE_ARGS
        -DCMAKE_BUILD_TYPE=Release
        -DBUILD_SHARED_LIBS=ON
        -DUSE_ASYNC=OFF
        -DUSE_MBROLA=OFF
        -DUSE_LIBSONIC=OFF
        -DUSE_LIBPCAUDIO=OFF
        -DUSE_KLATT=OFF
        -DUSE_SPEECHPLAYER=OFF
        -DBUILD_TESTING=OFF
        -DENABLE_TESTS=OFF
        -DESPEAK_NG_BUILD_TESTS=OFF
        -DEXTRA_cmn=ON
        -DEXTRA_ru=ON
        -DCMAKE_C_FLAGS=-D_FILE_OFFSET_BITS=64
        -DCMAKE_TOOLCHAIN_FILE=${CMAKE_TOOLCHAIN_FILE}
        -DCMAKE_MAKE_PROGRAM=${CMAKE_MAKE_PROGRAM}
        -DANDROID_ABI=${ANDROID_ABI}
        -DANDROID_PLATFORM=${ANDROID_PLATFORM}
        -DANDROID_STL=c++_shared
        -DANDROID_NDK=${ANDROID_NDK}
    BUILD_BYPRODUCTS "${ESPEAK_NG_LIB_DIR}/libespeak-ng.so"
)

ExternalProject_Add(
    aec3_external
    PREFIX "${CMAKE_BINARY_DIR}/aec3-build"
    URL "${AEC3_URL}"
    SOURCE_DIR "${AEC3_SRC_DIR}"
    PATCH_COMMAND
        ${CMAKE_COMMAND} -DROOT="${AEC3_SRC_DIR}" -P "${CMAKE_CURRENT_LIST_DIR}/patch_aec3.cmake"
    INSTALL_COMMAND ""
    CMAKE_ARGS
        -DCMAKE_BUILD_TYPE=Release
        -DCMAKE_POSITION_INDEPENDENT_CODE=ON
        -DCMAKE_TOOLCHAIN_FILE=${CMAKE_TOOLCHAIN_FILE}
        -DCMAKE_MAKE_PROGRAM=${CMAKE_MAKE_PROGRAM}
        -DANDROID_ABI=${ANDROID_ABI}
        -DANDROID_PLATFORM=${ANDROID_PLATFORM}
        -DANDROID_STL=c++_shared
        -DANDROID_NDK=${ANDROID_NDK}
    BUILD_BYPRODUCTS "${AEC3_LIB}"
)

add_library(
    espeak_jni
    SHARED
    espeak_jni.cpp
    "${PIPER_PHONEMIZE_SRC}/phonemize.cpp"
    "${PIPER_PHONEMIZE_SRC}/shared.cpp"
)

add_dependencies(espeak_jni espeak_ng_external)

add_custom_command(
    TARGET espeak_jni POST_BUILD
    COMMAND ${CMAKE_COMMAND} -E copy_if_different
        "${ESPEAK_NG_LIB_DIR}/libespeak-ng.so"
        "$<TARGET_FILE_DIR:espeak_jni>/libespeak-ng.so"
)

# Also copy espeak-ng to jniLibs so it is always packaged.
set(ESPEAK_NG_JNI_DIR "${CMAKE_CURRENT_LIST_DIR}/../jniLibs/${ANDROID_ABI}")
add_custom_command(
    OUTPUT "${ESPEAK_NG_JNI_DIR}/libespeak-ng.so"
    COMMAND ${CMAKE_COMMAND} -E make_directory "${ESPEAK_NG_JNI_DIR}"
    COMMAND ${CMAKE_COMMAND} -E copy_if_different
        "${ESPEAK_NG_LIB_DIR}/libespeak-ng.so"
        "${ESPEAK_NG_JNI_DIR}/libespeak-ng.so"
    DEPENDS espeak_ng_external
)
add_custom_target(espeak_ng_copy ALL
    DEPENDS "${ESPEAK_NG_JNI_DIR}/libespeak-ng.so"
)
add_dependencies(espeak_jni espeak_ng_copy)

target_include_directories(
    espeak_jni PRIVATE
    "${PIPER_PHONEMIZE_SRC}"
    "${ESPEAK_NG_INCLUDE_DIR}"
)

target_link_directories(
    espeak_jni PRIVATE
    "${ESPEAK_NG_LIB_DIR}"
)

target_link_libraries(
    espeak_jni
    espeak-ng
    log
)

target_compile_options(espeak_jni PRIVATE -fexceptions)

add_library(
    aec3_jni
    SHARED
    aec3_jni.cpp
)

add_dependencies(aec3_jni aec3_external)

target_include_directories(
    aec3_jni PRIVATE
    "${AEC3_SRC_DIR}"
    "${AEC3_SRC_DIR}/audio_processing"
    "${AEC3_SRC_DIR}/audio_processing/include"
    "${AEC3_SRC_DIR}/api"
    "${AEC3_SRC_DIR}/base"
    "${AEC3_SRC_DIR}/base/abseil"
)

target_link_libraries(
    aec3_jni
    "${AEC3_LIB}"
    log
)

target_compile_options(aec3_jni PRIVATE -fexceptions)
