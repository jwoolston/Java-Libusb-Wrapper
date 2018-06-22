//
// Created by Jared Woolston (Jared.Woolston@gmail.com)
//

#include <common.h>

#define  LOG_TAG    "AsyncTransfer-Native"

JNIEXPORT void JNICALL
Java_com_jwoolston_android_libusb_async_AsyncTransfer_nativeDestroy(JNIEnv *env, jobject instance,
                                                                    jobject nativeObject) {
    struct libusb_transfer *transfer = (struct libusb_transfer *) (*env)->GetDirectBufferAddress(env, nativeObject);
    libusb_free_transfer(transfer);
}