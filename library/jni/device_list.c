//
// Created by jared on 6/26/2019.
//
#include <common.h>

#define  LOG_TAG    "DeviceList-Native"

jobject listClass;
jobject usbDeviceClass;
jmethodID listAdd;
jmethodID usbDeviceConstructor;

JNIEXPORT jlong JNICALL
Java_com_jwoolston_libusb_DeviceList_nativeInitialize(JNIEnv *env, jclass type) {
    // Find the classes
    jclass localClass = (*env)->FindClass(env, "java/util/List");
    listClass = (jclass) (*env)->NewGlobalRef(env, localClass);
    localClass = (*env)->FindClass(env, "com/jwoolston/libusb/UsbDevice");
    usbDeviceClass = (jclass) (*env)->NewGlobalRef(env, localClass);

    // Find the methods
    listAdd = (*env)->GetMethodID(env, listClass, "add", "(Ljava/lang/Object;)Z");
    usbDeviceConstructor = (*env)->GetMethodID(env, usbDeviceClass, "<init>", "(J)V");
}

JNIEXPORT jlong JNICALL
Java_com_jwoolston_libusb_DeviceList_nativeGetDeviceList(JNIEnv *env, jclass type,
                                                             jobject nativeContext) {

    struct libusb_context *ctx
            = (libusb_context *) (*env)->GetDirectBufferAddress(env, nativeContext);
    // Discover devices
    libusb_device **list;
    ssize_t count = libusb_get_device_list(ctx, &list);
    if (count < 0) {
        LOGE("Failed to retrieve USB device list. Error: %i", count);
        return NULL;
    }
    return (void *) list;
}

JNIEXPORT void JNICALL
Java_com_jwoolston_libusb_DeviceList_nativePopulateDeviceList(JNIEnv *env, jclass type,
                                                             jlong nativeObject, jobject devices) {
    if (nativeObject == NULL) {
        return;
    }

    libusb_device **list = (libusb_device **) nativeObject;
    int index = 0;
    libusb_device *device = list[index];
    while (device != NULL) {
        jobject usbDevice = (*env)->NewObject(env, usbDeviceClass, usbDeviceConstructor,
                (void *) device);
        (*env)->CallBooleanMethod(env, devices, listAdd, usbDevice);
        device = list[++index];
    }
}

JNIEXPORT void JNICALL
Java_com_jwoolston_libusb_DeviceList_nativeRelease(JNIEnv *env, jobject instance,
                                                    jlong nativeObject) {
    if (nativeObject == NULL) {
        return;
    }

    libusb_device **list = (libusb_device **) nativeObject;
    libusb_free_device_list(list, 1);
}
