//
// Created by Jared Woolston (Jared.Woolston@gmail.com)
//

#include <unistd.h>
#include <string.h>
#include <stdbool.h>
#include <common.h>

#pragma clang diagnostic push
#pragma clang diagnostic ignored "-Wunused-parameter"
#define  LOG_TAG    "UsbDeviceConnection-Native"

static jmethodID controlCallback;
static jmethodID bulkCallback;
static jmethodID interruptCallback;

struct transfer_callback_holder {
    jobject *callback;

    JavaVM *vm;

    bool cleanup;
};

static struct transfer_callback_holder *allocate_callback_holder(jobject *callback, JNIEnv *env, bool cleanup) {
    struct transfer_callback_holder *holder = malloc(sizeof(struct transfer_callback_holder));
    holder->callback = callback;
    JavaVM *vm;
    (*env)->GetJavaVM(env, &vm);
    holder->vm = vm;
    holder->cleanup = cleanup;
    return holder;
}

static void LIBUSB_CALL libusb_transfer_callback(struct libusb_transfer *transfer) {
    int result;
    switch (transfer->status) {
        case LIBUSB_TRANSFER_COMPLETED:
            result = transfer->actual_length;
            break;
        case LIBUSB_TRANSFER_TIMED_OUT:
            result = LIBUSB_ERROR_TIMEOUT;
            break;
        case LIBUSB_TRANSFER_STALL:
            result = LIBUSB_ERROR_PIPE;
            break;
        case LIBUSB_TRANSFER_NO_DEVICE:
            result = LIBUSB_ERROR_NO_DEVICE;
            break;
        case LIBUSB_TRANSFER_OVERFLOW:
            result = LIBUSB_ERROR_OVERFLOW;
            break;
        case LIBUSB_TRANSFER_ERROR:
        case LIBUSB_TRANSFER_CANCELLED:
            result = LIBUSB_ERROR_IO;
            break;
        default:
            LOGE("Unrecognised status code %d", transfer->status);
            result = LIBUSB_ERROR_OTHER;
    }

    struct transfer_callback_holder *holder = (struct transfer_callback_holder *) transfer->user_data;
    if (holder == NULL) {
        LOGE("Null callback holder! Callback cannot be called.");
        // This is a fatal error so we cant make any decisions about freeing the transfer, so we leave it intact in case
        // the calling code recovers.
        return;
    }
    JNIEnv *env;
    int jniResult = (*holder->vm)->GetEnv(holder->vm, (void **) &env, JNI_VERSION_1_6);
    if (jniResult != JNI_OK) {
        result = LIBUSB_ERROR_OTHER;
        LOGE("Failed to retrieve JNI environment: %i", jniResult);
    }

    unsigned char *data = NULL;
    jobject byteBuffer = NULL;

    if (result >= 0) {
        switch (transfer->type) {
            case LIBUSB_TRANSFER_TYPE_CONTROL: {
                struct libusb_control_setup *controlSetup = libusb_control_transfer_get_setup(transfer);
                if ((controlSetup->bmRequestType & LIBUSB_ENDPOINT_DIR_MASK) == LIBUSB_ENDPOINT_IN) {
                    data = malloc(transfer->actual_length * sizeof(unsigned char));
                    memcpy(data, libusb_control_transfer_get_data(transfer), (size_t) transfer->actual_length);
                    // We don't free the buffer here because we expect libusb to do it with control transfers
                    (*env)->NewDirectByteBuffer(env, data, transfer->actual_length);
                }
                jobject callback = holder->callback;
                // We must always free our callback holder
                free(holder);
                libusb_free_transfer(transfer);
                (*env)->CallVoidMethod(env, callback, controlCallback, byteBuffer, result);
                break;
            }
            case LIBUSB_TRANSFER_TYPE_BULK: {
                if ((transfer->endpoint & LIBUSB_ENDPOINT_DIR_MASK) == LIBUSB_ENDPOINT_IN) {
                    data = malloc(transfer->actual_length * sizeof(unsigned char));
                    memcpy(data, transfer->buffer, (size_t) transfer->actual_length);
                    // We don't free the buffer here because we expect libusb to do it with bulk transfers
                    (*env)->NewDirectByteBuffer(env, data, transfer->actual_length);
                }
                jobject callback = holder->callback;
                // We must always free our callback holder
                free(holder);
                libusb_free_transfer(transfer);
                (*env)->CallVoidMethod(env, callback, bulkCallback, byteBuffer, result);
                break;
            }
            case LIBUSB_TRANSFER_TYPE_INTERRUPT: {
                if ((transfer->endpoint & LIBUSB_ENDPOINT_DIR_MASK) == LIBUSB_ENDPOINT_IN) {
                    data = malloc(transfer->actual_length * sizeof(unsigned char));
                    memcpy(data, transfer->buffer, (size_t) transfer->actual_length);
                    // We don't free the buffer here because we expect libusb to do it with basic interrupt transfers
                    (*env)->NewDirectByteBuffer(env, data, transfer->actual_length);
                }
                jobject callback = holder->callback;
                // We must always free our callback holder
                free(holder);
                libusb_free_transfer(transfer);
                (*env)->CallVoidMethod(env, callback, interruptCallback, byteBuffer, result);
                break;
            }
            case LIBUSB_TRANSFER_TYPE_ISOCHRONOUS:
                break;
            default:
                LOGE("Unsupported transfer type: %i", transfer->type);
                // We must always free our callback holder
                free(holder);
                libusb_free_transfer(transfer);
        }
    }
}

