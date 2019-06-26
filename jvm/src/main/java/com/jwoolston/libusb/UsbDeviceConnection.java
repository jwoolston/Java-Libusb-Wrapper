package com.jwoolston.libusb;

import org.jetbrains.annotations.NotNull;

/**
 * This class is used for sending and receiving data and control messages to a USB device.
 */
public class UsbDeviceConnection extends BaseUsbDeviceConnection {

    /**
     * BaseUsbDevice should only be instantiated by UsbService implementation
     */
    private UsbDeviceConnection(@NotNull UsbManager manager, @NotNull UsbDevice device) {
        super(manager, device);
    }

    @Override
    @NotNull
    public UsbDevice getDevice() {
        return (UsbDevice) super.getDevice();
    }
}
