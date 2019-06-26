package com.jwoolston.libusb;

import org.jetbrains.annotations.NotNull;

/**
 * @author Jared Woolston (Jared.Woolston@gmail.com)
 */
public class DevicePermissionDenied extends Exception {

    public DevicePermissionDenied(@NotNull android.hardware.usb.UsbDevice device) {
        super("Permission was denied for device: " + device.getDeviceName());
    }
}