JNIEXPORT jboolean JNICALL
Java_com_jwoolston_android_libusb_UsbDeviceConnection_nativeInitialize(JNIEnv *env, jclass type) {
    // Find the control transfer callback method
    jclass clazz = (*env)->FindClass(env, "com/jwoolston/android/libusb/async/ControlTransferCallback");
    if (clazz == NULL) {
        LOGE("Failed to find class com.jwoolston.android.libusb.async.ControlTransferCallback");
        return JNI_FALSE;
    }
    controlCallback = (*env)->GetMethodID(env, clazz, "onControlTransferComplete", "(Ljava/nio/ByteBuffer;I)V");
    if (controlCallback == NULL) {
        LOGE("Failed to find onControlTransferComplete(ByteBuffer, int) method.");
        return JNI_FALSE;
    }

    // Find the bulk transfer callback method
    clazz = (*env)->FindClass(env, "com/jwoolston/android/libusb/async/BulkTransferCallback");
    if (clazz == NULL) {
        LOGE("Failed to find class com.jwoolston.android.libusb.async.BulkTransferCallback");
        return JNI_FALSE;
    }
    bulkCallback = (*env)->GetMethodID(env, clazz, "onBulkTransferComplete", "(Ljava/nio/ByteBuffer;I)V");
    if (controlCallback == NULL) {
        LOGE("Failed to find onBulkTransferComplete(ByteBuffer, int) method.");
        return JNI_FALSE;
    }

    // Find the interrupt transfer callback method
    clazz = (*env)->FindClass(env, "com/jwoolston/android/libusb/async/InterruptTransferCallback");
    if (clazz == NULL) {
        LOGE("Failed to find class com.jwoolston.android.libusb.async.InterruptTransferCallback");
        return JNI_FALSE;
    }
    interruptCallback = (*env)->GetMethodID(env, clazz, "onInterruptTransferComplete", "(Ljava/nio/ByteBuffer;I)V");
    if (controlCallback == NULL) {
        LOGE("Failed to find onInterruptTransferComplete(ByteBuffer, int) method.");
        return JNI_FALSE;
    }
    return JNI_TRUE;
}

JNIEXPORT void JNICALL
Java_com_jwoolston_android_libusb_UsbDeviceConnection_nativeClose(JNIEnv *env, jobject instance, jobject device) {
    struct libusb_device_handle *deviceHandle = (struct libusb_device_handle *) (*env)->GetDirectBufferAddress(env,
                                                                                                               device);
    libusb_close(deviceHandle);
    if (deviceHandle != NULL) {
        free(deviceHandle);
    }
}

JNIEXPORT jbyteArray JNICALL
Java_com_jwoolston_android_libusb_UsbDeviceConnection_nativeGetRawDescriptor(JNIEnv *env, jobject instance, jint fd) {
    char buffer[16384];
    if (fd < 0) return NULL;
    lseek(fd, 0, SEEK_SET);
    int length = read(fd, buffer, sizeof(buffer));
    if (length < 0) return NULL;
    jbyteArray ret = (*env)->NewByteArray(env, length);
    if (ret) {
        jbyte *bytes = (jbyte *) (*env)->GetPrimitiveArrayCritical(env, ret, 0);
        if (bytes) {
            memcpy(bytes, buffer, length);
            (*env)->ReleasePrimitiveArrayCritical(env, ret, bytes, 0);
        }
    }
    return ret;
}

