//
// Created by Jared Woolston (Jared.Woolston@gmail.com)
//

#include <common.h>

#pragma clang diagnostic push
#pragma clang diagnostic ignored "-Wunused-parameter"
#define  LOG_TAG    "UsbDevice-Native"

JNIEXPORT jstring JNICALL
Java_com_jwoolston_libusb_BaseUsbDevice_nativeGetStringDescriptor(JNIEnv *env, jclass type, jobject device,
                                                                      jint index) {
    if (index == 0) {
        return NULL;
    }
    struct libusb_device_handle *deviceHandle = (struct libusb_device_handle *) (*env)->GetDirectBufferAddress(env,
                                                                                                               device);
    size_t length = 50 * sizeof(unsigned char);
    unsigned char *name = malloc(length);
    libusb_get_string_descriptor_ascii(deviceHandle, (uint8_t) (0xFF & index), name, (int) length);
    jstring retval = (*env)->NewStringUTF(env, (const char *) name);
    free(name);
    return retval;
}

JNIEXPORT jobject JNICALL
Java_com_jwoolston_libusb_BaseUsbDevice_wrapDevice(JNIEnv *env, jclass type, jobject context, jint fd) {
    LOGD("Wrapping USB Device Handle.");
    struct libusb_device_handle *deviceHandle;

    struct libusb_context *ctx = (struct libusb_context *) (*env)->GetDirectBufferAddress(env, context);
    int ret =  libusb_wrap_sys_device(ctx, fd, &deviceHandle);

    if (deviceHandle == NULL) {
        LOGE("Failed to wrap usb device file descriptor. Error: %s", libusb_strerror((enum libusb_error) ret));
        return NULL;
    }

    return ((*env)->NewDirectByteBuffer(env, (void *) deviceHandle, sizeof(struct libusb_device_handle)));
}

JNIEXPORT jstring JNICALL
Java_com_jwoolston_libusb_BaseUsbDevice_nativeGetManufacturerString(JNIEnv *env, jobject instance, jobject device,
                                                                        jobject descriptor) {
    struct libusb_device_handle *deviceHandle = (struct libusb_device_handle *) (*env)->GetDirectBufferAddress(env,
                                                                                                               device);
    struct libusb_device_descriptor *deviceDescriptor = (struct libusb_device_descriptor *)
            (*env)->GetDirectBufferAddress(env, descriptor);

    size_t length = 50 * sizeof(unsigned char);
    unsigned char *name = malloc(length);
    libusb_get_string_descriptor_ascii(deviceHandle, deviceDescriptor->iManufacturer, name, (int) length);
    jstring retval = (*env)->NewStringUTF(env, (const char *) name);
    free(name);
    return retval;
}

JNIEXPORT jstring JNICALL
Java_com_jwoolston_libusb_BaseUsbDevice_nativeGetSerialString(JNIEnv *env, jobject instance, jobject device,
                                                                    jobject descriptor) {
    struct libusb_device_handle *deviceHandle = (struct libusb_device_handle *) (*env)->GetDirectBufferAddress(env,
                                                                                                               device);
    struct libusb_device_descriptor *deviceDescriptor = (struct libusb_device_descriptor *)
            (*env)->GetDirectBufferAddress(env, descriptor);

    size_t length = 50 * sizeof(unsigned char);
    unsigned char *serial = malloc(length);
    libusb_get_string_descriptor_ascii(deviceHandle, deviceDescriptor->iSerialNumber, serial, (int) length);
    jstring retval = (*env)->NewStringUTF(env, (const char *) serial);
    free(serial);
    return retval;
}

JNIEXPORT jstring JNICALL
Java_com_jwoolston_libusb_BaseUsbDevice_nativeGetProductNameString(JNIEnv *env, jobject instance, jobject device,
                                                                       jobject descriptor) {
    struct libusb_device_handle *deviceHandle = (struct libusb_device_handle *) (*env)->GetDirectBufferAddress(env,
                                                                                                               device);
    struct libusb_device_descriptor *deviceDescriptor = (struct libusb_device_descriptor *)
            (*env)->GetDirectBufferAddress(env, descriptor);

    size_t length = 50 * sizeof(unsigned char);
    unsigned char *name = malloc(length);
    libusb_get_string_descriptor_ascii(deviceHandle, deviceDescriptor->iProduct, name, (int) length);
    jstring retval = (*env)->NewStringUTF(env, (const char *) name);
    free(name);
    return retval;
}

JNIEXPORT jstring JNICALL
Java_com_jwoolston_libusb_BaseUsbDevice_nativeGetDeviceVersion(JNIEnv *env, jobject instance, jobject descriptor) {
    struct libusb_device_descriptor *deviceDescriptor = (struct libusb_device_descriptor *)
            (*env)->GetDirectBufferAddress(env, descriptor);
    uint16_t bcdDevice = deviceDescriptor->bcdDevice;
    size_t length = 4 * sizeof(unsigned char);
    char *version = malloc(length);
    snprintf(version, length, "%i.%i", 0xFF & (bcdDevice >> 8), 0xFF & bcdDevice);
    jstring retval = (*env)->NewStringUTF(env, (const char *) version);
    free(version);
    return retval;
}

JNIEXPORT jint JNICALL
Java_com_jwoolston_libusb_BaseUsbDevice_nativeGetDeviceSpeed(JNIEnv *env, jobject instance, jobject device,
                                                                 jobject descriptor) {

    struct libusb_device_handle *deviceHandle = (struct libusb_device_handle *) (*env)->GetDirectBufferAddress(env,
                                                                                                               device);
    return libusb_get_device_speed(deviceHandle);
}

JNIEXPORT jint JNICALL
Java_com_jwoolston_libusb_BaseUsbDevice_nativeGetConfigurationCount(JNIEnv *env, jobject instance, jobject device) {
    struct libusb_device_handle *deviceHandle = (struct libusb_device_handle *) (*env)->GetDirectBufferAddress(env,
                                                                                                               device);
    return deviceHandle->dev->num_configurations;
}

JNIEXPORT jlong JNICALL
Java_com_jwoolston_libusb_BaseUsbDevice_nativeGetPointerFromNativeObject(JNIEnv *env, jobject instance,
                                                                             jobject device) {
    struct libusb_device_handle *deviceHandle = (struct libusb_device_handle *) (*env)->GetDirectBufferAddress(env,
                                                                                                               device);
    return (jlong) ((void *) deviceHandle);

}

JNIEXPORT jobject JNICALL
Java_com_jwoolston_libusb_BaseUsbDevice_nativeGetNativeObjectFromPointer(JNIEnv *env, jobject instance,
                                                                             jlong pointer) {
    struct libusb_device_handle *deviceHandle = (struct libusb_device_handle *) ((void *) pointer);

    if (deviceHandle == NULL) {
        return NULL;
    }

    return ((*env)->NewDirectByteBuffer(env, (void *) deviceHandle, sizeof(struct libusb_device_handle)));
}

#pragma clang diagnostic pop