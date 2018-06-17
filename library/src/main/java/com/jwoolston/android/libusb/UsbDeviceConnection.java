/*
 * Copyright (C) 2011 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.jwoolston.android.libusb;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import java.nio.ByteBuffer;

/**
 * This class is used for sending and receiving data and control messages to a USB device. nstances of this class are
 * created by {@link UsbManager#registerDevice(android.hardware.usb.UsbDevice)}.
 */
public class UsbDeviceConnection {

    private static final String TAG = "UsbDeviceConnection";

    private final UsbManager manager;
    private final UsbDevice  device;

    private Context context;

    @NonNull
    static UsbDeviceConnection fromAndroidConnection(@NonNull Context context, @NonNull UsbManager manager,
                                                     @NonNull UsbDevice device) {
        return new UsbDeviceConnection(context, manager, device);
    }

    /**
     * UsbDevice should only be instantiated by UsbService implementation
     */
    private UsbDeviceConnection(@NonNull Context context, @NonNull UsbManager manager, @NonNull UsbDevice device) {
        this.context = context;
        this.manager = manager;
        this.device = device;
    }

    /**
     * @return The application context the connection was created for.
     */
    @Nullable
    public Context getContext() {
        return context;
    }

    /**
     * Releases all system resources related to the device. Once the object is closed it cannot be used again. The
     * client must call {@link UsbManager#registerDevice(android.hardware.usb.UsbDevice)} again to retrieve a new
     * instance to reestablish communication with the device.
     */
    public void close() {
        nativeClose(device.getNativeObject());
        manager.unregisterDevice(device);
    }

    /**
     * Returns the native file descriptor for the device, or -1 if the device is not opened. This is intended for
     * passing to native code to access the device.
     *
     * @return the native file descriptor
     */
    public int getFileDescriptor() {
        return device.getFileDescriptor();
    }

    /**
     * Returns the raw USB descriptors for the device. This can be used to access descriptors not supported directly
     * via the higher level APIs.
     *
     * @return raw USB descriptors
     */
    @Nullable
    public byte[] getRawDescriptors() {
        return nativeGetRawDescriptor(device.getFileDescriptor());
    }

    /**
     * Claims exclusive access to a {@link UsbInterface}. This must be done before sending or receiving data on any
     * {@link UsbEndpoint}s belonging to the interface.
     *
     * @param intf  the interface to claim
     * @param force true to disconnect kernel driver if necessary
     *
     * @return {@link LibusbError} The libusb result.
     */
    public LibusbError claimInterface(UsbInterface intf, boolean force) {
        return LibusbError.fromNative(nativeClaimInterface(device.getNativeObject(), intf.getId(), force));
    }

    /**
     * Releases exclusive access to a {@link UsbInterface}.
     *
     * @return {@link LibusbError} The libusb result.
     */
    public LibusbError releaseInterface(UsbInterface intf) {
        return LibusbError.fromNative(nativeReleaseInterface(device.getNativeObject(), intf.getId()));
    }

    /**
     * Sets the current {@link UsbInterface}. Used to select between two interfaces with the same ID but different
     * alternate setting.
     *
     * @return {@link LibusbError} The libusb result.
     */
    public LibusbError setInterface(UsbInterface intf) {
        return LibusbError.fromNative(nativeSetInterface(device.getNativeObject(), intf.getId(),
                                                         intf.getAlternateSetting()));
    }

    /**
     * Sets the device's current {@link UsbConfiguration}.
     *
     * @return {@link LibusbError} The libusb result.
     */
    public LibusbError setConfiguration(UsbConfiguration configuration) {
        return LibusbError.fromNative(nativeSetConfiguration(device.getNativeObject(), configuration.getId()));
    }

    /**
     * Performs a control transaction on endpoint zero for this device. The direction of the transfer is determined
     * by the request type. If requestType & {@link UsbConstants#USB_ENDPOINT_DIR_MASK} is
     * {@link UsbConstants#USB_DIR_OUT}, then the transfer is a write, and if it is {@link UsbConstants#USB_DIR_IN},
     * then the transfer is a read.
     * <p>
     * This method transfers data starting from index 0 in the buffer. To specify a different offset, use
     * {@link #controlTransfer(int, int, int, int, byte[], int, int, int)}.
     * </p>
     *
     * @param requestType request type for this transaction
     * @param request     request ID for this transaction
     * @param value       value field for this transaction
     * @param index       index field for this transaction
     * @param buffer      buffer for data portion of transaction,
     *                    or null if no data needs to be sent or received
     * @param length      the length of the data to send or receive
     * @param timeout     in milliseconds
     *
     * @return length of data transferred (or zero) for success, or negative value for failure
     */
    public int controlTransfer(int requestType, int request, int value, int index, byte[] buffer, int length,
                               int timeout) {
        return controlTransfer(requestType, request, value, index, buffer, 0, length, timeout);
    }

