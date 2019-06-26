package com.jwoolston.libusb;

import android.content.Context;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * This class is used for sending and receiving data and control messages to a USB device. Instances of this class are
 * created by {@link UsbManager#registerDevice(android.hardware.usb.UsbDevice)}.
 */
public class UsbDeviceConnection extends BaseUsbDeviceConnection {

    private Context context;

    @NotNull
    static UsbDeviceConnection fromAndroidConnection(@NotNull Context context, @NotNull UsbManager manager,
                                                         @NotNull UsbDevice device) {
        return new UsbDeviceConnection(context, manager, device);
    }

    /**
     * BaseUsbDevice should only be instantiated by UsbService implementation
     */
    private UsbDeviceConnection(@NotNull Context context, @NotNull UsbManager manager, @NotNull UsbDevice device) {
        super(manager, device);
        this.context = context;
    }

    /**
     * @return The application context the connection was created for.
     */
    @Nullable
    public Context getContext() {
        return context;
    }

    @Override
    @NotNull
    public UsbDevice getDevice() {
        return (UsbDevice) super.getDevice();
    }
}
