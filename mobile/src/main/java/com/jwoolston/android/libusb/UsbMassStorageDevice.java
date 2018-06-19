/*
 * (C) Copyright 2014 mjahnen <jahnen@in.tum.de>
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

package com.jwoolston.android.libusb;

import android.content.Context;
import android.util.Log;
import com.jwoolston.android.libusb.msc_test_core.driver.BlockDeviceDriver;
import com.jwoolston.android.libusb.msc_test_core.driver.BlockDeviceDriverFactory;
import com.jwoolston.android.libusb.msc_test_core.usb.UsbCommunication;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Class representing a connected USB mass storage device. You can enumerate
 * through all connected mass storage devices via
 * {@link #getMassStorageDevices(Context, UsbManager)}. This method only returns supported
 * devices or if no device is connected an empty array.
 * <p>
 * After choosing a device you have to get the permission for the underlying
 * {@link android.hardware.usb.UsbDevice}. The underlying
 * {@link android.hardware.usb.UsbDevice} can be accessed via
 * {@link #getUsbDevice()}.
 * <p>
 * After that you need to call {@link #setupDevice()}. This will initialize the
 * mass storage device
 *
 * @author mjahnen
 */
public class UsbMassStorageDevice {

    private static final String TAG = UsbMassStorageDevice.class.getSimpleName();

    /**
     * subclass 6 means that the usb mass storage device implements the SCSI
     * transparent command set
     */
    private static final int INTERFACE_SUBCLASS = 6;

    /**
     * protocol 80 means the communication happens only via bulk transfers
     */
    private static final int INTERFACE_PROTOCOL = 80;

    private UsbManager          usbManager;
    private UsbDeviceConnection deviceConnection;
    private UsbInterface        usbInterface;
    private UsbEndpoint         inEndpoint;
    private UsbEndpoint         outEndpoint;

    private BlockDeviceDriver blockDevice;

    /**
     * Construct a new {@link UsbMassStorageDevice}.
     * The given parameters have to actually be a mass storage device, this is
     * not checked in the constructor!
     *
     * @param usbManager
     * @param usbDeviceConnection
     * @param usbInterface
     * @param inEndpoint
     * @param outEndpoint
     */
    private UsbMassStorageDevice(UsbManager usbManager, UsbDeviceConnection usbDeviceConnection,
                                 UsbInterface usbInterface, UsbEndpoint inEndpoint, UsbEndpoint outEndpoint) {
        this.usbManager = usbManager;
        this.deviceConnection = usbDeviceConnection;
        this.usbInterface = usbInterface;
        this.inEndpoint = inEndpoint;
        this.outEndpoint = outEndpoint;
    }

    /**
     * This method iterates through all connected USB devices and searches for
     * mass storage devices.
     *
     * @param context Context to get the {@link UsbManager}
     *
     * @return An array of suitable mass storage devices or an empty array if
     * none could be found.
     */
    public static UsbMassStorageDevice getMassStorageDevice(Context context, UsbManager usbManager,
                                                            UsbDeviceConnection connection) {
        UsbDevice device = connection.getDevice();
        Log.i(TAG, "found usb device: " + device);

        int interfaceCount = device.getInterfaceCount();
        for (int i = 0; i < interfaceCount; i++) {
            UsbInterface usbInterface = device.getInterface(i);
            Log.i(TAG, "found usb interface: " + usbInterface);

            // we currently only support SCSI transparent command set with
            // bulk transfers only!
            if (usbInterface.getInterfaceClass() != UsbConstants.USB_CLASS_MASS_STORAGE
                || usbInterface.getInterfaceSubclass() != INTERFACE_SUBCLASS
                || usbInterface.getInterfaceProtocol() != INTERFACE_PROTOCOL) {
                Log.i(TAG, "device interface not suitable!");
                continue;
            }

            // Every mass storage device has exactly two endpoints
            // One IN and one OUT endpoint
            int endpointCount = usbInterface.getEndpointCount();
            if (endpointCount != 2) {
                Log.w(TAG, "inteface endpoint count != 2");
            }

            UsbEndpoint outEndpoint = null;
            UsbEndpoint inEndpoint = null;
            for (int j = 0; j < endpointCount; j++) {
                UsbEndpoint endpoint = usbInterface.getEndpoint(j);
                Log.i(TAG, "found usb endpoint: " + endpoint);
                if (endpoint.getType() == UsbConstants.USB_ENDPOINT_XFER_BULK) {
                    if (endpoint.getDirection() == UsbConstants.USB_DIR_OUT) {
                        outEndpoint = endpoint;
                    } else {
                        inEndpoint = endpoint;
                    }
                }
            }

            if (outEndpoint == null || inEndpoint == null) {
                Log.e(TAG, "Not all needed endpoints found!");
                continue;
            }
            return new UsbMassStorageDevice(usbManager, connection, usbInterface, inEndpoint, outEndpoint);
        }
        return null;
    }

    /**
     * Initializes the mass storage device and determines different things like
     * for example the MBR or the file systems for the different partitions.
     *
     * @throws IOException           If reading from the physical device fails.
     * @throws IllegalStateException If permission to communicate with the underlying
     *                               {@link UsbDevice} is missing.
     * @see #getUsbDevice()
     */
    public void init() throws IOException {
        setupDevice();
    }

    public BlockDeviceDriver getBlockDevice() {
        return blockDevice;
    }

    /**
     * Sets the device up. Claims interface and initiates the device connection.
     * Initializes the {@link #blockDevice}
     *
     * @throws IOException If reading from the physical device fails.
     * @see #init()
     */
    private void setupDevice() throws IOException {
        Log.d(TAG, "setup device");
        if (deviceConnection == null) {
            throw new IOException("deviceConnection is null!");
        }

        LibusbError claim = deviceConnection.claimInterface(usbInterface, true);
        if (claim != LibusbError.LIBUSB_SUCCESS) {
            throw new IOException("could not claim interface! " + claim);
        }

        UsbCommunication communication = new SynchronousMSC(deviceConnection, outEndpoint, inEndpoint);
        byte[] b = new byte[1];
        deviceConnection.controlTransfer(0b10100001, 0b11111110, 0, usbInterface.getId(), b, 1, 5000);
        Log.i(TAG, "MAX LUN " + (int) b[0]);
        blockDevice = BlockDeviceDriverFactory.createBlockDevice(communication);
        blockDevice.init();
    }

    /**
     * Releases the {@link android.hardware.usb.UsbInterface} and closes the
     * {@link android.hardware.usb.UsbDeviceConnection}. After calling this
     * method no further communication is possible.
     */
    public void close() {
        Log.d(TAG, "close device");
        if (deviceConnection == null) {
            return;
        }

        LibusbError release = deviceConnection.releaseInterface(usbInterface);
        if (release != LibusbError.LIBUSB_SUCCESS) {
            Log.e(TAG, "could not release interface! " + release);
        }
        deviceConnection.close();
    }

    /**
     * This returns the {@link android.hardware.usb.UsbDevice} which can be used
     * to request permission for communication.
     *
     * @return Underlying {@link android.hardware.usb.UsbDevice} used for
     * communication.
     */
    public UsbDevice getUsbDevice() {
        return deviceConnection.getDevice();
    }
}