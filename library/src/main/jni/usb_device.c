//
// Created by Jared Woolston (Jared.Woolston@gmail.com)
//

#include <jni.h>
#include <libusb.h>
#include <libusbi.h>

#include "logging.h"

#define  LOG_TAG    "UsbDevice-Native"

JNIEXPORT jint JNICALL
Java_com_jwoolston_android_libusb_UsbDevice_nativeGetDeviceId(JNIEnv *env, jclass type, jstring name_) {
    const char *name = (*env)->GetStringUTFChars(env, name_, 0);

    // TODO

    (*env)->ReleaseStringUTFChars(env, name_, name);

    return 0;
}

JNIEXPORT jstring JNICALL
Java_com_jwoolston_android_libusb_UsbDevice_nativeGetDeviceName(JNIEnv *env, jclass type, jint id) {

    char *returnValue = "retval";


    return (*env)->NewStringUTF(env, returnValue);
}

JNIEXPORT jobject JNICALL
Java_com_jwoolston_android_libusb_UsbDevice_wrapDevice(JNIEnv *env, jclass type, jobject context, jint fd) {
    LOGD("Wrapping USB Device Handle.");
    struct libusb_device_handle *deviceHandle;

    struct libusb_context *ctx = (libusb_context *) (*env)->GetDirectBufferAddress(env, context);
    libusb_wrap_fd(ctx, fd, &deviceHandle);

    if (deviceHandle == NULL) {
        LOGE("Failed to wrap usb device file descriptor.");
        return NULL;
    }

    // Claim the control interface
    libusb_claim_interface(deviceHandle, 0);

    return ((*env)->NewDirectByteBuffer(env, (void *) deviceHandle, sizeof(struct libusb_device_handle)));
}

JNIEXPORT jstring JNICALL
Java_com_jwoolston_android_libusb_UsbDevice_nativeGetManufacturerString(JNIEnv *env, jobject instance, jobject device,
                                                                        jobject descriptor) {
    struct libusb_device_handle *deviceHandle = (libusb_device_handle *) (*env)->GetDirectBufferAddress(env, device);
    struct libusb_device_descriptor *deviceDescriptor = (struct libusb_device_descriptor *)
            (*env)->GetDirectBufferAddress(env, descriptor);

    size_t length = 50 * sizeof(unsigned char);
    unsigned char *name = malloc(length);
    libusb_get_string_descriptor_ascii(deviceHandle, deviceDescriptor->iManufacturer, name, length);
    jstring retval = (*env)->NewStringUTF(env, (const char *) name);
    free(name);
    return retval;
}


JNIEXPORT jstring JNICALL
Java_com_jwoolston_android_libusb_UsbDevice_nativeGetProductNameString(JNIEnv *env, jobject instance, jobject device,
                                                                       jobject descriptor) {
    struct libusb_device_handle *deviceHandle = (libusb_device_handle *) (*env)->GetDirectBufferAddress(env, device);
    struct libusb_device_descriptor *deviceDescriptor = (struct libusb_device_descriptor *)
            (*env)->GetDirectBufferAddress(env, descriptor);

    size_t length = 50 * sizeof(unsigned char);
    unsigned char *name = malloc(length);
    libusb_get_string_descriptor_ascii(deviceHandle, deviceDescriptor->iProduct, name, length);
    jstring retval = (*env)->NewStringUTF(env, (const char *) name);
    free(name);
    return retval;
}

JNIEXPORT jstring JNICALL
Java_com_jwoolston_android_libusb_UsbDevice_nativeGetDeviceVersion(JNIEnv *env, jobject instance, jobject device,
                                                                   jobject descriptor) {
    struct libusb_device_handle *deviceHandle = (libusb_device_handle *) (*env)->GetDirectBufferAddress(env, device);
    struct libusb_device_descriptor *deviceDescriptor = (struct libusb_device_descriptor *)
            (*env)->GetDirectBufferAddress(env, descriptor);
    uint16_t bcdDevice = deviceDescriptor->bcdDevice;
    size_t length = 4 * sizeof(unsigned char);
    unsigned char *version = malloc(length);
    snprintf(version, length, "%i.%i", 0xFF & (bcdDevice >> 8), 0xFF & bcdDevice);
    jstring retval = (*env)->NewStringUTF(env, (const char *) version);
    free(version);
    return retval;
}

JNIEXPORT jobject JNICALL
Java_com_jwoolston_android_libusb_LibUsbDeviceDescriptor_nativeGetDeviceDescriptor(JNIEnv *env, jclass type,
                                                                                   jobject device) {
    struct libusb_device_handle *deviceHandle = (struct libusb_device_handle *)
            (*env)->GetDirectBufferAddress(env, device);

    struct libusb_device_descriptor *descriptor;
    libusb_get_device_descriptor(deviceHandle->dev, descriptor);
    return ((*env)->NewDirectByteBuffer(env, (void *) descriptor, sizeof(struct libusb_device_descriptor)));
}