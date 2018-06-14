//
// Created by Jared Woolston (Jared.Woolston@gmail.com)
//

#include <jni.h>
#include <libusb.h>

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
Java_com_jwoolston_android_libusb_UsbDevice_wrapDevice(JNIEnv *env, jclass type, jobject libUsbContext, jint fd) {
    LOGD("Wrapping USB Device Handle.");
    struct libusb_device_handle *dev_handle;

    libusb_context *ctx = (libusb_context *) (*env)->GetDirectBufferAddress(env, buffer);
    libusb_wrap_fd(ctx, fd, &dev_handle);

    if (dev_handle == NULL) {
        LOGE("Failed to wrap usb device file descriptor.");
        return NULL;
    }

    // Claim the control interface
    libusb_claim_interface(dev_handle, 0);

    return ((*env)->NewDirectByteBuffer(env, (void *) dev_handle, sizeof(struct libusb_device_handle)));
}