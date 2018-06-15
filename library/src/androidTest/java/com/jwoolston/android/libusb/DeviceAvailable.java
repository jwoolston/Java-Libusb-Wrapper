package com.jwoolston.android.libusb;

import android.support.annotation.NonNull;

/**
 * @author Jared Woolston (Jared.Woolston@gmail.com)
 */
interface DeviceAvailable {

    void onDeviceAvailable(@NonNull android.hardware.usb.UsbDevice device);

    void onDeviceDenied(@NonNull android.hardware.usb.UsbDevice device);
}
