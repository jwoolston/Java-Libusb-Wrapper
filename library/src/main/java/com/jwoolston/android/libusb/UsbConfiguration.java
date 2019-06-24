/*
 * Copyright (C) 2014 The Android Open Source Project
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

import android.os.Parcel;
import android.os.Parcelable;

import com.jwoolston.android.libusb.util.Preconditions;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

/**
 * A class representing a configuration on a {@link UsbDevice}. A USB configuration can have one or more interfaces,
 * each one providing a different piece of functionality, separate from the other interfaces. An interface will have
 * one or more {@link UsbEndpoint}s, which are the channels by which the host transfers data with the device.
 */
public class UsbConfiguration implements Parcelable {

    /**
     * Mask for "self-powered" bit in the configuration's attributes.
     */
    private static final int ATTR_SELF_POWERED  = 1 << 6;

    /**
     * Mask for "remote wakeup" bit in the configuration's attributes.
     */
    private static final int ATTR_REMOTE_WAKEUP = 1 << 5;

    private final int    id;
    @Nullable
    private final String name;
    private final int    attributes;
    private final int    maxPower;

    /**
     * All interfaces for this config, only null during creation
     */
    @Nullable
    private Parcelable[] interfaces;

    /**
     * UsbConfiguration should only be instantiated by UsbService implementation
     */
    public UsbConfiguration(int id, @Nullable String name, int attributes, int maxPower) {
        this.id = id;
        this.name = name;
        this.attributes = attributes;
        this.maxPower = maxPower;
    }

    /**
     * Returns the configuration's ID field.
     * This is an integer that uniquely identifies the configuration on the device.
     *
     * @return the configuration's ID
     */
    public int getId() {
        return id;
    }

    /**
     * Returns the configuration's name.
     *
     * @return the configuration's name, or {@code null} if the property could not be read
     */
    public @Nullable
    String getName() {
        return name;
    }

    /**
     * Returns the self-powered attribute value configuration's attributes field.
     * This attribute indicates that the device has a power source other than the USB connection.
     *
     * @return the configuration's self-powered attribute
     */
    public boolean isSelfPowered() {
        return (attributes & ATTR_SELF_POWERED) != 0;
    }

    /**
     * Returns the remote-wakeup attribute value configuration's attributes field.
     * This attributes that the device may signal the host to wake from suspend.
     *
     * @return the configuration's remote-wakeup attribute
     */
    public boolean isRemoteWakeup() {
        return (attributes & ATTR_REMOTE_WAKEUP) != 0;
    }

    /**
     * Returns the configuration's max power consumption, in milliamps.
     *
     * @return the configuration's max power
     */
    public int getMaxPower() {
        return maxPower * 2;
    }

    /**
     * Returns the number of {@link UsbInterface}s this configuration contains.
     *
     * @return the number of endpoints
     */
    public int getInterfaceCount() {
        return interfaces.length;
    }

    /**
     * Returns the {@link UsbInterface} at the given index.
     *
     * @return the interface
     */
    public @NotNull
    UsbInterface getInterface(int index) {
        return (UsbInterface) interfaces[index];
    }

    /**
     * Only used by UsbService implementation
     */
    public void setInterfaces(@NotNull Parcelable[] interfaces) {
        this.interfaces = Preconditions.checkArrayElementsNotNull(interfaces, "interfaces");
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder("UsbConfiguration[id=" + id +
                                                  ",name=" + name + ",attributes=" + attributes +
                                                  ",maxPower=" + maxPower + ",interfaces=[");
        for (int i = 0; i < interfaces.length; i++) {
            builder.append("\n");
            builder.append(interfaces[i].toString());
        }
        builder.append("]");
        return builder.toString();
    }

    public static final Parcelable.Creator<UsbConfiguration> CREATOR =
            new Parcelable.Creator<UsbConfiguration>() {
                public UsbConfiguration createFromParcel(Parcel in) {
                    int id = in.readInt();
                    String name = in.readString();
                    int attributes = in.readInt();
                    int maxPower = in.readInt();
                    Parcelable[] interfaces = in.readParcelableArray(UsbInterface.class.getClassLoader());
                    UsbConfiguration configuration = new UsbConfiguration(id, name, attributes, maxPower);
                    configuration.setInterfaces(interfaces);
                    return configuration;
                }

                public UsbConfiguration[] newArray(int size) {
                    return new UsbConfiguration[size];
                }
            };

    public int describeContents() {
        return 0;
    }

    public void writeToParcel(Parcel parcel, int flags) {
        parcel.writeInt(id);
        parcel.writeString(name);
        parcel.writeInt(attributes);
        parcel.writeInt(maxPower);
        parcel.writeParcelableArray(interfaces, 0);
    }

    private static final int INDEX_NUMBER_INTERFACES = 4;
    private static final int INDEX_CONFIGURATION_VALUE = 5;
    private static final int INDEX_CONFIGURATION_STRING_INDEX = 6;
    private static final int INDEX_ATTRIBUTES = 7;
    private static final int INDEX_MAX_POWER = 8;

    @NotNull
    static UsbConfiguration fromNativeObject(@NotNull UsbDevice device, int configuration) {
        // Get the native configuration object. Make sure you free it!
        final ByteBuffer nativeObject = nativeGetConfiguration(device.getNativeObject(), configuration);
        final int numberInterfaces = 0xFF & nativeObject.get(INDEX_NUMBER_INTERFACES);
        final int id = 0xFF & nativeObject.get(INDEX_CONFIGURATION_VALUE);
        final int stringIndex = 0xFF & nativeObject.get(INDEX_CONFIGURATION_STRING_INDEX);
        final int attributes = 0xFF & nativeObject.get(INDEX_ATTRIBUTES);
        final int maxPower = 0xFF & nativeObject.get(INDEX_MAX_POWER);
        final String name = UsbDevice.nativeGetStringDescriptor(device.getNativeObject(), stringIndex);

        final UsbConfiguration usbConfiguration = new UsbConfiguration(id, name, attributes, maxPower);
        final List<UsbInterface> usbInterfaces = new ArrayList<>();
        for (int i = 0; i < numberInterfaces; ++i) {
            // This is of type struct libusb_interface
            final ByteBuffer nativeInterface = nativeGetInterface(nativeObject, i);
            List<UsbInterface> usbInterface = UsbInterface.fromNativeObject(device, nativeInterface);
            usbInterfaces.addAll(usbInterface);
        }
        usbConfiguration.setInterfaces(usbInterfaces.toArray(new UsbInterface[0]));

        // Destroy the native configuration object
        nativeDestroy(nativeObject);
        return usbConfiguration;
    }

    private static native ByteBuffer nativeGetConfiguration(@NotNull ByteBuffer device, int configuration);

    /**
     *
     * @param nativeObject {@link ByteBuffer} wrapper to native stuct. Expected to be a libusb_config_descriptor.
     * @param interfaceIndex
     * @return
     */
    private static native ByteBuffer nativeGetInterface(@NotNull ByteBuffer nativeObject, int interfaceIndex);

    private static native void nativeDestroy(@NotNull ByteBuffer nativeObject);
}