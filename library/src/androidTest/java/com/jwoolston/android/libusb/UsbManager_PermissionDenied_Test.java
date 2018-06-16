package com.jwoolston.android.libusb;

import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.test.InstrumentationRegistry;
import android.support.test.filters.MediumTest;
import android.support.test.filters.RequiresDevice;
import android.support.test.runner.AndroidJUnit4;
import android.util.Log;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * @author Jared Woolston (Jared.Woolston@gmail.com)
 */
@RequiresDevice
@MediumTest
@RunWith(AndroidJUnit4.class)
public class UsbManager_PermissionDenied_Test extends USBTestCase {

    private static final String TAG = "UsbManagerTest";

    @Ignore("For now, there is no way to ensure the permission will be properly denied.")
    @Test
    public void registerDevicePermissionDenied() {
        Log.d(TAG, "Executing permission denied registration test.");
        Context context = InstrumentationRegistry.getTargetContext();
        final android.hardware.usb.UsbManager androidManager = (android.hardware.usb.UsbManager) context.getSystemService
                (Context.USB_SERVICE);
        final UsbManager _manager = new UsbManager(context);
        android.hardware.usb.UsbDevice device = findDevice(androidManager);
        requestPermissions(context, androidManager, device, new DeviceAvailable() {
            @Override public void onDeviceAvailable(@NonNull android.hardware.usb.UsbDevice device) {
                _manager.destroy();
                assertNull("onDeviceAvailable() should have never been called.", device);
            }

            @Override public void onDeviceDenied(@NonNull android.hardware.usb.UsbDevice device) {
                boolean thrown = false;
                try {
                    _manager.registerDevice(device);
                } catch (DevicePermissionDenied e) {
                    thrown = true;
                }
                _manager.destroy();
                assertTrue("Exception was not thrown as expected from attempting to register.", thrown);
            }
        });
        denyPermissions();
    }
}