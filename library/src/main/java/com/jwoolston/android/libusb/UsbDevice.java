package com.jwoolston.android.libusb;

/*
 * Copyright (C) 2010 The Android Open Source Project
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

import android.os.Parcel;
import android.os.Parcelable;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.jwoolston.android.libusb.util.Preconditions;

import java.nio.ByteBuffer;

/**
 * This class represents a USB device attached to the android device with the android device
 * acting as the USB host.
 * Each device contains one or more {@link UsbInterface}s, each of which contains a number of
 * {@link UsbEndpoint}s (the channels via which data is transmitted over USB).
 * <p>
 * <p> This class contains information (along with {@link UsbInterface} and {@link UsbEndpoint})
 * that describes the capabilities of the USB device.
 * To communicate with the device, you open a {@link UsbDeviceConnection} for the device
 * and use {@link UsbRequest} to send and receive data on an endpoint.
 * {@link UsbDeviceConnection#controlTransfer} is used for control requests on endpoint zero.
 * <p>
 * <div class="special reference">
 * <h3>Developer Guides</h3>
 * <p>For more information about communicating with USB hardware, read the
 * <a href="{@docRoot}guide/topics/connectivity/usb/index.html">USB</a> developer guide.</p>
 * </div>
 */
public class UsbDevice {

    private static final String TAG = "UsbDevice";
    private static final boolean DEBUG = false;

    @NonNull
    private final android.hardware.usb.UsbDevice mDevice;

    @NonNull
    private final String mName;
    @Nullable
    private final String mManufacturerName;
    @Nullable
    private final String mProductName;
    @NonNull
    private final String mVersion;
    @Nullable
    private final String mSerialNumber;
    private final int mVendorId;
    private final int mProductId;
    private final int mClass;
    private final int mSubclass;
    private final int mProtocol;

    @NonNull
    private final ByteBuffer nativeObject;

    /** All configurations for this device, only null during creation */
    @Nullable
    private Parcelable[] mConfigurations;
    /** All interfaces on the device. Initialized on first call to getInterfaceList */
    @Nullable
    private UsbInterface[] mInterfaces;

    @Nullable
    private static native ByteBuffer wrapDevice(@NonNull ByteBuffer context, int fd);

    private native String nativeGetManufacturerString(@NonNull ByteBuffer device, @NonNull ByteBuffer descriptor);

    private native String nativeGetProductNameString(@NonNull ByteBuffer device, @NonNull ByteBuffer descriptor);

    public native String nativeGetDeviceVersion(@NonNull ByteBuffer device, @NonNull ByteBuffer descriptor);

    static UsbDevice fromAndroidDevice(@NonNull LibUsbContext context, @NonNull android.hardware.usb.UsbDevice device,
                                       @NonNull android.hardware.usb.UsbDeviceConnection connection) {
        return new UsbDevice(connection, device, wrapDevice(context.getNativeObject(), connection.getFileDescriptor()));
    }

    private UsbDevice(@NonNull android.hardware.usb.UsbDeviceConnection connection,
                      @NonNull android.hardware.usb.UsbDevice device, ByteBuffer nativeObject) {
        Preconditions.checkNotNull(nativeObject, "UsbDevice initialization failed.");
        this.nativeObject = nativeObject;
        mDevice = device;
        mName = device.getDeviceName();
        mVendorId = device.getVendorId();
        mProductId = device.getProductId();
        mClass = device.getDeviceClass();
        mSubclass = device.getDeviceSubclass();
        mProtocol = device.getDeviceProtocol();

        LibUsbDeviceDescriptor descriptor = LibUsbDeviceDescriptor.getDeviceDescriptor(this);
        mManufacturerName = nativeGetManufacturerString(nativeObject, descriptor.getNativeObject());
        mProductName = nativeGetProductNameString(nativeObject, descriptor.getNativeObject());
        mVersion = nativeGetDeviceVersion(nativeObject, descriptor.getNativeObject());
        mSerialNumber = connection.getSerial();
    }

    @NonNull
    ByteBuffer getNativeObject() {
        return nativeObject;
    }

    /**
     * Returns the name of the device.
     * In the standard implementation, this is the path of the device file
     * for the device in the usbfs file system.
     *
     * @return the device name
     */
    public @NonNull
    String getDeviceName() {
        return mName;
    }

    /**
     * Returns the manufacturer name of the device.
     *
     * @return the manufacturer name, or {@code null} if the property could not be read
     */
    public @Nullable
    String getManufacturerName() {
        return mManufacturerName;
    }

    /**
     * Returns the product name of the device.
     *
     * @return the product name, or {@code null} if the property could not be read
     */
    public @Nullable
    String getProductName() {
        return mProductName;
    }

