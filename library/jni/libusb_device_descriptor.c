//
// Created by Jared Woolston (Jared.Woolston@gmail.com)
//

#include <common.h>

JNIEXPORT jobject JNICALL
Java_com_jwoolston_libusb_LibUsbDeviceDescriptor_nativeGetDeviceDescriptor(JNIEnv *env, jclass type,
                                                                                   jobject device) {
    struct libusb_device_handle *deviceHandle = (struct libusb_device_handle *)
            (*env)->GetDirectBufferAddress(env, device);

    struct libusb_device_descriptor *descriptor = malloc(sizeof(struct libusb_device_descriptor));
    libusb_get_device_descriptor(deviceHandle->dev, descriptor);
    return ((*env)->NewDirectByteBuffer(env, (void *) descriptor, sizeof(struct libusb_device_descriptor)));
}

JNIEXPORT void JNICALL
Java_com_jwoolston_libusb_LibUsbDeviceDescriptor_nativeDestroy(JNIEnv *env, jclass type, jobject descriptor) {
    struct libusb_device_descriptor *deviceDescriptor = (struct libusb_device_descriptor *)
            (*env)->GetDirectBufferAddress(env, descriptor);
    free(deviceDescriptor);
}
