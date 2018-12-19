package com.jwoolston.libusb.android;

import android.content.Context;
import android.util.Log;
import com.jwoolston.libusb.UsbDeviceConnection;
import org.jetbrains.annotations.NotNull;

/**
 * @author Jared Woolston (jwoolston@keywcorp.com)
 */
public class UsbManager extends com.jwoolston.libusb.UsbManager {

    private static final String TAG = "Android LibUSB Manager";

    private final Context context;
    private final android.hardware.usb.UsbManager androidUsbManager;

    public UsbManager(@NotNull Context context) {
        super();
        this.context = context.getApplicationContext();
        androidUsbManager = (android.hardware.usb.UsbManager) context.getSystemService(Context.USB_SERVICE);
    }

    @NotNull
    public UsbDeviceConnection registerDevice(@NotNull android.hardware.usb.UsbDevice device) throws
                                                                                              DevicePermissionDenied {
        synchronized (cacheLock) {
            final String key = device.getDeviceName();
            if (localConnectionCache.containsKey(key)) {
                // We have already dealt with this device, do nothing
                Log.d(TAG, "returning cached device.");
                return localConnectionCache.get(key);
            } else {
                android.hardware.usb.UsbDeviceConnection connection = androidUsbManager.openDevice(device);
                if (connection == null) {
                    throw new DevicePermissionDenied(device);
                }
                final UsbDevice usbDevice = UsbDevice.fromAndroidDevice(libUsbContext, device, connection);
                final UsbDeviceConnection usbConnection = new UsbDeviceConnection(this, usbDevice);
                localDeviceCache.put(key, usbDevice);
                localConnectionCache.put(key, usbConnection);

                usbDevice.populate();
                return usbConnection;
            }
        }
    }
}