    /**
     * Returns the version number of the device.
     *
     * @return the device version
     */
    public @NonNull
    String getVersion() {
        return mVersion;
    }

    /**
     * Returns the serial number of the device.
     *
     * @return the serial number name, or {@code null} if the property could not be read
     */
    public @Nullable
    String getSerialNumber() {
        return mSerialNumber;
    }

    /**
     * Returns a unique integer ID for the device.
     * This is a convenience for clients that want to use an integer to represent
     * the device, rather than the device name.
     * IDs are not persistent across USB disconnects.
     *
     * @return the device ID
     */
    public int getDeviceId() {
        return mDevice.getDeviceId();
    }

    /**
     * Returns a vendor ID for the device.
     *
     * @return the device vendor ID
     */
    public int getVendorId() {
        return mVendorId;
    }

    /**
     * Returns a product ID for the device.
     *
     * @return the device product ID
     */
    public int getProductId() {
        return mProductId;
    }

    /**
     * Returns the devices's class field.
     * Some useful constants for USB device classes can be found in {@link UsbConstants}.
     *
     * @return the devices's class
     */
    public int getDeviceClass() {
        return mClass;
    }

    /**
     * Returns the device's subclass field.
     *
     * @return the device's subclass
     */
    public int getDeviceSubclass() {
        return mSubclass;
    }

    /**
     * Returns the device's protocol field.
     *
     * @return the device's protocol
     */
    public int getDeviceProtocol() {
        return mProtocol;
    }

    /**
     * Returns the number of {@link UsbConfiguration}s this device contains.
     *
     * @return the number of configurations
     */
    public int getConfigurationCount() {
        return mConfigurations.length;
    }

    /**
     * Returns the {@link UsbConfiguration} at the given index.
     *
     * @return the configuration
     */
    public @NonNull
    UsbConfiguration getConfiguration(int index) {
        return (UsbConfiguration) mConfigurations[index];
    }

    private @Nullable
    UsbInterface[] getInterfaceList() {
        if (mInterfaces == null) {
            int configurationCount = mConfigurations.length;
            int interfaceCount = 0;
            for (int i = 0; i < configurationCount; i++) {
                UsbConfiguration configuration = (UsbConfiguration) mConfigurations[i];
                interfaceCount += configuration.getInterfaceCount();
            }
            mInterfaces = new UsbInterface[interfaceCount];
            int offset = 0;
            for (int i = 0; i < configurationCount; i++) {
                UsbConfiguration configuration = (UsbConfiguration) mConfigurations[i];
                interfaceCount = configuration.getInterfaceCount();
                for (int j = 0; j < interfaceCount; j++) {
                    mInterfaces[offset++] = configuration.getInterface(j);
                }
            }
        }
        return mInterfaces;
    }

    /**
     * Returns the number of {@link UsbInterface}s this device contains.
     * For devices with multiple configurations, you will probably want to use
     * {@link UsbConfiguration#getInterfaceCount} instead.
     *
     * @return the number of interfaces
     */
    public int getInterfaceCount() {
        return getInterfaceList().length;
    }

    /**
     * Returns the {@link UsbInterface} at the given index.
     * For devices with multiple configurations, you will probably want to use
     * {@link UsbConfiguration#getInterface} instead.
     *
     * @return the interface
     */
    public @NonNull
    UsbInterface getInterface(int index) {
        return getInterfaceList()[index];
    }

    /**
     * Only used by UsbService implementation
     *
     * @hide
     */
    public void setConfigurations(@NonNull Parcelable[] configuration) {
        mConfigurations = Preconditions.checkArrayElementsNotNull(configuration, "configuration");
    }

    @Override
    public boolean equals(Object o) {
        if (o instanceof UsbDevice) {
            return ((UsbDevice) o).mName.equals(mName);
        } else if (o instanceof String) {
            return ((String) o).equals(mName);
        } else {
            return false;
        }
    }

    @Override
    public int hashCode() {
        return mName.hashCode();
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder("UsbDevice[mName=" + mName +
            ",mVendorId=" + mVendorId + ",mProductId=" + mProductId +
            ",mClass=" + mClass + ",mSubclass=" + mSubclass + ",mProtocol=" + mProtocol +
            ",mManufacturerName=" + mManufacturerName + ",mProductName=" + mProductName +
            ",mVersion=" + mVersion + ",mSerialNumber=" + mSerialNumber + ",mConfigurations=[");
        if (mConfigurations != null) {
            for (int i = 0; i < mConfigurations.length; i++) {
                builder.append("\n");
                builder.append(mConfigurations[i].toString());
            }
        }
        builder.append("]");
        return builder.toString();
    }
}
