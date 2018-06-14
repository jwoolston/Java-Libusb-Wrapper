
#include <jni.h>
#include <libusb.h>

#include "logging.h"

#define  LOG_TAG    "UsbManager-Native"

JNIEXPORT jobject JNICALL
Java_com_jwoolston_android_libusb_UsbManager_initialize(JNIEnv *env, jobject instance) {
    LOGD("Initializing libusb.");
    struct libusb_context *ctx;
    int r = libusb_init(&ctx);
    if (r < 0) {
        LOGE("Initialization returned: %i", r);
        return NULL;
    } else {
        jobject buffer = (*env)->NewDirectByteBuffer(env, (void *) ctx, sizeof(libusb_context));
        return buffer;
    }
}

