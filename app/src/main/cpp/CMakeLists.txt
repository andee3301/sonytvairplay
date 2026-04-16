cmake_minimum_required(VERSION 3.10)
project(native_alac)

set(CMAKE_C_STANDARD 99)

add_library(native_alac SHARED native_alac.c)

# Directory where optional prebuilt ffmpeg native libs are placed by CI or manually
set(PREBUILT_LIBS_DIR "${CMAKE_SOURCE_DIR}/../libs/${ANDROID_ABI}")

set(FFMPEG_LIBS)

foreach(lib avcodec avformat avutil swresample swscale)
  set(libfile "${PREBUILT_LIBS_DIR}/lib${lib}.so")
  if(EXISTS ${libfile})
    add_library(${lib} SHARED IMPORTED)
    set_target_properties(${lib} PROPERTIES IMPORTED_LOCATION ${libfile})
    list(APPEND FFMPEG_LIBS ${lib})
  endif()
endforeach()

find_library(log-lib log)

target_link_libraries(native_alac ${FFMPEG_LIBS} ${log-lib})

