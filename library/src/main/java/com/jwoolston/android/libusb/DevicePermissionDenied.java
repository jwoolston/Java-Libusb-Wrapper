package com.jwoolston.android.libusb;

import android.hardware.usb.UsbDevice;
import android.support.annotation.NonNull;

/**
 * @author Jared Woolston (Jared.Woolston@gmail.com)
 */
public class DevicePermissionDenied extends Exception {

    public DevicePermissionDenied(@NonNull UsbDevice device) {
        super("Permission was denied for device: " + device.getDeviceName());
    }
}
