package com.jwoolston.libusb;

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

import com.jwoolston.libusb.util.Preconditions;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.nio.ByteBuffer;

/**
 * This class represents a USB device attached to the android device with the android device acting as the USB host.
 * Each device contains one or more {@link BaseUsbInterface}s, each of which contains a number of {@link BaseUsbEndpoint}s
 * (the channels via which data is transmitted over USB).
 * <p>
 * <p> This class contains information (along with {@link BaseUsbInterface} and {@link BaseUsbEndpoint}) that describes the
 * capabilities of the USB device. To communicate with the device, you open a {@link BaseUsbDeviceConnection} for the device
 * and use {@code UsbRequest} to send and receive data on an endpoint. {@link BaseUsbDeviceConnection#controlTransfer} is
 * used for control requests on endpoint zero.
 * <p>
 */
public abstract class BaseUsbDevice{

    @NotNull String name;
    @Nullable String manufacturerName;
    @Nullable String productName;
    @NotNull String version;
    @NotNull String serialNumber;
    @NotNull LibusbSpeed speed;

    int vendorId;
    int productId;
    int deviceClass;
    int subclass;
    int protocol;

    int fileDescriptor;

    @NotNull ByteBuffer nativeObject;

    /**
     * All configurations for this device, only null during creation
     */
    @Nullable BaseUsbConfiguration[] configurations;
    /**
     * All interfaces on the device. Initialized on first call to getInterfaceList
     */
    @Nullable BaseUsbInterface[]     interfaces;

    abstract BaseUsbConfiguration createConfiguration(int id, @Nullable String name, int attributes,
                                                      int maxPower);

    abstract BaseUsbInterface createInterface(int id, int alternateSetting, @Nullable String name,
                                              int interfaceClass, int subClass, int protocol);

    abstract BaseUsbEndpoint createEndpoint(int address, int attributes, int maxPacketSize, int interval);

    public int getFileDescriptor() {
        return fileDescriptor;
    }

    /**
     * Returns the name of the device. In the standard implementation, this is the path of the device file for the
     * device in the usbfs file system.
     *
     * @return the device name
     */
    @NotNull
    public String getDeviceName() {
        return name;
    }

    /**
     * Returns the manufacturer name of the device.
     *
     * @return the manufacturer name, or {@code null} if the property could not be read
     */
    @Nullable
    public String getManufacturerName() {
        return manufacturerName;
    }

    /**
     * Returns the product name of the device.
     *
     * @return the product name, or {@code null} if the property could not be read
     */
    @Nullable
    public String getProductName() {
        return productName;
    }

    /**
     * Returns the version number of the device.
     *
     * @return the device version
     */
    @NotNull
    public String getVersion() {
        return version;
    }

    /**
     * Returns the serial number of the device.
     *
     * @return the serial number name, or {@code null} if the property could not be read
     */
    @NotNull
    public String getSerialNumber() {
        return serialNumber;
    }

    /**
     * Returns the connection speed of the device.
     *
     * @return the connection speed.
     */
    @NotNull
    public LibusbSpeed getDeviceSpeed() {
        return speed;
    }

    /**
     * Returns a vendor ID for the device.
     *
     * @return the device vendor ID
     */
    public int getVendorId() {
        return vendorId;
    }

    /**
     * Returns a product ID for the device.
     *
     * @return the device product ID
     */
    public int getProductId() {
        return productId;
    }

    /**
     * Returns the devices's class field.
     * Some useful constants for USB device classes can be found in {@link UsbConstants}.
     *
     * @return the devices's class
     */
    public int getDeviceClass() {
        return deviceClass;
    }

    /**
     * Returns the device's subclass field.
     *
     * @return the device's subclass
     */
    public int getDeviceSubclass() {
        return subclass;
    }

    /**
     * Returns the device's protocol field.
     *
     * @return the device's protocol
     */
    public int getDeviceProtocol() {
        return protocol;
    }

    /**
     * Returns the number of {@link BaseUsbConfiguration}s this device contains.
     *
     * @return the number of configurations
     */
    public int getConfigurationCount() {
        return configurations.length;
    }

    /**
     * Returns the {@link BaseUsbConfiguration} at the given index.
     *
     * @return the configuration
     */
    @NotNull
    public BaseUsbConfiguration getConfiguration(int index) {
        return configurations[index];
    }

