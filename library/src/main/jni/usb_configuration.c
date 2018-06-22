//
// Created by Jared Woolston (Jared.Woolston@gmail.com)
//

#include <unistd.h>
#include <string.h>
#include <common.h>

#define  LOG_TAG    "UsbConfiguration-Native"

JNIEXPORT jobject JNICALL
Java_com_jwoolston_android_libusb_UsbConfiguration_nativeGetConfiguration(JNIEnv *env, jclass type, jobject device,
                                                                          jint configuration) {
    struct libusb_device_handle *deviceHandle = (struct libusb_device_handle *) (*env)->GetDirectBufferAddress(env,
                                                                                                               device);
    struct libusb_config_descriptor *config;
    int retval = libusb_get_config_descriptor(deviceHandle->dev, (uint8_t) (0xFF & configuration), &config);
    if (retval) {
        LOGE("Error fetching configuration descriptor: %s", libusb_strerror(retval));
        return NULL;
    }

    return ((*env)->NewDirectByteBuffer(env, (void *) config, sizeof(struct libusb_config_descriptor)));
}

JNIEXPORT jobject JNICALL
Java_com_jwoolston_android_libusb_UsbConfiguration_nativeGetInterface(JNIEnv *env, jclass type, jobject nativeObject,
                                                                      jint interfaceIndex) {
    struct libusb_config_descriptor *config = (struct libusb_config_descriptor *)
            (*env)->GetDirectBufferAddress(env, nativeObject);

    return ((*env)->NewDirectByteBuffer(env, (void *) (config->interface + interfaceIndex), sizeof(struct
            libusb_interface)));
}

JNIEXPORT void JNICALL
Java_com_jwoolston_android_libusb_UsbConfiguration_nativeDestroy(JNIEnv *env, jclass type, jobject nativeObject) {
    struct libusb_config_descriptor *config = (struct libusb_config_descriptor *)
            (*env)->GetDirectBufferAddress(env, nativeObject);
    libusb_free_config_descriptor(config);
}
