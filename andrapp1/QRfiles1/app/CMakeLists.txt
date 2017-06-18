# For more information about using CMake with Android Studio, read the
# documentation: https://d.android.com/studio/projects/add-native-code.html

# Sets the minimum version of CMake required to build the native library.

cmake_minimum_required(VERSION 3.4.1)

#copy prebuilt native libs

configure_file(../../../androidarchs/${ANDROID_ABI}/libfec.so ${CMAKE_LIBRARY_OUTPUT_DIRECTORY}/libfec.so COPYONLY)
configure_file(../../../androidarchs/${ANDROID_ABI}/libqrencoder_wrapper.so ${CMAKE_LIBRARY_OUTPUT_DIRECTORY}/libqrencoder_wrapper.so COPYONLY)
configure_file(../../../androidarchs/${ANDROID_ABI}/libRSencoder.so ${CMAKE_LIBRARY_OUTPUT_DIRECTORY}/libRSencoder.so COPYONLY)
configure_file(../../../androidarchs/${ANDROID_ABI}/libRSdecoder.so ${CMAKE_LIBRARY_OUTPUT_DIRECTORY}/libRSdecoder.so COPYONLY)
configure_file(../../../androidarchs/${ANDROID_ABI}/libRSdecAPI.so ${CMAKE_LIBRARY_OUTPUT_DIRECTORY}/libRSdecAPI.so COPYONLY)
configure_file(../../../androidarchs/${ANDROID_ABI}/libRSencAPI.so ${CMAKE_LIBRARY_OUTPUT_DIRECTORY}/libRSencAPI.so COPYONLY)

include_directories(../../../)

link_directories(${CMAKE_LIBRARY_OUTPUT_DIRECTORY})

# Creates and names a library, sets it as either STATIC
# or SHARED, and provides the relative paths to its source code.
# You can define multiple libraries, and CMake builds them for you.
# Gradle automatically packages shared libraries with your APK.

#add_library(RSdecAPI
#            SHARED
#            ${CMAKE_LIBRARY_OUTPUT_DIRECTORY}/libRSdecAPI.so)

#add_library(fec
#            SHARED
#            ${CMAKE_LIBRARY_OUTPUT_DIRECTORY}/libfec.so)

#add_library(encoder_wrapper
#            SHARED
#            ${CMAKE_LIBRARY_OUTPUT_DIRECTORY}/libqrencoder_wrapper.so)

#add_library(RSencoder
#            SHARED
#            ${CMAKE_LIBRARY_OUTPUT_DIRECTORY}/libRSencoder.so)

#add_library(RSdecoder
#            SHARED
#            ${CMAKE_LIBRARY_OUTPUT_DIRECTORY}/libRSdecoder.so)

#set_target_properties(RSdecAPI PROPERTIES LINKER_LANGUAGE CXX)
#set_target_properties(fec PROPERTIES LINKER_LANGUAGE CXX)
#set_target_properties(encoder_wrapper PROPERTIES LINKER_LANGUAGE CXX)
#set_target_properties(RSencoder PROPERTIES LINKER_LANGUAGE CXX)
#set_target_properties(RSdecoder PROPERTIES LINKER_LANGUAGE CXX)

add_library( # Sets the name of the library.
             native-lib

             # Sets the library as a shared library.
             SHARED

             # Provides a relative path to your source file(s).
             src/main/cpp/native-lib.cpp )

# Searches for a specified prebuilt library and stores the path as a
# variable. Because CMake includes system libraries in the search path by
# default, you only need to specify the name of the public NDK library
# you want to add. CMake verifies that the library exists before
# completing its build.

find_library( # Sets the name of the path variable.
              log-lib

              # Specifies the name of the NDK library that
              # you want CMake to locate.
              log )



# Specifies libraries CMake should link to your target library. You
# can link multiple libraries, such as libraries you define in this
# build script, prebuilt third-party libraries, or system libraries.

target_link_libraries( # Specifies the target library.
                       native-lib

                       # Links the target library to the log library
                       # included in the NDK.
                       ${log-lib} )

get_filename_component(ABSOLUTE_PATH0
                       "${CMAKE_CURRENT_LIST_DIR}/../../../androidarchs/${ANDROID_ABI}/libRSencAPI.so"
                       ABSOLUTE)

target_link_libraries(
    native-lib
    ${ABSOLUTE_PATH0})

get_filename_component(ABSOLUTE_PATH
                       "${CMAKE_CURRENT_LIST_DIR}/../../../androidarchs/${ANDROID_ABI}/libRSdecAPI.so"
                       ABSOLUTE)

target_link_libraries(
    native-lib
    ${ABSOLUTE_PATH})

get_filename_component(ABSOLUTE_PATH2
                       "${CMAKE_CURRENT_LIST_DIR}/../../../androidarchs/${ANDROID_ABI}/libfec.so"
                       ABSOLUTE)

target_link_libraries(
    native-lib
    ${ABSOLUTE_PATH2})

get_filename_component(ABSOLUTE_PATH3
                       "${CMAKE_CURRENT_LIST_DIR}/../../../androidarchs/${ANDROID_ABI}/libqrencoder_wrapper.so"
                       ABSOLUTE)

target_link_libraries(
    native-lib
    ${ABSOLUTE_PATH3})

get_filename_component(ABSOLUTE_PATH4
                       "${CMAKE_CURRENT_LIST_DIR}/../../../androidarchs/${ANDROID_ABI}/libRSdecoder.so"
                       ABSOLUTE)

target_link_libraries(
    native-lib
    ${ABSOLUTE_PATH4})

get_filename_component(ABSOLUTE_PATH5
                       "${CMAKE_CURRENT_LIST_DIR}/../../../androidarchs/${ANDROID_ABI}/libRSencoder.so"
                       ABSOLUTE)

target_link_libraries(
    native-lib
    ${ABSOLUTE_PATH5})
