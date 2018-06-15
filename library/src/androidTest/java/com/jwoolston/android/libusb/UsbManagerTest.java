package com.jwoolston.android.libusb;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.test.InstrumentationRegistry;
import android.support.test.filters.RequiresDevice;
import android.support.test.runner.AndroidJUnit4;
import android.support.test.uiautomator.UiDevice;
import android.support.test.uiautomator.UiObject;
import android.support.test.uiautomator.UiObjectNotFoundException;
import android.support.test.uiautomator.UiSelector;
import android.util.Log;
import java.util.HashMap;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * @author Jared Woolston (Jared.Woolston@gmail.com)
 */
@RequiresDevice
@RunWith(AndroidJUnit4.class)
public class UsbManagerTest extends USBTestCase {

    private static final String TAG = "UsbManagerTest";

    Context                         context;
    android.hardware.usb.UsbManager androidManager;
    UsbManager                      manager;
    UiDevice                        uiDevice;

    @Before
    public void setUp() throws Exception {
        // Context of the app under test.
        context = InstrumentationRegistry.getTargetContext();
        androidManager = (android.hardware.usb.UsbManager) context.getSystemService(Context.USB_SERVICE);
        manager = new UsbManager(context);
        assertNotNull("SetUp() failed to create UsbManager", manager);

        uiDevice = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation());
    }

    @After
    public void tearDown() throws Exception {
    }

    @Test
    public void registerDevice() {
        HashMap<String, android.hardware.usb.UsbDevice> devices = androidManager.getDeviceList();
        android.hardware.usb.UsbDevice device = null;
        for (String key : devices.keySet()) {
            device = devices.get(key);
            break;
        }
        assertNotNull("Failed to find a USB device.", device);
        requestPermissions(context, androidManager, device, new DeviceAvailable() {
            @Override public void onDeviceAvailable(@NonNull android.hardware.usb.UsbDevice device) {
                try {
                    UsbDevice usbDevice = manager.registerDevice(device);
                    assertNotNull("Failed to register USB device.", usbDevice);
                } catch (IllegalAccessException e) {
                    assertNull("Registration threw exception.", e);
                }
            }

            @Override public void onDeviceDenied(@NonNull android.hardware.usb.UsbDevice device) {
                assertNull("Permission for device was denied.", device);
            }
        });
        allowPermissions();
    }

    @Test
    public void registerDevicePermissionDenied() {
        HashMap<String, android.hardware.usb.UsbDevice> devices = androidManager.getDeviceList();
        android.hardware.usb.UsbDevice device = null;
        for (String key : devices.keySet()) {
            device = devices.get(key);
            break;
        }
        assertNotNull("Failed to find a USB device.", device);
        requestPermissions(context, androidManager, device, new DeviceAvailable() {
            @Override public void onDeviceAvailable(@NonNull android.hardware.usb.UsbDevice device) {
                assertNull("onDeviceAvailable() should have never been called.", device);
            }

            @Override public void onDeviceDenied(@NonNull android.hardware.usb.UsbDevice device) {
                boolean thrown = false;
                try {
                    manager.registerDevice(device);
                } catch (IllegalAccessException e) {
                    thrown = true;
                }
                assertTrue("Exception was not thrown as expected from attempting to register.", thrown);
            }
        });
        denyPermissions();
    }

    private void allowPermissions() {
        UiObject allowPermissions = uiDevice.findObject(new UiSelector().text("OK"));
        if (allowPermissions.exists()) {
            try {
                allowPermissions.click();
            } catch (UiObjectNotFoundException e) {
                Log.e(TAG, "There is no permissions dialog to interact with ");
            }
        }
    }

    private void denyPermissions() {
        UiObject allowPermissions = uiDevice.findObject(new UiSelector().text("OK"));
        if (allowPermissions.exists()) {
            try {
                allowPermissions.click();
            } catch (UiObjectNotFoundException e) {
                Log.e(TAG, "There is no permissions dialog to interact with ");
            }
        }
    }
}