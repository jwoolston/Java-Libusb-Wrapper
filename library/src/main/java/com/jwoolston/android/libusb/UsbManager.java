/*
 * Copyright (C) 2010 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.jwoolston.android.libusb;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Log;
import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.Map.Entry;

/**
 * This class allows you to access the state of USB and communicate with USB devices.
 * Currently only host mode is supported in the public API.
 *
 * This class API is based on the Android {@link android.hardware.usb.UsbManager} class.
 *
 * @author Jared Woolston (Jared.Woolston@gmail.com)
 */
public class UsbManager {

    static {
        System.loadLibrary("wrapper_libusb");
    }

    private static final String TAG = "LibUSB UsbManager";

    private final Context                         context;
    private final android.hardware.usb.UsbManager androidUsbManager;
    private final HashMap<String, UsbDevice>           localDeviceCache     = new HashMap<>();
    private final HashMap<String, UsbDeviceConnection> localConnectionCache = new HashMap<>();
    private final LibUsbContext libUsbContext;

    @Nullable
    private native ByteBuffer nativeInitialize();

    private native void nativeDestroy(@NonNull ByteBuffer context);

    public UsbManager(@NonNull Context context) {
        this.context = context.getApplicationContext();
        androidUsbManager = (android.hardware.usb.UsbManager) context.getSystemService(Context.USB_SERVICE);
        libUsbContext = new LibUsbContext(nativeInitialize());
    }

    public void destroy() {
        if (libUsbContext != null) {
            nativeDestroy(libUsbContext.getNativeObject());
        }
    }

    @NonNull
    public UsbDeviceConnection registerDevice(@NonNull android.hardware.usb.UsbDevice device) throws
                                                                                              DevicePermissionDenied {
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
            final UsbDeviceConnection usbConnection = UsbDeviceConnection.fromAndroidConnection(context, this,
                                                                                                usbDevice);
            localDeviceCache.put(key, usbDevice);
            localConnectionCache.put(key, usbConnection);

            usbDevice.populate();
            return usbConnection;
        }
    }

    public void unregisterDevice(@NonNull UsbDevice device) {
        final String key = device.getDeviceName();
        localConnectionCache.remove(key);
        localDeviceCache.remove(key);
    }

    /**
     * Returns a {@link HashMap} containing all USB devices currently attached. USB device name is the key for the
     * returned {@link HashMap}. The result will be empty if no devices are attached, or if USB host mode is inactive
     * or unsupported.
     *
     * @return {@link HashMap} containing all connected USB devices.
     */
    public HashMap<String, UsbDevice> getDeviceList() {
        final HashMap<String, UsbDevice> map = new HashMap<>();
        for (Entry<String, UsbDevice> entry : localDeviceCache.entrySet()) {
            map.put(entry.getKey(), entry.getValue());
        }
        return map;
    }
}