    @NotNull
    private BaseUsbInterface[] getInterfaceList() {
        if (interfaces == null) {
            int configurationCount = configurations.length;
            int interfaceCount = 0;
            for (int i = 0; i < configurationCount; i++) {
                BaseUsbConfiguration configuration = configurations[i];
                interfaceCount += configuration.getInterfaceCount();
            }
            interfaces = new BaseUsbInterface[interfaceCount];
            int offset = 0;
            for (int i = 0; i < configurationCount; i++) {
                BaseUsbConfiguration configuration = configurations[i];
                interfaceCount = configuration.getInterfaceCount();
                for (int j = 0; j < interfaceCount; j++) {
                    interfaces[offset++] = configuration.getInterface(j);
                }
            }
        }
        return interfaces;
    }

    /**
     * Returns the number of {@link BaseUsbInterface}s this device contains. For devices with multiple configurations,
     * you will probably want to use {@link BaseUsbConfiguration#getInterfaceCount} instead.
     *
     * @return the number of interfaces
     */
    public int getInterfaceCount() {
        return getInterfaceList().length;
    }

    /**
     * Returns the {@link BaseUsbInterface} at the given index. For devices with multiple configurations, you will
     * probably want to use {@link BaseUsbConfiguration#getInterface} instead.
     *
     * @return the interface
     */
    @NotNull
    public BaseUsbInterface getInterface(int index) {
        return getInterfaceList()[index];
    }

    @Override
    public boolean equals(Object o) {
        if (o instanceof BaseUsbDevice) {
            return ((BaseUsbDevice) o).name.equals(name);
        } else {
            return (o instanceof String && ((String) o).equals(name));
        }
    }

    @Override
    public int hashCode() {
        return name.hashCode();
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder("BaseUsbDevice[name=" + name +
                                                  ",vendorId=" + vendorId + ",productId=" + productId +
                                                  ",deviceClass=" + deviceClass + ",subclass=" + subclass
                                                  + ",protocol=" + protocol +
                                                  ",manufacturerName=" + manufacturerName + ",productName="
                                                  + productName
                                                  +
                                                  ",version=" + version + ",serialNumber=" + serialNumber
                                                  + ",configurations=[");
        if (configurations != null) {
            for (BaseUsbConfiguration configuration : configurations) {
                builder.append("\n");
                builder.append(configuration.toString());
            }
        }
        builder.append("]");
        return builder.toString();
    }

    /**
     * Retrieves the {@link ByteBuffer} pointing to a {@code libusb_device_handle} instance in native.
     *
     * @return The {@link ByteBuffer} pointing to a {@code libusb_device_handle} instance in native.
     */
    @NotNull
    public ByteBuffer getNativeObject() {
        return nativeObject;
    }

    /**
     * Populates the internal data structures of this device which include the {@link BaseUsbConfiguration}s,
     * the {@link BaseUsbInterface}s and {@link BaseUsbEndpoint}s.
     */
    void populate() {
        final int numConfigurations = nativeGetConfigurationCount(getNativeObject());
        @SuppressWarnings({"unchecked", "ConstantConditions"})
        final BaseUsbConfiguration[] configurations = new BaseUsbConfiguration[numConfigurations];
        for (int i = 0; i < numConfigurations; ++i) {
            configurations[i] = BaseUsbConfiguration.fromNativeObject(this, i);
        }
        setConfigurations(configurations);
    }

    /**
     * Sets the available configurations for this device. Only expected to be called by the {@link BaseUsbDevice#populate()}
     * method.
     *
     * @param configuration Array of {@link BaseUsbConfiguration}s. Must not be {@code null} or contain {@code null}s.
     */
    void setConfigurations(@NotNull BaseUsbConfiguration[] configuration) {
        configurations = Preconditions.checkArrayElementsNotNull(configuration, "configuration");
    }

    /**
     * Retrieves a string descriptor from the device.
     *
     * @param device {@link ByteBuffer} pointing to a {@code libusb_device_handle} instance in native. Provided by
     *               {@link BaseUsbDevice#getNativeObject()}.
     * @param index  {@code int} The string index to retrieve. A value of 0 will  cause {@code null} to be returned.
     *
     * @return {@link String} The descriptor or null if one is not present on the device.
     */
    @Nullable
    static native String nativeGetStringDescriptor(@NotNull ByteBuffer device, int index);

