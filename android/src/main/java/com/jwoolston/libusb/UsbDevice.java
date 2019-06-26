package com.jwoolston.libusb;

import android.os.Parcel;
import android.os.Parcelable;
import android.support.annotation.NonNull;

import com.jwoolston.libusb.util.Preconditions;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.nio.ByteBuffer;

public class UsbDevice extends BaseUsbDevice implements Parcelable {

    @NotNull
    private final android.hardware.usb.UsbDevice device;

    @NotNull
    public android.hardware.usb.UsbDevice getAndroidDevice() {
        return device;
    }

    /**
     * Returns a unique integer ID for the device. This is a convenience for clients that want to
     * use an integer to represent the device, rather than the device name. IDs are not persistent
     * across USB disconnects.
     *
     * @return the device ID
     */
    public int getDeviceId() {
        return device.getDeviceId();
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeLong(nativeGetPointerFromNativeObject(getNativeObject()));
        dest.writeParcelable(device, flags);
        dest.writeString(name);
        dest.writeString(manufacturerName);
        dest.writeString(productName);
        dest.writeString(version);
        dest.writeString(serialNumber);
        dest.writeInt(speed.code);
        dest.writeInt(vendorId);
        dest.writeInt(productId);
        dest.writeInt(deviceClass);
        dest.writeInt(subclass);
        dest.writeInt(protocol);
        dest.writeInt(fileDescriptor);
        dest.writeTypedArray((UsbConfiguration[]) configurations, flags);
        dest.writeTypedArray((UsbInterface[]) interfaces, flags);
    }

    public static final Creator<UsbDevice> CREATOR = new Creator<UsbDevice>() {
        @Override
        public UsbDevice createFromParcel(Parcel in) {
            return new UsbDevice(in);
        }

        @Override
        public UsbDevice[] newArray(int size) {
            return new UsbDevice[size];
        }
    };

    @NonNull
    static UsbDevice fromAndroidDevice(@NotNull LibUsbContext context, @NotNull android.hardware.usb.UsbDevice device,
                                           @NotNull android.hardware.usb.UsbDeviceConnection connection) {
        return new UsbDevice(connection, device, wrapDevice(context.getNativeObject(), connection.getFileDescriptor()));
    }

    private UsbDevice(@NotNull android.hardware.usb.UsbDeviceConnection connection,
                          @NotNull android.hardware.usb.UsbDevice device, @Nullable ByteBuffer nativeObject) {
        Preconditions.checkNotNull(nativeObject, "BaseUsbDevice initialization failed.");
        this.nativeObject = nativeObject;
        this.device = device;
        name = device.getDeviceName();
        vendorId = device.getVendorId();
        productId = device.getProductId();
        deviceClass = device.getDeviceClass();
        subclass = device.getDeviceSubclass();
        protocol = device.getDeviceProtocol();

        LibUsbDeviceDescriptor descriptor = LibUsbDeviceDescriptor.getDeviceDescriptor(this);
        manufacturerName = nativeGetManufacturerString(nativeObject, descriptor.getNativeObject());
        productName = nativeGetProductNameString(nativeObject, descriptor.getNativeObject());
        version = nativeGetDeviceVersion(descriptor.getNativeObject());
        serialNumber = connection.getSerial();
        speed = LibusbSpeed.fromNative(nativeGetDeviceSpeed(nativeObject, descriptor.getNativeObject()));

        fileDescriptor = connection.getFileDescriptor();
    }

    protected UsbDevice(Parcel in) {
        final ByteBuffer buffer = nativeGetNativeObjectFromPointer(in.readLong());
        if (buffer == null) {
            throw new IllegalStateException("Received a null reference for the native object. Creation from "
                    + "parcel failed.");
        }
        nativeObject = buffer;
        device = in.readParcelable(android.hardware.usb.UsbDevice.class.getClassLoader());
        name = in.readString();
        manufacturerName = in.readString();
        productName = in.readString();
        version = in.readString();
        serialNumber = in.readString();
        speed = LibusbSpeed.fromNative(in.readInt());
        vendorId = in.readInt();
        productId = in.readInt();
        deviceClass = in.readInt();
        subclass = in.readInt();
        protocol = in.readInt();
        fileDescriptor = in.readInt();
        configurations = in.createTypedArray(UsbConfiguration.CREATOR);
        interfaces = in.createTypedArray(UsbInterface.CREATOR);
    }

    @Override
    UsbConfiguration createConfiguration(int id, @Nullable String name, int attributes, int maxPower) {
        return new UsbConfiguration(id, name, attributes, maxPower);
    }

    @Override
    UsbInterface createInterface(int id, int alternateSetting, @Nullable String name,
                                     int interfaceClass, int subClass, int protocol) {
        return new UsbInterface(id, alternateSetting, name, interfaceClass, subClass, protocol);
    }

    @Override
    UsbEndpoint createEndpoint(int address, int attributes, int maxPacketSize, int interval) {
        return new UsbEndpoint(address, attributes, maxPacketSize, interval);
    }

    @Override
    @NotNull
    public UsbInterface getInterface(int index) {
        return (UsbInterface) super.getInterface(index);
    }
}
