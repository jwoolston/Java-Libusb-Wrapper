//
// Created by Jared Woolston (Jared.Woolston@gmail.com)
//

#include "usb_device.h"

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