JNIEXPORT jint JNICALL
Java_com_jwoolston_android_libusb_UsbDeviceConnection_nativeClaimInterface(JNIEnv *env, jobject instance,
                                                                           jobject device, jint interfaceID,
                                                                           jboolean force) {
    struct libusb_device_handle *deviceHandle = (struct libusb_device_handle *) (*env)->GetDirectBufferAddress(env,
                                                                                                               device);
    jint ret = libusb_claim_interface(deviceHandle, interfaceID);
    if (ret == LIBUSB_ERROR_BUSY && force) {
        libusb_detach_kernel_driver(deviceHandle, interfaceID);
        ret = libusb_claim_interface(deviceHandle, interfaceID);
    }
    return ret;
}

JNIEXPORT jint JNICALL
Java_com_jwoolston_android_libusb_UsbDeviceConnection_nativeReleaseInterface(JNIEnv *env, jobject instance,
                                                                             jobject device, jint interfaceID) {
    struct libusb_device_handle *deviceHandle = (struct libusb_device_handle *) (*env)->GetDirectBufferAddress(env,
                                                                                                               device);
    return libusb_release_interface(deviceHandle, interfaceID);
}

JNIEXPORT jint JNICALL
Java_com_jwoolston_android_libusb_UsbDeviceConnection_nativeSetInterface(JNIEnv *env, jobject instance, jobject device,
                                                                         jint interfaceID, jint alternateSetting) {
    struct libusb_device_handle *deviceHandle = (struct libusb_device_handle *) (*env)->GetDirectBufferAddress(env,
                                                                                                               device);
    return libusb_set_interface_alt_setting(deviceHandle, interfaceID, alternateSetting);
}

JNIEXPORT jint JNICALL
Java_com_jwoolston_android_libusb_UsbDeviceConnection_nativeSetConfiguration(JNIEnv *env, jobject instance,
                                                                             jobject device, jint configurationID) {
    struct libusb_device_handle *deviceHandle = (struct libusb_device_handle *) (*env)->GetDirectBufferAddress(env,
                                                                                                               device);
    return libusb_set_configuration(deviceHandle, configurationID);
}

JNIEXPORT jint JNICALL
Java_com_jwoolston_android_libusb_UsbDeviceConnection_nativeControlRequest(JNIEnv *env, jobject instance,
                                                                           jobject device, jint requestType,
                                                                           jint request, jint value, jint index,
                                                                           jbyteArray buffer_, jint offset, jint length,
                                                                           jint timeout) {
    struct libusb_device_handle *deviceHandle = (struct libusb_device_handle *) (*env)->GetDirectBufferAddress(env,
                                                                                                               device);
    jbyte *buffer = NULL;
    if (buffer_) {
        buffer = (jbyte *) (*env)->GetPrimitiveArrayCritical(env, buffer_, NULL);
    }
    jint result = libusb_control_transfer(deviceHandle, (uint8_t) (0xFF & requestType), (uint8_t) (0xFF & request),
                                          (uint16_t) (0xFFFF & value), (uint16_t) (0xFFFF & index),
                                          (unsigned char *) (buffer + offset), (uint16_t) (0xFFFF & length),
                                          (unsigned int) timeout);
    if (buffer) {
        (*env)->ReleasePrimitiveArrayCritical(env, buffer_, buffer, 0);
    }

    return result;
}