    /**
     * Creates a {@code libusb_device_handle} native instance for the give file descriptor. On Android This file
     * descriptor must be provided by {@code android.hardware.usb.BaseUsbDeviceConnection#getFileDescriptor()} in
     * order to have proper permissions.
     *
     * @param context {@link ByteBuffer} pointing to a {@code libusb_context} instance in native.
     * @param fd      {@code int} The file descriptor for the opened device.
     *
     * @return {@link ByteBuffer} pointing to a {@code libusb_device_handle} instance in native, or {@code null} if a
     * failure occurred.
     */
    @Nullable
    static native ByteBuffer wrapDevice(@NotNull ByteBuffer context, int fd);

    /**
     * Retrieves the manufacturer name string from the device.
     *
     * @param device     {@link ByteBuffer} pointing to a {@code libusb_device_handle} instance in native. Provided
     *                   by {@link BaseUsbDevice#getNativeObject()}.
     * @param descriptor {@link ByteBuffer} pointing to a {@code libusb_device_descriptor} instanace in native.
     *                   Provided by {@link LibUsbDeviceDescriptor#getNativeObject()}.
     *
     * @return {@link String} The device manufacturer name.
     */
    native String nativeGetManufacturerString(@NotNull ByteBuffer device, @NotNull ByteBuffer descriptor);

    /**
     * Retrieves the product name string from the device.
     *
     * @param device     {@link ByteBuffer} pointing to a {@code libusb_device_handle} instance in native. Provided
     *                   by {@link BaseUsbDevice#getNativeObject()}.
     * @param descriptor {@link ByteBuffer} pointing to a {@code libusb_device_descriptor} instanace in native.
     *                   Provided by {@link LibUsbDeviceDescriptor#getNativeObject()}.
     *
     * @return {@link String} The device product name.
     */
    native String nativeGetProductNameString(@NotNull ByteBuffer device, @NotNull ByteBuffer descriptor);

    /**
     * Retrieves the product version number for the device.
     *
     * @param descriptor {@link ByteBuffer} pointing to a {@code libusb_device_descriptor} instanace in native.
     *                   Provided by {@link LibUsbDeviceDescriptor#getNativeObject()}.
     *
     * @return {@link String} The device product version.
     */
    native String nativeGetDeviceVersion(@NotNull ByteBuffer descriptor);

    /**
     * Retrieves the connection speed for the device.
     *
     * @param device     {@link ByteBuffer} pointing to a {@code libusb_device_handle} instance in native. Provided
     *                   by {@link BaseUsbDevice#getNativeObject()}.
     * @param descriptor {@link ByteBuffer} pointing to a {@code libusb_device_descriptor} instanace in native.
     *                                     Provided by {@link LibUsbDeviceDescriptor#getNativeObject()}.
     *
     * @return
     */
    native int nativeGetDeviceSpeed(@NotNull ByteBuffer device, @NotNull ByteBuffer descriptor);

    /**
     * Retrieves the number of configurations available on the device.
     *
     * @param device {@link ByteBuffer} pointing to a {@code libusb_device_handle} instance in native. Provided by
     *               {@link BaseUsbDevice#getNativeObject()}.
     *
     * @return {@code int} The number of configurations.
     */
    private native int nativeGetConfigurationCount(@NotNull ByteBuffer device);

    /**
     * Retrieves the pointer to a {@code libusb_device_handle} instance in native. This is useful when
     * parceling/serializing the {@link BaseUsbDevice}
     * as the {@link ByteBuffer} we normally use cannot be serialized and still point to the same instance.
     *
     * @param device {@link ByteBuffer} pointing to a {@code libusb_device_handle} instance in native. Provided by
     *               {@link BaseUsbDevice#getNativeObject()}.
     *
     * @return {@code long} The native pointer.
     */
    native long nativeGetPointerFromNativeObject(@NotNull ByteBuffer device);

    /**
     * Converts a native pointer into a {@link ByteBuffer} wrapping a {@code libusb_device_handle} instance in native
     * . This is
     * useful when constructing a {@link BaseUsbDevice} from serialization or parcel as only the pointer can be stored.
     *
     * @param pointer {@code long} The native pointer.
     *
     * @return {@link ByteBuffer} pointing to a {@code libusb_device_handle} instance in native. Provided by
     * {@link BaseUsbDevice#getNativeObject()}.
     */
    @Nullable
    native ByteBuffer nativeGetNativeObjectFromPointer(long pointer);


}
