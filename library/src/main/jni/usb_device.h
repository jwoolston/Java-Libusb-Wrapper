//
// Created by Jared Woolston (Jared.Woolston@gmail.com)
//

#ifndef LIBUSB_WRAPPER_USB_DEVICE_H
#define LIBUSB_WRAPPER_USB_DEVICE_H

#include <jni.h>

#ifdef __cplusplus
extern "C" {
#endif

JNIEXPORT jint JNICALL
Java_com_jwoolston_android_libusb_UsbDevice_nativeGetDeviceId(JNIEnv *env, jclass type, jstring name_);

JNIEXPORT jstring JNICALL
Java_com_jwoolston_android_libusb_UsbDevice_nativeGetDeviceName(JNIEnv *env, jclass type, jint id);

#ifdef __cplusplus
}
#endif
#endif //LIBUSB_WRAPPER_USB_DEVICE_H
