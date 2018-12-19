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
package com.jwoolston.libusb;

import com.jwoolston.libusb.util.Preconditions;
import java.io.Externalizable;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * A class representing an interface on a {@link UsbDevice}. USB devices can have one or more interfaces, each one
 * providing a different piece of functionality, separate from the other interfaces. An interface will have one or
 * more {@link UsbEndpoint}s, which are the channels by which the host transfers data with the device.
 */
public class UsbInterface implements Externalizable {

    private int    id;
    private int    alternateSetting;
    @Nullable
    private String name;
    private int    interfaceClass;
    private int    subclass;
    private int    protocol;

    /** All endpoints of this interface, only null during creation */
    private UsbEndpoint[] endpoints;

    /**
     * Required for externalizable, do not call
     */
    public UsbInterface() {
        // Required for externalizable
    }

    /**
     * UsbInterface should only be instantiated by UsbManager implementation
     */
    UsbInterface(int id, int alternateSetting, @Nullable String name, int interfaceClass, int subClass, int protocol) {
        this.id = id;
        this.alternateSetting = alternateSetting;
        this.name = name;
        this.interfaceClass = interfaceClass;
        subclass = subClass;
        this.protocol = protocol;
    }

    /**
     * Returns the interface's bInterfaceNumber field. This is an integer that along with the alternate setting
     * uniquely identifies the interface on the device.
     *
     * @return the interface's ID
     */
    public int getId() {
        return id;
    }

    /**
     * Returns the interface's bAlternateSetting field. This is an integer that along with the ID uniquely identifies
     * the interface on the device. {@link UsbDeviceConnection#setInterface} can be used to switch between
     * two interfaces with the same ID but different alternate setting.
     *
     * @return the interface's alternate setting
     */
    public int getAlternateSetting() {
        return alternateSetting;
    }

    /**
     * Returns the interface's name.
     *
     * @return the interface's name, or {@code null} if the property could not be read
     */
    public @Nullable
    String getName() {
        return name;
    }

    /**
     * Returns the interface's class field. Some useful constants for USB classes can be found in {@link UsbConstants}
     *
     * @return the interface's class
     */
    public int getInterfaceClass() {
        return interfaceClass;
    }

    /**
     * Returns the interface's subclass field.
     *
     * @return the interface's subclass
     */
    public int getInterfaceSubclass() {
        return subclass;
    }

    /**
     * Returns the interface's protocol field.
     *
     * @return the interface's protocol
     */
    public int getInterfaceProtocol() {
        return protocol;
    }

    /**
     * Returns the number of {@link android.hardware.usb.UsbEndpoint}s this interface contains.
     *
     * @return the number of endpoints
     */
    public int getEndpointCount() {
        return endpoints.length;
    }

    /**
     * Returns the {@link android.hardware.usb.UsbEndpoint} at the given index.
     *
     * @return the endpoint
     */
    public UsbEndpoint getEndpoint(int index) {
        return (UsbEndpoint) endpoints[index];
    }

    /**
     * Only used by UsbManager implementation
     */
    void setEndpoints(UsbEndpoint[] endpoints) {
        this.endpoints = Preconditions.checkArrayElementsNotNull(endpoints, "endpoints");
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder("UsbInterface[id=" + id +
                                                  ",alternateSetting=" + alternateSetting +
                                                  ",name=" + name + ",interfaceClass=" + interfaceClass +
                                                  ",subclass=" + subclass + ",protocol=" + protocol +
                                                  ",endpoints=[");
        if (endpoints != null) {
            for (int i = 0; i < endpoints.length; i++) {
                builder.append("\n");
                builder.append(endpoints[i].toString());
            }
            builder.append("]");
        }
        return builder.toString();
    }

    @Override
    public void writeExternal(ObjectOutput out) throws IOException {
        out.writeInt(id);
        out.writeInt(alternateSetting);
        out.writeUTF(name);
        out.writeInt(interfaceClass);
        out.writeInt(subclass);
        out.writeInt(protocol);
        out.writeInt(endpoints.length);
        for (UsbEndpoint endpoint : endpoints) {
            out.writeObject(endpoint);
        }
    }

