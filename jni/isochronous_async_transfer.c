//
// Created by Jared Woolston (Jared.Woolston@gmail.com)
//

#include <common.h>

#define  LOG_TAG    "IsochronousAsyncTransfer-Native"

JNIEXPORT jobject JNICALL
Java_com_jwoolston_libusb_async_IsochronousAsyncTransfer_nativeAllocate(JNIEnv *env, jobject instance,
                                                                            jint numberPackets) {
    struct libusb_transfer *transfer = libusb_alloc_transfer(numberPackets);
    transfer->num_iso_packets = numberPackets;
    transfer->type = LIBUSB_TRANSFER_TYPE_ISOCHRONOUS;
    return ((*env)->NewDirectByteBuffer(env, (void *) transfer, sizeof(struct libusb_transfer)));
}

JNIEXPORT jint JNICALL
Java_com_jwoolston_libusb_async_IsochronousAsyncTransfer_nativeSetupPackets(JNIEnv *env, jobject instance,
                                                                                jobject device, jobject nativeObject,
                                                                                jint endpoint, jint packetSize) {
    struct libusb_device_handle *deviceHandle = (struct libusb_device_handle *) (*env)->GetDirectBufferAddress(env,
                                                                                                               device);
    //int size = libusb_get_max_iso_packet_size(deviceHandle->dev, endpoint);
    //TODO: Libusb currently does not use the current alternate setting for this
    //LOGV("ISO Packet Size: %i", packetSize);

    // Check for error condition
    /*if (packetSize < 0) {
        return packetSize;
    }*/

    struct libusb_transfer *transfer = (struct libusb_transfer *) (*env)->GetDirectBufferAddress(env, nativeObject);
    libusb_set_iso_packet_lengths(transfer, (unsigned int) packetSize);

    //return size;
}

JNIEXPORT void JNICALL
Java_com_jwoolston_libusb_async_IsochronousAsyncTransfer_nativeDestroy(JNIEnv *env, jobject instance,
                                                                           jobject nativeObject) {

}