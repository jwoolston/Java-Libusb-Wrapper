package com.jwoolston.myapplication;

import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbManager;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import com.github.mjdev.libaums.UsbMassStorageDevice;
import com.github.mjdev.libaums.fs.FileSystem;
import com.github.mjdev.libaums.fs.UsbFile;
import java.io.IOException;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "MainActivity";

    private static final String ACTION_USB_PERMISSION = "com.android.example.USB_PERMISSION";

    UsbManager usbManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        usbManager = (UsbManager) getSystemService(USB_SERVICE);
    }

    @Override protected void onStart() {
        super.onStart();
        UsbMassStorageDevice[] devices = UsbMassStorageDevice.getMassStorageDevices(this /* Context or Activity */);

        for (UsbMassStorageDevice device : devices) {

            PendingIntent permissionIntent = PendingIntent.getBroadcast(this, 0, new Intent(ACTION_USB_PERMISSION),
                                                                        0);
            IntentFilter filter = new IntentFilter(ACTION_USB_PERMISSION);
            registerReceiver(mUsbReceiver, filter);
            usbManager.requestPermission(device.getUsbDevice(), permissionIntent);
        }
    }

    private final BroadcastReceiver mUsbReceiver = new BroadcastReceiver() {

        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (ACTION_USB_PERMISSION.equals(action)) {
                synchronized (this) {
                    UsbDevice device = (UsbDevice) intent.getParcelableExtra(UsbManager.EXTRA_DEVICE);

                    if (intent.getBooleanExtra(UsbManager.EXTRA_PERMISSION_GRANTED, false)) {
                        if (device != null) {
                            UsbMassStorageDevice msc = null;
                            UsbMassStorageDevice[] devices = UsbMassStorageDevice.getMassStorageDevices(context);
                            for (UsbMassStorageDevice device1 : devices) {
                                if (device1.getUsbDevice().equals(device)) {
                                    msc = device1;
                                    break;
                                }
                            }

                            // before interacting with a device you need to call init()!
                            try {
                                msc.init();

                                // Only uses the first partition on the device
                                FileSystem currentFs = msc.getPartitions().get(0).getFileSystem();
                                Log.d(TAG, "Capacity: " + currentFs.getCapacity());
                                Log.d(TAG, "Occupied Space: " + currentFs.getOccupiedSpace());
                                Log.d(TAG, "Free Space: " + currentFs.getFreeSpace());
                                Log.d(TAG, "Chunk size: " + currentFs.getChunkSize());

                                UsbFile root = currentFs.getRootDirectory();

                                UsbFile[] files = root.listFiles();
                                for (UsbFile file : files) {
                                    Log.d(TAG, file.getName());
                                    /*if (file.isDirectory()) {
                                        Log.d(TAG, "" + file.getLength());
                                    }*/
                                }
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
