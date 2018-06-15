package com.jwoolston.android.libusb;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
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
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.MethodSorters;

/**
 * @author Jared Woolston (Jared.Woolston@gmail.com)
 */
@RequiresDevice
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
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
        manager.destroy();
    }

    @Test
    public void registerDevice() throws InterruptedException {
        // Due to the nature of usb on android these test need to be carefully controlled relative to each other to
        // be sure of the state of the device
        // We have to test permission denied first
        registerDevicePermissionDenied();
        Thread.sleep(200); // We need a small pause here to ensure the UI is right.
        registerDevicePermissionGranted();
        Thread.sleep(200);
        registerDeviceDouble();
    }

    private void registerDevicePermissionGranted() {
        Log.d(TAG, "Executing permission granted registration test.");
        final UsbManager _manager = new UsbManager(context);
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
                    UsbDevice usbDevice = _manager.registerDevice(device);
                    _manager.destroy();
                    assertNotNull("Failed to register USB device.", usbDevice);
                } catch (IllegalAccessException e) {
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

    private void registerDevicePermissionDenied() {
        Log.d(TAG, "Executing permission denied registration test.");
        final UsbManager _manager = new UsbManager(context);
        HashMap<String, android.hardware.usb.UsbDevice> devices = androidManager.getDeviceList();
        android.hardware.usb.UsbDevice device = null;
        for (String key : devices.keySet()) {
            device = devices.get(key);
            break;
        }
        assertNotNull("Failed to find a USB device.", device);
        requestPermissions(context, androidManager, device, new DeviceAvailable() {
            @Override public void onDeviceAvailable(@NonNull android.hardware.usb.UsbDevice device) {
                _manager.destroy();
                assertNull("onDeviceAvailable() should have never been called.", device);
            }

            @Override public void onDeviceDenied(@NonNull android.hardware.usb.UsbDevice device) {
                boolean thrown = false;
                try {
                    _manager.registerDevice(device);
                } catch (IllegalAccessException e) {
                    thrown = true;
                }
                _manager.destroy();
                assertTrue("Exception was not thrown as expected from attempting to register.", thrown);
            }
        });
        denyPermissions();
    }

    private void registerDeviceDouble() {
        Log.d(TAG, "Executing double registration test.");
        final UsbManager _manager = new UsbManager(context);
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
                    UsbDevice usbDevice = _manager.registerDevice(device);
                    assertNotNull("Failed to register USB device.", usbDevice);
                    UsbDevice usbDevice2 = _manager.registerDevice(device);
                    assertEquals("Registering the same device should have returned the same device.", usbDevice,
                                 usbDevice2);
                    assertSame("Registering the same device should have returned identical references.",
                               usbDevice, usbDevice2);
                } catch (IllegalAccessException e) {
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
        UiObject allowPermissions = uiDevice.findObject(new UiSelector().text("CANCEL"));
        if (allowPermissions.exists()) {
            try {
                allowPermissions.click();
            } catch (UiObjectNotFoundException e) {
                Log.e(TAG, "There is no permissions dialog to interact with ");
            }
        }
    }
}