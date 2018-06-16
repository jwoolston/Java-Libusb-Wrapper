package com.jwoolston.android.libusb;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.test.InstrumentationRegistry;
import android.support.test.filters.MediumTest;
import android.support.test.filters.RequiresDevice;
import android.support.test.runner.AndroidJUnit4;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * @author Jared Woolston (Jared.Woolston@gmail.com)
 */
@RequiresDevice
@MediumTest
@RunWith(AndroidJUnit4.class)
public class UsbDeviceConnectionTest extends USBTestCase {

    private static final String TAG = "UsbDeviceConnectionTest";

    @Test
    public void getFileDescriptor() {
        Context context = InstrumentationRegistry.getTargetContext();
        android.hardware.usb.UsbManager androidManager = (android.hardware.usb.UsbManager) context.getSystemService
                (Context.USB_SERVICE);
        final UsbManager _manager = new UsbManager(context);
        android.hardware.usb.UsbDevice device = findDevice(androidManager);
        requestPermissions(context, androidManager, device, new DeviceAvailable() {
            @Override public void onDeviceAvailable(@NonNull android.hardware.usb.UsbDevice device) {
                try {
                    UsbDeviceConnection deviceConnection = _manager.registerDevice(device);
                    final int fd = deviceConnection.getFileDescriptor();
                    _manager.destroy();
                    assertTrue("File descriptor was less than 0.", 0 <= fd);
                } catch (DevicePermissionDenied e) {
                    _manager.destroy();
                    assertNull("Registration threw exception.", e);
                }
            }

            @Override public void onDeviceDenied(@NonNull android.hardware.usb.UsbDevice device) {
                _manager.destroy();
                assertNull("Permission for device was denied.", device);
            }
        });
        allowPermissions();
    }

    @Test
    public void getRawDescriptors() {
        Context context = InstrumentationRegistry.getTargetContext();
        android.hardware.usb.UsbManager androidManager = (android.hardware.usb.UsbManager) context.getSystemService
                (Context.USB_SERVICE);
        final UsbManager _manager = new UsbManager(context);
        android.hardware.usb.UsbDevice device = findDevice(androidManager);
        requestPermissions(context, androidManager, device, new DeviceAvailable() {
            @Override public void onDeviceAvailable(@NonNull android.hardware.usb.UsbDevice device) {
                try {
                    UsbDeviceConnection deviceConnection = _manager.registerDevice(device);
                    final byte[] descriptor = deviceConnection.getRawDescriptors();
                    _manager.destroy();
                    assertNotNull("Raw descriptor was null.", descriptor);
                } catch (DevicePermissionDenied e) {
                    _manager.destroy();
                    assertNull("Registration threw exception.", e);
                }
            }

            @Override public void onDeviceDenied(@NonNull android.hardware.usb.UsbDevice device) {
                _manager.destroy();
                assertNull("Permission for device was denied.", device);
            }
        });
        allowPermissions();
    }
}