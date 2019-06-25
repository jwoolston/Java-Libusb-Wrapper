# For more information about using CMake with Android Studio, read the
# documentation: https://d.android.com/studio/projects/add-native-code.html

# Sets the minimum version of CMake required to build the native library.

cmake_minimum_required(VERSION 3.4.1)

MESSAGE(STATUS "CMAKE_SYSTEM_NAME is ${CMAKE_SYSTEM_NAME}")

find_package(JNI)

SET(CMAKE_CXX_STANDARD 11)

# Creates and names a library, sets it as either STATIC
# or SHARED, and provides the relative paths to its source code.
# You can define multiple libraries, and CMake builds them for you.
# Gradle automatically packages shared libraries with your APK.

add_definitions(-DJNI_WRAPPER)

set(includeDIRS
        ../library/libusb/libusb
        ../library/libusb/libusb/os
        ../library/jni
        ${JAVA_INCLUDE_PATH})

if(WIN32)
    set(includeDIRS
            ${includeDIRS}
            ${JAVA_INCLUDE_PATH}/Win32
            ../library/libusb/msvc)
endif(WIN32)
if(CMAKE_SYSTEM_NAME STREQUAL "Linux")
    set(includeDIRS
            ${includeDIRS}
            ${JAVA_INCLUDE_PATH}/linux)
endif(CMAKE_SYSTEM_NAME STREQUAL "Linux")

include_directories(${includeDIRS})

set(POSIX_POLL_SRC
        ../library/libusb/libusb/os/poll_posix.h
        ../library/libusb/libusb/os/poll_posix.c)

set(POSIX_THREADS_SRC
        ../library/libusb/libusb/os/threads_posix.h
        ../library/libusb/libusb/os/threads_posix.c)

set(WINDOWS_POLL_SRC
        ../library/libusb/libusb/os/poll_windows.h
        ../library/libusb/libusb/os/poll_windows.c)

set(WINDOWS_THREADS_SRC
        ../library/libusb/libusb/os/threads_windows.h
        ../library/libusb/libusb/os/threads_windows.c)

set(LINUX_USBFS_SRC
        ../library/libusb/libusb/os/linux_usbfs.h
        ../library/libusb/libusb/os/linux_usbfs.c)

set(DARWIN_USB_SRC
        ../library/libusb/libusb/os/darwin_usb.h
        ../library/libusb/libusb/os/darwin_usb.c)

set(OPENBSD_USB_SRC
        ../library/libusb/libusb/os/openbsd_usb.c)

set(NETBSD_USB_SRC
        ../library/libusb/libusb/os/netbsd_usb.c)

set(SUNOS_USB_SRC
        ../library/libusb/libusb/os/sunos_usb.c
        ../library/libusb/libusb/os/sunos_usb.h)

set(WINDOWS_USB_SRC
        ../library/libusb/libusb/libusb-1.0.def
        ../library/libusb/libusb/libusb-1.0.rc
        ../library/libusb/libusb/os/windows_common.h
        ../library/libusb/libusb/os/windows_nt_common.h
        ../library/libusb/libusb/os/windows_nt_common.c
        ../library/libusb/libusb/os/windows_nt_shared_types.h
        ../library/libusb/libusb/os/windows_usbdk.h
        ../library/libusb/libusb/os/windows_usbdk.c
        ../library/libusb/libusb/os/windows_winusb.h
        ../library/libusb/libusb/os/windows_winusb.c)

set(WINCE_USB_SRC
        ../library/libusb/libusb/os/wince_usb.h
        ../library/libusb/libusb/os/wince_usb.c)

set(HAIKU_USB_SRC
        ../library/libusb/libusb/os/haiku_usb.h
        ../library/libusb/libusb/os/haiku_usb_backend.cpp
        ../library/libusb/libusb/os/haiku_usb_raw.h
        ../library/libusb/libusb/os/haiku_usb_raw.cpp
        ../library/libusb/libusb/os/haiku_pollfs.cpp)

set(EXTRA_DIST
        ${POSIX_POLL_SRC}
        ${POSIX_THREADS_SRC}
        ${WINDOWS_POLL_SRC}
        ${WINDOWS_THREADS_SRC}
        ${LINUX_USBFS_SRC}
        ${DARWIN_USB_SRC}
        ${OPENBSD_USB_SRC}
        ${NETBSD_USB_SRC}
        ${WINDOWS_USB_SRC}
        ${WINCE_USB_SRC}
        ${HAIKU_USB_SRC}
        ../library/libusb/libusb/os/linux_udev.c
        ../library/libusb/libusb/os/linux_netlink.c)

# Defines the source code for the library
set(libusb_SRCS
        ../library/libusb/libusb/core.c
        ../library/libusb/libusb/descriptor.c
        ../library/libusb/libusb/hotplug.c
        ../library/libusb/libusb/io.c
        ../library/libusb/libusb/sync.c
        ../library/libusb/libusb/strerror.c)

if(CMAKE_SYSTEM_NAME STREQUAL "Linux")
    if(USE_UDEV)
        set(OS_SRC
                ${LINUX_USBFS_SRC}
                ${POSIX_POLL_SRC}
                ${POSIX_THREADS_SRC}
                ../library/libusb/libusb/os/linux_udev.c)
    else(USE_UDEV)
        set(OS_SRC
                ${LINUX_USBFS_SRC}
                ${POSIX_POLL_SRC}
                ${POSIX_THREADS_SRC}
                ../library/libusb/libusb/os/linux_netlink.c)
    endif(USE_UDEV)
    set(OS_SRC ${OS_SRC} ../library/libusb/config.h.in)
endif(CMAKE_SYSTEM_NAME STREQUAL "Linux")
if(CMAKE_SYSTEM_NAME STREQUAL "Darwin")
    set(OS_SRC
            ${DARWIN_USB_SRC}
            ${POSIX_POLL_SRC}
            ${POSIX_THREADS_SRC}
            ../library/libusb/config.h.in)
    set(CMAKE_C_FLAGS "${CMAKE_C_FLAGS} -no-cpp-precomp")
endif(CMAKE_SYSTEM_NAME STREQUAL "Darwin")
if(WIN32)
    set(OS_SRC
            ${WINDOWS_USB_SRC}
            ${WINDOWS_POLL_SRC}
            ${WINDOWS_THREADS_SRC})
endif(WIN32)

add_library(
    # Sets the name of the library.
    wrapper_libusb

    # Sets the library as a shared library.
    SHARED

    # Provides a relative path to your source file(s).
        ../library/jni/async_transfer.c
        ../library/jni/async_usb_thread.c
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
        ${OS_SRC}
)