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
package com.jwoolston.libusb;

import com.toxicbakery.logging.Arbor;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.Map.Entry;

/**
 * This class allows you to access the state of USB and communicate with USB devices.
 * Currently only host mode is supported in the public API.
 * <p>
 * This class API is based on the Android {@code android.hardware.usb.BaseUsbManager} class.
 *
 * @author Jared Woolston (Jared.Woolston@gmail.com)
 */
public class BaseUsbManager {

    static {
        System.loadLibrary("wrapper_libusb");
    }

    final Object cacheLock = new Object();

    final HashMap<String, BaseUsbDevice> localDeviceCache = new HashMap<>();
    final HashMap<String, BaseUsbDeviceConnection> localConnectionCache = new HashMap<>();
    final LibUsbContext libUsbContext;

    private volatile AsyncUSBThread asyncUsbThread;

    @Nullable
    private native ByteBuffer nativeInitialize();

    private native void nativeSetLoggingLevel(@NotNull ByteBuffer nativeObject, int level);

    private native void nativeDestroy(@NotNull ByteBuffer context);

    public BaseUsbManager() {
        libUsbContext = new LibUsbContext(nativeInitialize());
        BaseUsbDeviceConnection.initialize();
    }

    public void setNativeLogLevel(@NotNull LoggingLevel level) {
        nativeSetLoggingLevel(libUsbContext.getNativeObject(), level.ordinal());
    }

    public void destroy() {
        if (libUsbContext != null) {
            nativeDestroy(libUsbContext.getNativeObject());
        }
    }

    void unregisterDevice(@NotNull BaseUsbDevice device) {
        synchronized (cacheLock) {
            final String key = device.getDeviceName();
            localConnectionCache.remove(key);
            localDeviceCache.remove(key);
            onDeviceClosed();
        }
    }

    /**
     * Returns a {@link HashMap} containing all USB devices currently attached. USB device name is
     * the key for the returned {@link HashMap}. The result will be empty if no devices are
     * attached, or if USB host mode is inactive or unsupported.
     *
     * @return {@link HashMap} containing all connected USB devices.
     */
    public HashMap<String, BaseUsbDevice> getDeviceList() {
        synchronized (cacheLock) {
            final HashMap<String, BaseUsbDevice> map = new HashMap<>();
            for (Entry<String, BaseUsbDevice> entry : localDeviceCache.entrySet()) {
                map.put(entry.getKey(), entry.getValue());
            }
            return map;
        }
    }

    void onClosingDevice() {
        synchronized (cacheLock) {
            if (localConnectionCache.size() == 1) {
                // We need to shutdown the async communication thread if it is running
                if (asyncUsbThread != null) {
                    asyncUsbThread.shutdown();
                }
            }
        }
    }

    void onDeviceClosed() {
        synchronized (cacheLock) {
            if (localConnectionCache.size() == 0) {
                try {
                    asyncUsbThread.join();
                    asyncUsbThread = null;
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    void startAsyncIfNeeded() {
        if (asyncUsbThread == null) {
            Arbor.d("Starting async usb thread.");
            asyncUsbThread = new AsyncUSBThread(libUsbContext);
            asyncUsbThread.start();
        }
    }

    public static enum LoggingLevel {
        NONE,
        ERROR,
        WARNING,
        INFO,
        DEBUG
    }
}