if (NOT DEFINED ROOT)
    message(FATAL_ERROR "ROOT not set for espeak-ng patch")
endif()

string(REPLACE "\"" "" ROOT "${ROOT}")
file(TO_CMAKE_PATH "${ROOT}" ROOT)

set(src_file "${ROOT}/src/CMakeLists.txt")
if (NOT EXISTS "${src_file}")
    file(GLOB children RELATIVE "${ROOT}" "${ROOT}/*")
    foreach(child ${children})
        if (IS_DIRECTORY "${ROOT}/${child}" AND EXISTS "${ROOT}/${child}/src/CMakeLists.txt")
            set(src_file "${ROOT}/${child}/src/CMakeLists.txt")
            break()
        endif()
    endforeach()
endif()
if (NOT EXISTS "${src_file}")
    message(FATAL_ERROR "espeak-ng CMakeLists not found under ${ROOT}")
endif()

file(READ "${src_file}" content)
if (content MATCHES "if \\(USE_SPEECHPLAYER\\)")
    message(STATUS "espeak-ng speechPlayer already gated")
else()
    string(REPLACE
        "add_subdirectory(speechPlayer)\n"
        "if (USE_SPEECHPLAYER)\n  add_subdirectory(speechPlayer)\nendif()\n"
        content
        "${content}"
    )
    file(WRITE "${src_file}" "${content}")
    message(STATUS "espeak-ng speechPlayer gated")
endif()

set(root_cmake "${ROOT}/CMakeLists.txt")
if (EXISTS "${root_cmake}")
    file(READ "${root_cmake}" root_content)
    if (root_content MATCHES "CMAKE_CROSSCOMPILING")
        message(STATUS "espeak-ng data step already gated")
    else()
        string(REPLACE
            "include(cmake/data.cmake)"
            "if (NOT CMAKE_CROSSCOMPILING)\n  include(cmake/data.cmake)\nendif()"
            root_content
            "${root_content}"
        )
        file(WRITE "${root_cmake}" "${root_content}")
        message(STATUS "espeak-ng data step gated for cross-compile")
    endif()
endif()
