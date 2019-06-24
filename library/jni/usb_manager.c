//
// Created by Jared Woolston (Jared.Woolston@gmail.com)
//

#include <common.h>

#define  LOG_TAG    "UsbManager-Native"

JNIEXPORT jobject JNICALL
Java_com_jwoolston_android_libusb_UsbManager_nativeInitialize(JNIEnv *env, jobject instance) {
    // Initialize Arbor
    initializeArbor(env);
    LOGD("Initializing libusb.");
    struct libusb_context *ctx;
    int r = libusb_init(&ctx);
    if (r < 0) {
        LOGE("Initialization returned: %i", r);
        return NULL;
    } else {
        jobject buffer = (*env)->NewDirectByteBuffer(env, (void *) ctx, sizeof(struct libusb_context));
        return buffer;
    }
}

JNIEXPORT void JNICALL
Java_com_jwoolston_android_libusb_UsbManager_nativeSetLoggingLevel(JNIEnv *env, jobject instance,
                                                                      jobject nativeObject, jint level) {
    struct libusb_context *ctx = (libusb_context *) (*env)->GetDirectBufferAddress(env, nativeObject);
    libusb_set_option(ctx, LIBUSB_OPTION_LOG_LEVEL, level);
}

JNIEXPORT void JNICALL
Java_com_jwoolston_android_libusb_UsbManager_nativeDestroy(JNIEnv *env, jobject instance, jobject context) {
    LOGD("De-initializing libusb.");
    struct libusb_context *ctx = (libusb_context *) (*env)->GetDirectBufferAddress(env, context);
    libusb_exit(ctx);
}