    @Override
    public void readExternal(ObjectInput in) throws ClassNotFoundException, IOException {
        id = in.readInt();
        alternateSetting = in.readInt();
        name = in.readUTF();
        interfaceClass = in.readInt();
        subclass = in.readInt();
        protocol = in.readInt();
        int endpointCount = in.readInt();
        UsbEndpoint[] endpoints = new UsbEndpoint[endpointCount];
        for (int i = 0; i < endpointCount; ++i) {
            endpoints[i] = (UsbEndpoint) in.readObject();
        }
        setEndpoints(endpoints);
    }

    private static final int INDEX_INTERFACE_ID = 2;
    private static final int INDEX_ALTERNATE_SETTING = 3;
    private static final int INDEX_NUM_ENDPOINTS = 4;
    private static final int INDEX_INTERFACE_CLASS = 5;
    private static final int INDEX_INTERFACE_SUBCLASS = 6;
    private static final int INDEX_INTERFACE_PROTOCOL = 7;
    private static final int INDEX_INTERFACE_STRING_INDEX = 8;

    @NotNull
    static List<UsbInterface> fromNativeObject(@NotNull UsbDevice device, @NotNull ByteBuffer nativeInterface) {
        final List<UsbInterface> interfaces = new ArrayList<>();
        UsbInterface usbInterface = null;
        do {
            usbInterface = fromNativeDescriptor(device, nativeInterface, interfaces.size());
            if (usbInterface != null) {
                interfaces.add(usbInterface);
            }
        } while (usbInterface != null);
        return interfaces;
    }

    @Nullable
    private static UsbInterface fromNativeDescriptor(@NotNull UsbDevice device, @NotNull ByteBuffer nativeObject,
                                                     int index) {
        final ByteBuffer nativeDescriptor = nativeGetInterfaceDescriptor(nativeObject, index);

        if (nativeDescriptor == null) {
            return null;
        }

        final int id = 0xFF & nativeDescriptor.get(INDEX_INTERFACE_ID);
        final int alternateSetting = 0xFF & nativeDescriptor.get(INDEX_ALTERNATE_SETTING);
        final int numEndpoints = 0xFF & nativeDescriptor.get(INDEX_NUM_ENDPOINTS);
        final int interfaceClass = 0xFF & nativeDescriptor.get(INDEX_INTERFACE_CLASS);
        final int subclass = 0xFF & nativeDescriptor.get(INDEX_INTERFACE_SUBCLASS);
        final int protocol = 0xFF & nativeDescriptor.get(INDEX_INTERFACE_PROTOCOL);
        final int stringIndex = 0xFF & nativeDescriptor.get(INDEX_INTERFACE_STRING_INDEX);
        final String name = UsbDevice.nativeGetStringDescriptor(device.getNativeObject(), stringIndex);
        final UsbInterface usbInterface = new UsbInterface(id, alternateSetting, name, interfaceClass, subclass,
                                                          protocol);
        final UsbEndpoint[] endpoints = new UsbEndpoint[numEndpoints];
        for (int i = 0; i < numEndpoints; ++i) {
            final ByteBuffer nativeEndpoint = nativeGetEndpoint(nativeDescriptor, i);
            if (nativeEndpoint != null) {
                endpoints[i] = UsbEndpoint.fromNativeObject(nativeEndpoint);
            } else {
                throw new IllegalStateException("Received a null endpoint when one was expected. Expected index: " +
                                                i + " Expected total: " + numEndpoints);
            }
        }
        usbInterface.setEndpoints(endpoints);
        return usbInterface;
    }

    @Nullable
    private static native ByteBuffer nativeGetInterfaceDescriptor(@NotNull ByteBuffer nativeObject, int index);

    @Nullable
    private static native ByteBuffer nativeGetEndpoint(@NotNull ByteBuffer nativeDescriptor, int index);
}