JNIEXPORT jint JNICALL
Java_com_jwoolston_android_libusb_UsbDeviceConnection_nativeControlRequestAsync(JNIEnv *env, jobject instance,
                                                                                jobject device, jobject callback,
                                                                                jint requestType, jint request,
                                                                                jint value, jint index,
                                                                                jbyteArray buffer_, jint offset,
                                                                                jint length, jint timeout) {
    struct libusb_device_handle *deviceHandle = (struct libusb_device_handle *) (*env)->GetDirectBufferAddress(env,
                                                                                                               device);

    if (usbi_handling_events(HANDLE_CTX(deviceHandle))) {
        return LIBUSB_ERROR_BUSY;
    }

    // Allocate the transfer
    struct libusb_transfer *transfer = libusb_alloc_transfer(0);
    jbyte *buffer = NULL;

    unsigned char *userData = (unsigned char *) malloc((LIBUSB_CONTROL_SETUP_SIZE + length) * sizeof(unsigned char));
    if (!userData) {
        libusb_free_transfer(transfer);
        return LIBUSB_ERROR_NO_MEM;
    }

    // Fill the data buffer if outgoing transfer
    if ((requestType & LIBUSB_ENDPOINT_DIR_MASK) == LIBUSB_ENDPOINT_OUT) {
        if (buffer_) {
            buffer = (jbyte *) (*env)->GetPrimitiveArrayCritical(env, buffer_, NULL);
        }
        memcpy(buffer + LIBUSB_CONTROL_SETUP_SIZE + offset, userData, (size_t) length);
        if (buffer) {
            (*env)->ReleasePrimitiveArrayCritical(env, buffer_, buffer, 0);
        }
    }

    // Fill the setup packet
    libusb_fill_control_setup(userData, (uint8_t) (0xFF & requestType), (uint8_t) (0xFF & request),
                              (uint16_t) (0xFFFF & value), (uint16_t) (0xFFFF & index), (uint16_t) (0xFFFF & length));

    // Populate the transfer structure
    struct transfer_callback_holder *holder = allocate_callback_holder(callback, env, true);

    libusb_fill_control_transfer(transfer, deviceHandle, userData, libusb_transfer_callback, holder,
                                 (unsigned int) timeout);
    transfer->flags = LIBUSB_TRANSFER_FREE_BUFFER;
    // Submit the transfer
    int result = libusb_submit_transfer(transfer);
    if (result < 0) {
        libusb_free_transfer(transfer);
    }
    return result;
}

JNIEXPORT jint JNICALL
Java_com_jwoolston_android_libusb_UsbDeviceConnection_nativeBulkRequest(JNIEnv *env, jobject instance, jobject device,
                                                                        jint endpoint, jbyteArray buffer_, jint offset,
                                                                        jint length, jint timeout) {
    struct libusb_device_handle *deviceHandle = (struct libusb_device_handle *) (*env)->GetDirectBufferAddress(env,
                                                                                                               device);
    jbyte *buffer = NULL;
    if (buffer_) {
        buffer = (jbyte *) (*env)->GetPrimitiveArrayCritical(env, buffer_, NULL);
    }
    int transferred;
    unsigned char *target;
    target = (endpoint & LIBUSB_ENDPOINT_DIR_MASK) == LIBUSB_ENDPOINT_IN ? calloc(1, 512) : buffer + offset;
    jint result = libusb_bulk_transfer(deviceHandle, (unsigned char) (0xFF & endpoint),
                                       target, length, &transferred, (unsigned int) timeout);

    if (result < 0) {
        LOGE("Result: %s", libusb_error_name(result));
    }

    if ((endpoint & LIBUSB_ENDPOINT_DIR_MASK) == LIBUSB_ENDPOINT_IN) {
        LOGV("Transferred: %i", transferred);
        log_dump(LOG_TAG, target, transferred, 16);
        memcpy(buffer, target, transferred);
        free(target);
    }

    if (buffer) {
        (*env)->ReleasePrimitiveArrayCritical(env, buffer_, buffer, 0);
    }
    return ((result == 0) ? transferred : result);
}

JNIEXPORT jint JNICALL
Java_com_jwoolston_android_libusb_UsbDeviceConnection_nativeBulkRequestAsync(JNIEnv *env, jobject instance,
                                                                             jobject device, jobject callback,
                                                                             jint address, jbyteArray buffer_,
                                                                             jint offset, jint length, jint timeout) {
    struct libusb_device_handle *deviceHandle = (struct libusb_device_handle *) (*env)->GetDirectBufferAddress(env,
                                                                                                               device);
    if (usbi_handling_events(HANDLE_CTX(deviceHandle))) {
        return LIBUSB_ERROR_BUSY;
    }

    // Allocate the transfer
    struct libusb_transfer *transfer = libusb_alloc_transfer(0);
    jbyte *buffer = NULL;

    unsigned char *userData = (unsigned char *) malloc((length) * sizeof(unsigned char));
    if (!userData) {
        libusb_free_transfer(transfer);
        return LIBUSB_ERROR_NO_MEM;
    }

    // Fill the data buffer if outgoing transfer
    if ((address & LIBUSB_ENDPOINT_DIR_MASK) == LIBUSB_ENDPOINT_OUT) {
        if (buffer_) {
            buffer = (jbyte *) (*env)->GetPrimitiveArrayCritical(env, buffer_, NULL);
        }
        memcpy(buffer + offset, userData, length);
        if (buffer) {
            (*env)->ReleasePrimitiveArrayCritical(env, buffer_, buffer, 0);
        }
    }

    // Populate the transfer structure
    struct transfer_callback_holder *holder = allocate_callback_holder(callback, env, true);

    libusb_fill_bulk_transfer(transfer, deviceHandle, address, userData, length, libusb_transfer_callback, holder,
                              timeout);
    transfer->type = LIBUSB_TRANSFER_TYPE_BULK;
    // Submit the transfer
    int result = libusb_submit_transfer(transfer);
    if (result < 0) {
        libusb_free_transfer(transfer);
        return result;
    }
}

