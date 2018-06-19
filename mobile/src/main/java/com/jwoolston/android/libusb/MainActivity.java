package com.jwoolston.android.libusb;

import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;
import java.io.IOException;
import java.util.HashMap;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "MainActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG)
                        .setAction("Action", null).show();
            }
        });
    }

    @Override
    protected void onStart() {
        super.onStart();
        request();
    }

    private void request() {
        android.hardware.usb.UsbManager usbManager =
                (android.hardware.usb.UsbManager) getSystemService(Context.USB_SERVICE);
        PendingIntent mPermissionIntent = PendingIntent.getBroadcast(this, 0, new Intent(ACTION_USB_PERMISSION), 0);
        IntentFilter filter = new IntentFilter(ACTION_USB_PERMISSION);
        registerReceiver(usbReceiver, filter);

        HashMap<String, android.hardware.usb.UsbDevice> devices = usbManager.getDeviceList();
        for (String key : devices.keySet()) {
            android.hardware.usb.UsbDevice device = devices.get(key);
            usbManager.requestPermission(device, mPermissionIntent);
            break;
        }
    }

    private void communicateWithDevice(@NonNull android.hardware.usb.UsbDevice device) throws DevicePermissionDenied,
                                                                                              IOException {
        final UsbManager manager = new UsbManager(getApplicationContext());
        final UsbDeviceConnection connection = manager.registerDevice(device);
        Log.d(TAG, "Initiating transfer from device: " + connection.getDevice());
        UsbMassStorageDevice msc = UsbMassStorageDevice.getMassStorageDevice(this, manager, connection);
        if (msc == null) {
            throw new RuntimeException("Received a null MSC device.");
        }
        // before interacting with a device you need to call init()!
        msc.init();
    }

    private static final String ACTION_USB_PERMISSION = "com.android.example.USB_PERMISSION";

    private final BroadcastReceiver usbReceiver = new BroadcastReceiver() {

        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (ACTION_USB_PERMISSION.equals(action)) {
                synchronized (this) {
                    android.hardware.usb.UsbDevice device = (android.hardware.usb.UsbDevice)
                            intent.getParcelableExtra(android.hardware.usb.UsbManager.EXTRA_DEVICE);

                    if (intent.getBooleanExtra(android.hardware.usb.UsbManager.EXTRA_PERMISSION_GRANTED, false)) {
                        if (device != null) {
                            try {
                                communicateWithDevice(device);
                            } catch (DevicePermissionDenied devicePermissionDenied) {
                                devicePermissionDenied.printStackTrace();
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                        }
                    } else {
                        Log.d(TAG, "permission denied for device " + device);
                    }
                }
            }
        }
    };
}
