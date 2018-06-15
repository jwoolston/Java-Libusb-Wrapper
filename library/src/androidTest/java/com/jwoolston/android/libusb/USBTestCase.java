package com.jwoolston.android.libusb;

import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.support.annotation.NonNull;
import android.support.test.rule.ActivityTestRule;
import android.support.test.runner.AndroidJUnit4;
import android.util.Log;
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
}
