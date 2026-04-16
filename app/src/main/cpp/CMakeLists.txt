cmake_minimum_required(VERSION 3.10)
project(native_alac)

add_library( # Sets the name of the library.
             native_alac
             SHARED
             native_alac.c )

find_library( # Sets the name of the path variable.
              log-lib
              log )

target_link_libraries( # Specifies the target library.
                       native_alac
                       ${log-lib} )
