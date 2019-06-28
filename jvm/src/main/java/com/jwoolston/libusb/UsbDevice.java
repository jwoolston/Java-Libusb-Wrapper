package com.jwoolston.libusb;

import com.jwoolston.libusb.util.Preconditions;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.nio.ByteBuffer;

public class UsbDevice extends BaseUsbDevice {

    /*private UsbDevice(@NotNull android.hardware.usb.UsbDeviceConnection connection,
                          @NotNull android.hardware.usb.UsbDevice device, @Nullable ByteBuffer nativeObject) {


        serialNumber = connection.getSerial();


        fileDescriptor = connection.getFileDescriptor();
    }*/

    private UsbDevice(long nativePointer) {
        Preconditions.checkArgument(nativePointer != 0, "BaseUsbDevice initialization failed.");
        LibUsbDeviceDescriptor descriptor = LibUsbDeviceDescriptor.getDeviceDescriptor(nativePointer);
        initFromDescriptor(descriptor);
    }

    private UsbDevice(@Nullable ByteBuffer nativeObject) {
        LibUsbDeviceDescriptor descriptor = LibUsbDeviceDescriptor.getDeviceDescriptor(this);
        initFromDescriptor(descriptor);
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