    /**
     * Performs a control transaction on endpoint zero for this device. The direction of the transfer is determined
     * by the request type. If requestType & {@link UsbConstants#USB_ENDPOINT_DIR_MASK} is
     * {@link UsbConstants#USB_DIR_OUT}, then the transfer is a write, and if it is {@link UsbConstants#USB_DIR_IN},
     * then the transfer is a read.
     *
     * @param requestType request type for this transaction
     * @param request     request ID for this transaction
     * @param value       value field for this transaction
     * @param index       index field for this transaction
     * @param buffer      buffer for data portion of transaction,
     *                    or null if no data needs to be sent or received
     * @param offset      the index of the first byte in the buffer to send or receive
     * @param length      the length of the data to send or receive
     * @param timeout     in milliseconds
     *
     * @return length of data transferred (or zero) for success, or negative value for failure
     */
    public int controlTransfer(int requestType, int request, int value, int index, byte[] buffer, int offset,
                               int length, int timeout) {
        checkBounds(buffer, offset, length);
        return nativeControlRequest(device.getNativeObject(), requestType, request, value, index, buffer, offset,
                                    length, timeout);
    }

    /**
     * Performs a bulk transaction on the given endpoint. The direction of the transfer is determined by the
     * direction of the endpoint.
     * <p>
     * This method transfers data starting from index 0 in the buffer. To specify a different offset, use
     * {@link #bulkTransfer(UsbEndpoint, byte[], int, int, int)}.
     * </p>
     *
     * @param endpoint the endpoint for this transaction
     * @param buffer   buffer for data to send or receive; can be {@code null} to wait for next
     *                 transaction without reading data
     * @param length   the length of the data to send or receive
     * @param timeout  in milliseconds, 0 is infinite
     *
     * @return length of data transferred (or zero) for success, or negative value for failure
     */
    public int bulkTransfer(UsbEndpoint endpoint, byte[] buffer, int length, int timeout) {
        return bulkTransfer(endpoint, buffer, 0, length, timeout);
    }

    /**
     * Performs a bulk transaction on the given endpoint. The direction of the transfer is determined by the
     * direction of the endpoint.
     *
     * @param endpoint the endpoint for this transaction
     * @param buffer   buffer for data to send or receive
     * @param offset   the index of the first byte in the buffer to send or receive
     * @param length   the length of the data to send or receive
     * @param timeout  in milliseconds, 0 is infinite
     *
     * @return length of data transferred (or zero) for success, or negative value for failure
     */
    public int bulkTransfer(UsbEndpoint endpoint, byte[] buffer, int offset, int length, int timeout) {
        checkBounds(buffer, offset, length);
        return nativeBulkRequest(device.getNativeObject(), endpoint.getAddress(), buffer, offset, length, timeout);
    }

    /**
     * Reset USB port for the connected device.
     *
     * @return {@link LibusbError} The libusb result.
     */
    public LibusbError resetDevice() {
        return LibusbError.fromNative(nativeResetDevice(device.getNativeObject()));
    }

    /**
     * Returns the serial number for the device.
     *
     * @return {@link String } The device serial number
     */
    @NonNull
    public String getSerial() {
        return device.getSerialNumber();
    }

    private static void checkBounds(byte[] buffer, int start, int length) {
        final int bufferLength = (buffer != null ? buffer.length : 0);
        if (length < 0 || start < 0 || start + length > bufferLength) {
            throw new IllegalArgumentException("Buffer start or length out of bounds.");
        }
    }

    private native void nativeClose(@NonNull ByteBuffer device);

    @Nullable
    private native byte[] nativeGetRawDescriptor(int fd);

    private native int nativeClaimInterface(@NonNull ByteBuffer device, int interfaceID, boolean force);

    private native int nativeReleaseInterface(@NonNull ByteBuffer device, int interfaceID);

    private native int nativeSetInterface(@NonNull ByteBuffer device, int interfaceID, int alternateSetting);

    private native int nativeSetConfiguration(@NonNull ByteBuffer device, int configurationID);

    private native int nativeControlRequest(@NonNull ByteBuffer device, int requestType, int request, int value,
                                            int index, byte[] buffer, int offset, int length, int timeout);

    private native int nativeBulkRequest(@NonNull ByteBuffer device, int endpoint, byte[] buffer, int offset,
                                         int length, int timeout);

    private native int nativeResetDevice(@NonNull ByteBuffer device);
}