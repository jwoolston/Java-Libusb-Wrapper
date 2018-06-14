package com.jwoolston.android.libusb;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.test.InstrumentationRegistry;
import android.support.test.filters.RequiresDevice;
import android.support.test.runner.AndroidJUnit4;
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
public class UsbManagerTest {

    @NonNull android.hardware.usb.UsbManager androidManager;
    @NonNull UsbManager manager;

    @Before
    public void setUp() throws Exception {
        // Context of the app under test.
        Context appContext = InstrumentationRegistry.getTargetContext();
        androidManager = (android.hardware.usb.UsbManager) appContext.getSystemService(Context.USB_SERVICE);
        manager = new UsbManager(appContext);
        assertNotNull("SetUp() failed to create UsbManager", manager);
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
        UsbDevice usbDevice = null;
        try {
            androidManager.requestPermission(device, null);
            usbDevice = manager.registerDevice(device);
        } catch (IllegalAccessException e) {
            assertNull("Registration threw exception.", e);
        }
        assertNotNull("Failed to register USB device.", usbDevice);
    }
}