//
// Created by Jared Woolston (Jared.Woolston@gmail.com)
//

#include <common.h>

JNIEXPORT jobject JNICALL
Java_com_jwoolston_libusb_LibUsbDeviceDescriptor_nativeGetDeviceDescriptorFromHandle(JNIEnv *env, jclass type,
                                                                                   jobject handle) {
    struct libusb_device_handle *deviceHandle = (struct libusb_device_handle *)
            (*env)->GetDirectBufferAddress(env, handle);

    // The descriptor is cached in memory so we don't need to allocate memory for it
    struct libusb_device_descriptor descriptor;
    libusb_get_device_descriptor(deviceHandle->dev, &descriptor);
    return ((*env)->NewDirectByteBuffer(env, (void *) &descriptor, sizeof(struct libusb_device_descriptor)));
}

JNIEXPORT jobject JNICALL
Java_com_jwoolston_libusb_LibUsbDeviceDescriptor_nativeGetDeviceDescriptorFromDevice(JNIEnv *env, jclass type,
                                                                                     jlong device) {
    struct libusb_device *devicePtr = (struct libusb_device *) device;

    // The descriptor is cached in memory so we don't need to allocate memory for it
    struct libusb_device_descriptor descriptor;
    libusb_get_device_descriptor(devicePtr, &descriptor);
    return ((*env)->NewDirectByteBuffer(env, (void *) &descriptor, sizeof(struct libusb_device_descriptor)));
}

JNIEXPORT void JNICALL
Java_com_jwoolston_libusb_LibUsbDeviceDescriptor_nativeDestroy(JNIEnv *env, jclass type, jobject descriptor) {
    struct libusb_device_descriptor *deviceDescriptor = (struct libusb_device_descriptor *)
            (*env)->GetDirectBufferAddress(env, descriptor);
    free(deviceDescriptor);
}

JNIEXPORT jint JNICALL
Java_com_jwoolston_libusb_LibUsbDeviceDescriptor_nativeGetVendorId(JNIEnv *env, jclass type, jobject descriptor) {
    struct libusb_device_descriptor *deviceDescriptor = (struct libusb_device_descriptor *)
            (*env)->GetDirectBufferAddress(env, descriptor);
    return deviceDescriptor->idVendor;
}

JNIEXPORT jint JNICALL
Java_com_jwoolston_libusb_LibUsbDeviceDescriptor_nativeGetProductId(JNIEnv *env, jclass type, jobject descriptor) {
    struct libusb_device_descriptor *deviceDescriptor = (struct libusb_device_descriptor *)
            (*env)->GetDirectBufferAddress(env, descriptor);
    return deviceDescriptor->idProduct;
}

JNIEXPORT jint JNICALL
Java_com_jwoolston_libusb_LibUsbDeviceDescriptor_nativeGetDeviceClass(JNIEnv *env, jclass type, jobject descriptor) {
    struct libusb_device_descriptor *deviceDescriptor = (struct libusb_device_descriptor *)
            (*env)->GetDirectBufferAddress(env, descriptor);
    return deviceDescriptor->bDeviceClass;
}

JNIEXPORT jint JNICALL
Java_com_jwoolston_libusb_LibUsbDeviceDescriptor_nativeGetDeviceSubclass(JNIEnv *env, jclass type, jobject descriptor) {
    struct libusb_device_descriptor *deviceDescriptor = (struct libusb_device_descriptor *)
            (*env)->GetDirectBufferAddress(env, descriptor);
    return deviceDescriptor->bDeviceSubClass;
}

JNIEXPORT jint JNICALL
Java_com_jwoolston_libusb_LibUsbDeviceDescriptor_nativeGetDeviceProtocol(JNIEnv *env, jclass type, jobject descriptor) {
    struct libusb_device_descriptor *deviceDescriptor = (struct libusb_device_descriptor *)
            (*env)->GetDirectBufferAddress(env, descriptor);
    return deviceDescriptor->bDeviceProtocol;
}