//
// Created by Jared Woolston (Jared.Woolston@gmail.com)
//

#include <unistd.h>
#include <string.h>
#include <common.h>

#define  LOG_TAG    "UsbConfInterface-Native"

JNIEXPORT jobject JNICALL
Java_com_jwoolston_android_libusb_UsbInterface_nativeGetInterfaceDescriptor(JNIEnv *env, jclass type,
                                                                            jobject nativeObject, jint index) {
    struct libusb_interface *interface = (struct libusb_interface *) (*env)->GetDirectBufferAddress(env, nativeObject);
    if (index >= interface->num_altsetting) {
        return NULL;
    }

    return ((*env)->NewDirectByteBuffer(env, (void *) (interface->altsetting + index), sizeof(struct
            libusb_interface_descriptor)));
}

JNIEXPORT jobject JNICALL
Java_com_jwoolston_android_libusb_UsbInterface_nativeGetEndpoint(JNIEnv *env, jclass type, jobject nativeDescriptor,
                                                                 jint index) {
    struct libusb_interface_descriptor *descriptor = (struct libusb_interface_descriptor *)
            (*env)->GetDirectBufferAddress(env, nativeDescriptor);
    if (index >= descriptor->bNumEndpoints) {
        return NULL;
    }

    return ((*env)->NewDirectByteBuffer(env, (void *) (descriptor->endpoint + index), sizeof(struct
            libusb_endpoint_descriptor)));
}