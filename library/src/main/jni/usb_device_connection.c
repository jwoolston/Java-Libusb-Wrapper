//
// Created by Jared Woolston (Jared.Woolston@gmail.com)
//

#include <unistd.h>
#include <string.h>
#include "common.h"

#define  LOG_TAG    "UsbDeviceConnection-Native"

JNIEXPORT void JNICALL
Java_com_jwoolston_android_libusb_UsbDeviceConnection_nativeClose(JNIEnv *env, jobject instance, jobject device) {
    struct libusb_device_handle *deviceHandle = (struct libusb_device_handle *) (*env)->GetDirectBufferAddress(env,
                                                                                                               device);
    libusb_close(deviceHandle);
    if (deviceHandle != NULL) {
        free(deviceHandle);
    }
}

JNIEXPORT jbyteArray JNICALL
Java_com_jwoolston_android_libusb_UsbDeviceConnection_nativeGetRawDescriptor(JNIEnv *env, jobject instance, jint fd) {
    char buffer[16384];
    if (fd < 0) return NULL;
    lseek(fd, 0, SEEK_SET);
    int length = read(fd, buffer, sizeof(buffer));
    if (length < 0) return NULL;
    jbyteArray ret = (*env)->NewByteArray(env, length);
    if (ret) {
        jbyte* bytes = (jbyte*)(*env)->GetPrimitiveArrayCritical(env, ret, 0);
        if (bytes) {
            memcpy(bytes, buffer, length);
            (*env)->ReleasePrimitiveArrayCritical(env, ret, bytes, 0);
        }
    }
    return ret;
}

JNIEXPORT jint JNICALL
Java_com_jwoolston_android_libusb_UsbDeviceConnection_nativeClaimInterface(JNIEnv *env, jobject instance,
                                                                           jobject device, jint interfaceID,
                                                                           jboolean force) {
    struct libusb_device_handle *deviceHandle = (struct libusb_device_handle *) (*env)->GetDirectBufferAddress(env,
                                                                                                               device);
    jint ret = libusb_claim_interface(deviceHandle, interfaceID);
    if (ret == LIBUSB_ERROR_BUSY && force) {
        libusb_detach_kernel_driver(deviceHandle, interfaceID);
        ret = libusb_claim_interface(deviceHandle, interfaceID);
    }
    return ret;
}

JNIEXPORT jint JNICALL
Java_com_jwoolston_android_libusb_UsbDeviceConnection_nativeReleaseInterface(JNIEnv *env, jobject instance,
                                                                             jobject device, jint interfaceID) {
    struct libusb_device_handle *deviceHandle = (struct libusb_device_handle *) (*env)->GetDirectBufferAddress(env,
                                                                                                               device);
    return libusb_release_interface(deviceHandle, interfaceID);
}

JNIEXPORT jint JNICALL
Java_com_jwoolston_android_libusb_UsbDeviceConnection_nativeSetInterface(JNIEnv *env, jobject instance, jobject device,
                                                                         jint interfaceID, jint alternateSetting) {
    struct libusb_device_handle *deviceHandle = (struct libusb_device_handle *) (*env)->GetDirectBufferAddress(env,
                                                                                                               device);
    return libusb_set_interface_alt_setting(deviceHandle, interfaceID, alternateSetting);
}

JNIEXPORT jint JNICALL
Java_com_jwoolston_android_libusb_UsbDeviceConnection_nativeSetConfiguration(JNIEnv *env, jobject instance,
                                                                             jobject device, jint configurationID) {
    struct libusb_device_handle *deviceHandle = (struct libusb_device_handle *) (*env)->GetDirectBufferAddress(env,
                                                                                                               device);
    return libusb_set_configuration(deviceHandle, configurationID);
}

JNIEXPORT jint JNICALL
Java_com_jwoolston_android_libusb_UsbDeviceConnection_nativeControlRequest(JNIEnv *env, jobject instance,
                                                                           jobject device, jint requestType,
                                                                           jint request, jint value, jint index,
                                                                           jbyteArray buffer_, jint offset, jint length,
                                                                           jint timeout) {
    struct libusb_device_handle *deviceHandle = (struct libusb_device_handle *) (*env)->GetDirectBufferAddress(env,
                                                                                                               device);
    jbyte* buffer = NULL;
    if (buffer_) {
        buffer = (jbyte*)(*env)->GetPrimitiveArrayCritical(env, buffer_, NULL);
    }
    jint result = libusb_control_transfer(deviceHandle, requestType, request, value, index, buffer + offset, length,
                                          timeout);
    if (buffer) {
        (*env)->ReleasePrimitiveArrayCritical(env, buffer_, buffer, 0);
    }

    return result;
}

JNIEXPORT jint JNICALL
Java_com_jwoolston_android_libusb_UsbDeviceConnection_nativeBulkRequest(JNIEnv *env, jobject instance, jobject device,
                                                                        jint endpoint, jbyteArray buffer_, jint offset,
                                                                        jint length, jint timeout) {
    struct libusb_device_handle *deviceHandle = (struct libusb_device_handle *) (*env)->GetDirectBufferAddress(env,
                                                                                                               device);
    jbyte* buffer = NULL;
    if (buffer_) {
        buffer = (jbyte*)(*env)->GetPrimitiveArrayCritical(env, buffer_, NULL);
    }
    jint transfered;
    jint result = libusb_bulk_transfer(deviceHandle, endpoint, buffer + offset, length, &transfered, timeout);
    if (buffer) {
        (*env)->ReleasePrimitiveArrayCritical(env, buffer_, buffer, 0);
    }
    return ((result == 0) ? transfered : result);
}

JNIEXPORT jint JNICALL
Java_com_jwoolston_android_libusb_UsbDeviceConnection_nativeResetDevice(JNIEnv *env, jobject instance, jobject device) {
    struct libusb_device_handle *deviceHandle = (struct libusb_device_handle *) (*env)->GetDirectBufferAddress(env,
                                                                                                               device);
    return libusb_reset_device(deviceHandle);
}