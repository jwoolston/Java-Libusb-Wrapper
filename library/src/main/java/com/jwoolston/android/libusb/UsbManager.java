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

    private final Context context;
    private final android.hardware.usb.UsbManager androidUsbManager;
    private final HashMap<String, UsbDevice> localDeviceCache = new HashMap<>();
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

    @NonNull
    LibUsbContext getLibUsbContext() {
        return libUsbContext;
    }

    public void destroy() {
        if (libUsbContext != null) {
            nativeDestroy(libUsbContext.getNativeObject());
        }
    }

    @NonNull
    public UsbDeviceConnection registerDevice(@NonNull android.hardware.usb.UsbDevice device) throws IllegalAccessException {
        final String key = device.getDeviceName();
        if (localConnectionCache.containsKey(key)) {
            // We have already dealt with this device, do nothing
            Log.d(TAG, "returning cached device.");
            return localConnectionCache.get(key);
        } else {
            android.hardware.usb.UsbDeviceConnection connection = androidUsbManager.openDevice(device);
            if (connection == null) {
                // TODO: Replace with custom exception
                throw new IllegalAccessException("Failed to open device: " + device);
            }
            final UsbDevice usbDevice = UsbDevice.fromAndroidDevice(libUsbContext, device, connection);
            final UsbDeviceConnection usbConnection = UsbDeviceConnection.fromAndroidConnection(libUsbContext,
                                                                                                connection,
                                                                                                usbDevice);
            localDeviceCache.put(key, usbDevice);
            localConnectionCache.put(key, usbConnection);
            return usbConnection;
        }
    }

    /**
     * Returns a HashMap containing all USB devices currently attached.
     * USB device name is the key for the returned HashMap.
     * The result will be empty if no devices are attached, or if
     * USB host mode is inactive or unsupported.
     *
     * @return HashMap containing all connected USB devices.
     */
    /*public HashMap<String, UsbDevice> getDeviceList() {
        final HashMap<String, android.hardware.usb.UsbDevice> androidDevices = androidUsbManager.getDeviceList();
        // Clear any stale devices
        for (String key : localDeviceCache.keySet()) {
            if (!androidDevices.containsKey(key)) {
                localDeviceCache.remove(key);
            }
        }
        // Add any new devices
        for (String key : androidDevices.keySet()) {
            if (!localDeviceCache.contains(key)) {
                localDeviceCache.put(key, UsbDevice.fromAndroidDevice(androidDevices.get(key)));
            }
        }
    }

    /**
     * Opens the device so it can be used to send and receive
     * data using {@link android.hardware.usb.UsbRequest}.
     *
     * @param device the device to open
     *
     * @return a {@link UsbDeviceConnection}, or {@code null} if open failed
     */
    /*public UsbDeviceConnection openDevice(UsbDevice device) {
        try {
            String deviceName = device.getDeviceName();
            ParcelFileDescriptor pfd = mService.openDevice(deviceName);
            if (pfd != null) {
                UsbDeviceConnection connection = new UsbDeviceConnection(device);
                boolean result = connection.open(deviceName, pfd, context);
                pfd.close();
                if (result) {
                    return connection;
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "exception in UsbManager.openDevice", e);
        }
        return null;
    }

    /**
     * Returns a list of currently attached USB accessories.
     * (in the current implementation there can be at most one)
     *
     * @return list of USB accessories, or null if none are attached.
     */
    /*public UsbAccessory[] getAccessoryList() {
        if (mService == null) {
            return null;
        }
        try {
            UsbAccessory accessory = mService.getCurrentAccessory();
            if (accessory == null) {
                return null;
            } else {
                return new UsbAccessory[]{accessory};
            }
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    /**
     * Opens a file descriptor for reading and writing data to the USB accessory.
     *
     * @param accessory the USB accessory to open
     *
     * @return file descriptor, or null if the accessor could not be opened.
     */
    /*public ParcelFileDescriptor openAccessory(UsbAccessory accessory) {
        try {
            return mService.openAccessory(accessory);
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    /**
     * Returns true if the caller has permission to access the device.
     * Permission might have been granted temporarily via
     * {@link #requestPermission(UsbDevice, PendingIntent)} or
     * by the user choosing the caller as the default application for the device.
     *
     * @param device to check permissions for
     *
     * @return true if caller has permission
     */
    /*public boolean hasPermission(UsbDevice device) {
        if (mService == null) {
            return false;
        }
        try {
            return mService.hasDevicePermission(device);
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    /**
     * Returns true if the caller has permission to access the accessory.
     * Permission might have been granted temporarily via
     * {@link #requestPermission(UsbAccessory, PendingIntent)} or
     * by the user choosing the caller as the default application for the accessory.
     *
     * @param accessory to check permissions for
     *
     * @return true if caller has permission
     */
    /*public boolean hasPermission(UsbAccessory accessory) {
        if (mService == null) {
            return false;
        }
        try {
            return mService.hasAccessoryPermission(accessory);
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    /**
     * Requests temporary permission for the given package to access the device.
     * This may result in a system dialog being displayed to the user
     * if permission had not already been granted.
     * Success or failure is returned via the {@link android.app.PendingIntent} pi.
     * If successful, this grants the caller permission to access the device only
     * until the device is disconnected.
     * <p>
     * The following extras will be added to pi:
     * <ul>
     * <li> {@link #EXTRA_DEVICE} containing the device passed into this call
     * <li> {@link #EXTRA_PERMISSION_GRANTED} containing boolean indicating whether
     * permission was granted by the user
     * </ul>
     *
     * @param device to request permissions for
     * @param pi     PendingIntent for returning result
     */
    /*public void requestPermission(UsbDevice device, PendingIntent pi) {
        try {
            mService.requestDevicePermission(device, context.getPackageName(), pi);
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    /**
     * Requests temporary permission for the given package to access the accessory.
     * This may result in a system dialog being displayed to the user
     * if permission had not already been granted.
     * Success or failure is returned via the {@link android.app.PendingIntent} pi.
     * If successful, this grants the caller permission to access the accessory only
     * until the device is disconnected.
     * <p>
     * The following extras will be added to pi:
     * <ul>
     * <li> {@link #EXTRA_ACCESSORY} containing the accessory passed into this call
     * <li> {@link #EXTRA_PERMISSION_GRANTED} containing boolean indicating whether
     * permission was granted by the user
     * </ul>
     *
     * @param accessory to request permissions for
     * @param pi        PendingIntent for returning result
     */
    /*public void requestPermission(UsbAccessory accessory, PendingIntent pi) {
        try {
            mService.requestAccessoryPermission(accessory, context.getPackageName(), pi);
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    /**
     * Returns true if the specified USB function is currently enabled when in device mode.
     * <p>
     * USB functions represent interfaces which are published to the host to access
     * services offered by the device.
     * </p>
     *
     * @param function name of the USB function
     *
     * @return true if the USB function is enabled
     * <p>
     * {@hide}
     */
    /*public boolean isFunctionEnabled(String function) {
        if (mService == null) {
            return false;
        }
        try {
            return mService.isFunctionEnabled(function);
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    /**
     * Sets the current USB function when in device mode.
     * <p>
     * USB functions represent interfaces which are published to the host to access
     * services offered by the device.
     * </p><p>
     * This method is intended to select among primary USB functions.  The system may
     * automatically activate additional functions such as {@link #USB_FUNCTION_ADB}
     * or {@link #USB_FUNCTION_ACCESSORY} based on other settings and states.
     * </p><p>
     * The allowed values are: {@link #USB_FUNCTION_NONE}, {@link #USB_FUNCTION_AUDIO_SOURCE},
     * {@link #USB_FUNCTION_MIDI}, {@link #USB_FUNCTION_MTP}, {@link #USB_FUNCTION_PTP},
     * or {@link #USB_FUNCTION_RNDIS}.
     * </p><p>
     * Also sets whether USB data (for example, MTP exposed pictures) should be made available
     * on the USB connection when in device mode. Unlocking usb data should only be done with
     * user involvement, since exposing pictures or other data could leak sensitive
     * user information.
     * </p><p>
     * Note: This function is asynchronous and may fail silently without applying
     * the requested changes.
     * </p>
     *
     * @param function        name of the USB function, or null to restore the default function
     * @param usbDataUnlocked whether user data is accessible
     *                        <p>
     *                        {@hide}
     */
    /*public void setCurrentFunction(String function, boolean usbDataUnlocked) {
        try {
            mService.setCurrentFunction(function, usbDataUnlocked);
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    /**
     * Sets the screen unlocked functions, which are persisted and set as the current functions
     * whenever the screen is unlocked.
     * <p>
     * The allowed values are: {@link #USB_FUNCTION_NONE},
     * {@link #USB_FUNCTION_MIDI}, {@link #USB_FUNCTION_MTP}, {@link #USB_FUNCTION_PTP},
     * or {@link #USB_FUNCTION_RNDIS}.
     * {@link #USB_FUNCTION_NONE} has the effect of switching off this feature, so functions
     * no longer change on screen unlock.
     * </p><p>
     * Note: When the screen is on, this method will apply given functions as current functions,
     * which is asynchronous and may fail silently without applying the requested changes.
     * </p>
     *
     * @param function function to set as default
     *                 <p>
     *                 {@hide}
     */
    /*public void setScreenUnlockedFunctions(String function) {
        try {
            mService.setScreenUnlockedFunctions(function);
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    /**
     * Returns a list of physical USB ports on the device.
     * <p>
     * This list is guaranteed to contain all dual-role USB Type C ports but it might
     * be missing other ports depending on whether the kernel USB drivers have been
     * updated to publish all of the device's ports through the new "dual_role_usb"
     * device class (which supports all types of ports despite its name).
     * </p>
     *
     * @return The list of USB ports, or null if none.
     * @hide
     */
    /*public UsbPort[] getPorts() {
        if (mService == null) {
            return null;
        }
        try {
            return mService.getPorts();
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    /**
     * Gets the status of the specified USB port.
     *
     * @param port The port to query.
     *
     * @return The status of the specified USB port, or null if unknown.
     * @hide
     */
    /*public UsbPortStatus getPortStatus(UsbPort port) {
        Preconditions.checkNotNull(port, "port must not be null");
        try {
            return mService.getPortStatus(port.getId());
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    /**
     * Sets the desired role combination of the port.
     * <p>
     * The supported role combinations depend on what is connected to the port and may be
     * determined by consulting
     * {@link UsbPortStatus#isRoleCombinationSupported UsbPortStatus.isRoleCombinationSupported}.
     * </p><p>
     * Note: This function is asynchronous and may fail silently without applying
     * the requested changes.  If this function does cause a status change to occur then
     * a {@link #ACTION_USB_PORT_CHANGED} broadcast will be sent.
     * </p>
     *
     * @param powerRole The desired power role: {@link UsbPort#POWER_ROLE_SOURCE}
     *                  or {@link UsbPort#POWER_ROLE_SINK}, or 0 if no power role.
     * @param dataRole  The desired data role: {@link UsbPort#DATA_ROLE_HOST}
     *                  or {@link UsbPort#DATA_ROLE_DEVICE}, or 0 if no data role.
     *
     * @hide
     */
    /*public void setPortRoles(UsbPort port, int powerRole, int dataRole) {
        Preconditions.checkNotNull(port, "port must not be null");
        UsbPort.checkRoles(powerRole, dataRole);
        Log.d(TAG, "setPortRoles Package:" + context.getPackageName());
        try {
            mService.setPortRoles(port.getId(), powerRole, dataRole);
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    /**
     * Sets the component that will handle USB device connection.
     * <p>
     * Setting component allows to specify external USB host manager to handle use cases, where
     * selection dialog for an activity that will handle USB device is undesirable.
     * Only system components can call this function, as it requires the MANAGE_USB permission.
     *
     * @param usbDeviceConnectionHandler The component to handle usb connections,
     *                                   {@code null} to unset.
     *                                   <p>
     *                                   {@hide}
     */
    /*public void setUsbDeviceConnectionHandler(@Nullable ComponentName usbDeviceConnectionHandler) {
        try {
            mService.setUsbDeviceConnectionHandler(usbDeviceConnectionHandler);
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    /** @hide */
    /*public static String addFunction(String functions, String function) {
        if (USB_FUNCTION_NONE.equals(functions)) {
            return function;
        }
        if (!containsFunction(functions, function)) {
            if (functions.length() > 0) {
                functions += ",";
            }
            functions += function;
        }
        return functions;
    }

    /** @hide */
    /*public static String removeFunction(String functions, String function) {
        String[] split = functions.split(",");
        for (int i = 0; i < split.length; i++) {
            if (function.equals(split[i])) {
                split[i] = null;
            }
        }
        if (split.length == 1 && split[0] == null) {
            return USB_FUNCTION_NONE;
        }
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < split.length; i++) {
            String s = split[i];
            if (s != null) {
                if (builder.length() > 0) {
                    builder.append(",");
                }
                builder.append(s);
            }
        }
        return builder.toString();
    }

    /** @hide */
    /*public static boolean containsFunction(String functions, String function) {
        int index = functions.indexOf(function);
        if (index < 0) return false;
        if (index > 0 && functions.charAt(index - 1) != ',') return false;
        int charAfter = index + function.length();
        if (charAfter < functions.length() && functions.charAt(charAfter) != ',') return false;
        return true;
    }*/
}