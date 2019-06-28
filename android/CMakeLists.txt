# For more information about using CMake with Android Studio, read the
# documentation: https://d.android.com/studio/projects/add-native-code.html

# Sets the minimum version of CMake required to build the native library.

cmake_minimum_required(VERSION 3.4.1)

# Creates and names a library, sets it as either STATIC
# or SHARED, and provides the relative paths to its source code.
# You can define multiple libraries, and CMake builds them for you.
# Gradle automatically packages shared libraries with your APK.

add_definitions(-DJNI_WRAPPER)

include_directories(
        ../library/libusb/android
        ../library/libusb/libusb
        ../library/libusb/libusb/os
        ../library/jni)

# Defines the source code for the library
set(libusb_SRCS
        ../library/libusb/libusb/core.c
        ../library/libusb/libusb/descriptor.c
        ../library/libusb/libusb/hotplug.c
        ../library/libusb/libusb/io.c
        ../library/libusb/libusb/sync.c
        ../library/libusb/libusb/strerror.c
        ../library/libusb/libusb/os/linux_usbfs.c
        ../library/libusb/libusb/os/poll_posix.c
        ../library/libusb/libusb/os/threads_posix.c
        ../library/libusb/libusb/os/linux_netlink.c
)

add_library(
    # Sets the name of the library.
    wrapper_libusb

    # Sets the library as a shared library.
    SHARED

    # Provides a relative path to your source file(s).
        ../library/jni/async_transfer.c
        ../library/jni/async_usb_thread.c
        ../library/jni/device_list.c
        ../library/jni/common.h
        ../library/jni/isochronous_async_transfer.c
        ../library/jni/libusb_device_descriptor.c
        ../library/jni/libusb_error_enum.c
        ../library/jni/logging.c
        ../library/jni/usb_configuration.c
        ../library/jni/usb_device.c
        ../library/jni/usb_device_connection.c
        ../library/jni/usb_interface.c
        ../library/jni/usb_manager.c
    ${libusb_SRCS}
)

#set_target_properties(wrapper_libusb PROPERTIES LINKER_LANGUAGE C)

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
                       wrapper_libusb

                       # Links the target library to the log library
                       # included in the NDK.
                       ${log-lib} )