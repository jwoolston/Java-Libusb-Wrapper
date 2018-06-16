package com.jwoolston.android.libusb;

import static org.junit.Assert.assertNotNull;

import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbManager;
import android.support.annotation.NonNull;
import android.support.test.InstrumentationRegistry;
import android.support.test.rule.ActivityTestRule;
import android.support.test.runner.AndroidJUnit4;
import android.support.test.uiautomator.UiDevice;
import android.support.test.uiautomator.UiObject;
import android.support.test.uiautomator.UiObjectNotFoundException;
import android.support.test.uiautomator.UiSelector;
import android.util.Log;
import java.util.HashMap;
import org.junit.Before;
import org.junit.Rule;
import org.junit.runner.RunWith;

/**
 * @author Jared Woolston (Jared.Woolston@gmail.com)
 */
@RunWith(AndroidJUnit4.class)
public abstract class USBTestCase {

    private static final String ACTION_USB_PERMISSION = "com.android.example.USB_PERMISSION";

    private static final String TAG = "USBTestCase";

    @Rule
    public ActivityTestRule<USBTestActivity> mActivityTestRule = new ActivityTestRule<>(USBTestActivity.class);

    @Before
    public void setUp() {
    }

    @NonNull
    protected android.hardware.usb.UsbDevice findDevice(UsbManager androidManager) {
        HashMap<String, UsbDevice> devices = androidManager.getDeviceList();
        android.hardware.usb.UsbDevice device = null;
        for (String key : devices.keySet()) {
            device = devices.get(key);
            break;
        }
        assertNotNull("Failed to find a USB device.", device);
        return device;
    }

    protected void requestPermissions(@NonNull Context context, android.hardware.usb.UsbManager manager,
                                      @NonNull android.hardware.usb.UsbDevice device,
                                      @NonNull final DeviceAvailable callback) {
        PendingIntent permissionIntent = PendingIntent
                .getBroadcast(context, 0, new Intent(ACTION_USB_PERMISSION), 0);
        IntentFilter filter = new IntentFilter(ACTION_USB_PERMISSION);
        context.registerReceiver(new BroadcastReceiver() {
            @Override public void onReceive(Context context, Intent intent) {
                String action = intent.getAction();
                if (ACTION_USB_PERMISSION.equals(action)) {
                    synchronized (this) {
                        android.hardware.usb.UsbDevice _device = (android.hardware.usb.UsbDevice) intent
                                .getParcelableExtra(android.hardware.usb.UsbManager.EXTRA_DEVICE);
                        if (intent.getBooleanExtra(android.hardware.usb.UsbManager.EXTRA_PERMISSION_GRANTED, false)) {
                            if (_device != null) {
                                callback.onDeviceAvailable(_device);
                            }
                        } else {
                            Log.d(TAG, "permission denied for device " + _device);
                            callback.onDeviceDenied(_device);
                        }
                        context.unregisterReceiver(this);
                    }
                }
            }
        }, filter);
        manager.requestPermission(device, permissionIntent);
    }

    protected void allowPermissions() {
        UiDevice uiDevice = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation());
        UiObject allowPermissions = uiDevice.findObject(new UiSelector().text("OK"));
        if (allowPermissions.exists()) {
            try {
                allowPermissions.click();
            } catch (UiObjectNotFoundException e) {
                Log.e(TAG, "There is no permissions dialog to interact with ");
            }
        }
    }

    protected void denyPermissions() {
        UiDevice uiDevice = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation());
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
