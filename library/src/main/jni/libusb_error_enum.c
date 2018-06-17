//
// Created by Jared Woolston (Jared.Woolston@gmail.com)
//

#include <common.h>

JNIEXPORT jstring JNICALL
Java_com_jwoolston_android_libusb_LibusbError_getDescriptionString(JNIEnv *env, jclass type, jint code) {
    return (*env)->NewStringUTF(env, libusb_strerror((enum libusb_error) code));
}
