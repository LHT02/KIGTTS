if(NOT DEFINED ROOT)
    message(FATAL_ERROR "ROOT not set for patch_aec3.cmake")
endif()

string(REPLACE "\"" "" ROOT "${ROOT}")
set(CMAKELISTS "${ROOT}/CMakeLists.txt")
if(NOT EXISTS "${CMAKELISTS}")
    message(FATAL_ERROR "CMakeLists.txt not found at ${CMAKELISTS}")
endif()

file(READ "${CMAKELISTS}" CONTENTS)

# Drop -m64 (invalid for Android toolchains).
string(REPLACE "add_compile_options(-Wall -fPIC -Wno-deprecated -m64 -fexceptions)"
               "add_compile_options(-Wall -fPIC -Wno-deprecated -fexceptions)"
               CONTENTS "${CONTENTS}")

# Use C++17 to avoid std::result_of removal in C++20.
string(REPLACE "set(CMAKE_CXX_STANDARD 20)"
               "set(CMAKE_CXX_STANDARD 17)"
               CONTENTS "${CONTENTS}")

# Avoid setting WEBRTC_LINUX on Android.
string(REPLACE "elseif(UNIX AND NOT APPLE)"
               "elseif(UNIX AND NOT APPLE AND NOT ANDROID)"
               CONTENTS "${CONTENTS}")

# Exclude SSE resampler source on Android.
string(REPLACE "list(FILTER AECM_SRC EXCLUDE REGEX \".*unittest.cc$\")"
               "list(FILTER AECM_SRC EXCLUDE REGEX \".*unittest.cc$\")\nlist(FILTER AECM_SRC EXCLUDE REGEX \".*sinc_resampler_sse.cc$\")\nlist(FILTER AECM_SRC EXCLUDE REGEX \".*cpu_features_android.c$\")"
               CONTENTS "${CONTENTS}")

# Fix accidental leading backslash before Windows drive letter.
string(REGEX REPLACE "\\\\([A-Za-z]:/)" "\\1" CONTENTS "${CONTENTS}")

# Drop SSE2/MIPS sources that won't build for Android.
string(REGEX REPLACE "\n[^\n]*ooura_fft_sse2\\.cc[^\n]*" "" CONTENTS "${CONTENTS}")
string(REGEX REPLACE "\n[^\n]*ooura_fft_mips\\.cc[^\n]*" "" CONTENTS "${CONTENTS}")

# Ensure Android definitions exist.
if(NOT CONTENTS MATCHES "WEBRTC_ANDROID")
    set(EXTRA "\nif(ANDROID)\n  add_definitions(-DWEBRTC_POSIX -DWEBRTC_ANDROID)\nendif()\n")
    set(CONTENTS "${CONTENTS}${EXTRA}")
endif()

file(WRITE "${CMAKELISTS}" "${CONTENTS}")

set(PLATFORM_THREAD_FILE "${ROOT}/base/rtc_base/platform_thread_types.cc")
if(EXISTS "${PLATFORM_THREAD_FILE}")
    file(READ "${PLATFORM_THREAD_FILE}" THREAD_CONTENTS)
    string(REPLACE "#if defined(WEBRTC_LINUX)\n#include <sys/prctl.h>\n#include <sys/syscall.h>\n#endif"
                   "#if defined(WEBRTC_LINUX) || defined(WEBRTC_ANDROID)\n#include <sys/prctl.h>\n#endif\n#if defined(WEBRTC_LINUX)\n#include <sys/syscall.h>\n#endif"
                   THREAD_CONTENTS "${THREAD_CONTENTS}")
    file(WRITE "${PLATFORM_THREAD_FILE}" "${THREAD_CONTENTS}")
endif()
