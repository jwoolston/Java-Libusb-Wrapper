package com.jwoolston.libusb;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class UsbDevice extends BaseUsbDevice {

    /*private UsbDevice(@NotNull android.hardware.usb.UsbDeviceConnection connection,
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
    }*/

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
