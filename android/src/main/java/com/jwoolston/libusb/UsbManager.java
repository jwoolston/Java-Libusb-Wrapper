package com.jwoolston.libusb;

import android.content.Context;
import android.support.annotation.NonNull;

import com.toxicbakery.logging.Arbor;

import org.jetbrains.annotations.NotNull;

public class UsbManager extends BaseUsbManager {

    private final Context context;
    private final android.hardware.usb.UsbManager androidUsbManager;

    public UsbManager(@NotNull Context context) {
        super();
        this.context = context.getApplicationContext();
        androidUsbManager = (android.hardware.usb.UsbManager) context.getSystemService(Context.USB_SERVICE);
    }

    @NonNull
    public UsbDeviceConnection registerDevice(@NonNull android.hardware.usb.UsbDevice device) throws
            DevicePermissionDenied {
        synchronized (cacheLock) {
            final String key = device.getDeviceName();
            if (localConnectionCache.containsKey(key)) {
                // We have already dealt with this device, do nothing
                Arbor.d("returning cached device.");
                return (UsbDeviceConnection) localConnectionCache.get(key);
            } else {
                android.hardware.usb.UsbDeviceConnection connection = androidUsbManager.openDevice(device);
                if (connection == null) {
                    throw new DevicePermissionDenied(device);
                }
                final UsbDevice usbDevice = UsbDevice.fromAndroidDevice(libUsbContext, device, connection);
                final UsbDeviceConnection usbConnection = UsbDeviceConnection.fromAndroidConnection(context, this, usbDevice);
                localDeviceCache.put(key, usbDevice);
                localConnectionCache.put(key, usbConnection);

                usbDevice.populate();
                return usbConnection;
            }
        }
    }
}