JNIEXPORT jint JNICALL
Java_com_jwoolston_android_libusb_UsbDeviceConnection_nativeInterruptRequest(JNIEnv *env, jobject instance,
                                                                             jobject device, jint endpoint,
                                                                             jbyteArray buffer_, jint offset,
                                                                             jint length, jint timeout) {
    struct libusb_device_handle *deviceHandle = (struct libusb_device_handle *) (*env)->GetDirectBufferAddress(env,
                                                                                                               device);
    jbyte *buffer = NULL;
    if (buffer_) {
        buffer = (jbyte *) (*env)->GetPrimitiveArrayCritical(env, buffer_, NULL);
    }
    jint transfered;
    jint result = libusb_interrupt_transfer(deviceHandle, endpoint, buffer + offset, length, &transfered, timeout);
    if (buffer) {
        (*env)->ReleasePrimitiveArrayCritical(env, buffer_, buffer, 0);
    }
    return ((result == 0) ? transfered : result);
}

JNIEXPORT jint JNICALL
Java_com_jwoolston_android_libusb_UsbDeviceConnection_nativeInterruptRequestAsync(JNIEnv *env, jobject instance,
                                                                                  jobject callback,
                                                                                  jobject device, jint address,
                                                                                  jbyteArray buffer_, jint offset,
                                                                                  jint length, jint timeout) {
    struct libusb_device_handle *deviceHandle = (struct libusb_device_handle *) (*env)->GetDirectBufferAddress(env,
                                                                                                               device);
    if (usbi_handling_events(HANDLE_CTX(deviceHandle))) {
        return LIBUSB_ERROR_BUSY;
    }

    // Allocate the transfer
    struct libusb_transfer *transfer = libusb_alloc_transfer(0);
    jbyte *buffer = NULL;

    unsigned char *userData = (unsigned char *) malloc((length) * sizeof(unsigned char));
    if (!userData) {
        libusb_free_transfer(transfer);
        return LIBUSB_ERROR_NO_MEM;
    }

    // Fill the data buffer if outgoing transfer
    if ((address & LIBUSB_ENDPOINT_DIR_MASK) == LIBUSB_ENDPOINT_OUT) {
        if (buffer_) {
            buffer = (jbyte *) (*env)->GetPrimitiveArrayCritical(env, buffer_, NULL);
        }
        memcpy(buffer + offset, userData, length);
        if (buffer) {
            (*env)->ReleasePrimitiveArrayCritical(env, buffer_, buffer, 0);
        }
    }

    // Populate the transfer structure
    struct transfer_callback_holder *holder = allocate_callback_holder(callback, env, true);

    libusb_fill_interrupt_transfer(transfer, deviceHandle, address, userData, length, libusb_transfer_callback, holder,
                                   timeout);
    transfer->type = LIBUSB_TRANSFER_TYPE_INTERRUPT;
    // Submit the transfer
    int result = libusb_submit_transfer(transfer);
    if (result < 0) {
        libusb_free_transfer(transfer);
        return result;
    }
}

JNIEXPORT jint JNICALL
Java_com_jwoolston_android_libusb_UsbDeviceConnection_nativeResetDevice(JNIEnv *env, jobject instance, jobject device) {
    struct libusb_device_handle *deviceHandle = (struct libusb_device_handle *) (*env)->GetDirectBufferAddress(env,
                                                                                                               device);
    return libusb_reset_device(deviceHandle);
}

#pragma clang diagnostic pop