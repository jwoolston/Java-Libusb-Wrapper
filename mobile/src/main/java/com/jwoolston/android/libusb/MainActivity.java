package com.jwoolston.android.libusb;

import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.hardware.usb.UsbDevice;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.FloatingActionButton;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.jwoolston.android.libusb.msc_test_core.driver.scsi.ScsiBlockDevice;
import com.jwoolston.libusb.BaseUsbManager;
import com.jwoolston.libusb.DevicePermissionDenied;
import com.jwoolston.libusb.UsbDeviceConnection;
import com.jwoolston.libusb.UsbManager;
import com.toxicbakery.logging.Arbor;
import com.toxicbakery.logging.LogCatSeedling;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.Locale;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "MainActivity";

    private static final int BLOCKS_PER_TRANSFER = 2048;

    private volatile long syncStartTime;
    private volatile long asyncStartTime;
    private volatile int blockSize;

    private SynchronousTask syncTask;
    private AsynchronousTask asyncTask;

    private TextView syncEllapsedTime;
    private ProgressBar syncProgress;
    private TextView syncSpeed;

    private TextView asyncEllapsedTime;
    private ProgressBar asyncProgress;
    private TextView asyncSpeed;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Arbor.sow(new LogCatSeedling());
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        syncEllapsedTime = (TextView) findViewById(R.id.sync_ellapsed_time);
        syncProgress = (ProgressBar) findViewById(R.id.sync_progress);
        syncSpeed = (TextView) findViewById(R.id.sync_speed);

        asyncEllapsedTime = (TextView) findViewById(R.id.async_ellapsed_time);
        asyncProgress = (ProgressBar) findViewById(R.id.async_progress);
        asyncSpeed = (TextView) findViewById(R.id.async_speed);

        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                request();
            }
        });
    }

    private void request() {
        android.hardware.usb.UsbManager usbManager =
            (android.hardware.usb.UsbManager) getSystemService(Context.USB_SERVICE);
        PendingIntent mPermissionIntent = PendingIntent.getBroadcast(this, 0, new Intent(ACTION_USB_PERMISSION), 0);
        IntentFilter filter = new IntentFilter(ACTION_USB_PERMISSION);
        registerReceiver(usbReceiver, filter);

        HashMap<String, android.hardware.usb.UsbDevice> devices = usbManager.getDeviceList();
        for (String key : devices.keySet()) {
            UsbDevice device = devices.get(key);
            usbManager.requestPermission(device, mPermissionIntent);
            break;
        }
    }

    private final class SynchronousTask extends AsyncTask<UsbDevice, Long, UsbDeviceConnection> {

        @Override
        protected UsbDeviceConnection doInBackground(UsbDevice... usbDevices) {
            try {
                return communicateWithDevice(usbDevices[0]);
            } catch (DevicePermissionDenied | IOException e) {
                e.printStackTrace();
            }
            return null;
        }

        @Override
        protected void onProgressUpdate(Long... values) {
            long duration = values[1] - syncStartTime;
            final String ellapsed = getEllapsedTimeString(duration);
            syncEllapsedTime.setText(ellapsed);
            int count = Math.toIntExact(values[0]);
            syncProgress.setProgress(count);
            double speed = (1000.0 * count * blockSize) / (1024 * 1024 * duration); // bytes / s
            syncSpeed.setText(String.format(Locale.US, "%.2fMB/s", speed));
        }

        @Override
        protected void onPostExecute(UsbDeviceConnection usbDeviceConnection) {
            super.onPostExecute(usbDeviceConnection);
        }

        private UsbDeviceConnection communicateWithDevice(@NonNull android.hardware.usb.UsbDevice device) throws DevicePermissionDenied,
            IOException {
            final UsbManager manager = new UsbManager(getApplicationContext());
            manager.setNativeLogLevel(BaseUsbManager.LoggingLevel.DEBUG);
            final UsbDeviceConnection connection = manager.registerDevice(device);
            connection.resetDevice();
            Arbor.d("Initiating transfer from device: %s", connection.getDevice());
            Arbor.d("Device speed: %s", connection.getDevice().getDeviceSpeed());
            UsbMassStorageDevice msc = UsbMassStorageDevice.getMassStorageDevice(MainActivity.this, manager, connection);
            if (msc == null) {
                throw new RuntimeException("Received a null MSC device.");
            }
            // before interacting with a device you need to call init()!
            msc.init(false);
            final ScsiBlockDevice block = (ScsiBlockDevice) msc.getBlockDevice();
            int blockSize = block.getBlockSize();
            int lastBlockAddress = block.getLastBlockAddress();
            MainActivity.this.blockSize = blockSize;
            final byte[] data = new byte[BLOCKS_PER_TRANSFER * blockSize];
            final ByteBuffer buffer = ByteBuffer.wrap(data);
            syncProgress.setMax(lastBlockAddress);
            syncStartTime = System.currentTimeMillis();
            for (int i = 0; i < lastBlockAddress; i += BLOCKS_PER_TRANSFER) {
                buffer.rewind();
                block.read(i, buffer);
                //Log.i(TAG, "\n" + Hexdump.dumpHexString(data, 0, 256));
                //Log.i(TAG, "\n" + Hexdump.dumpHexString(data, 256, 256));
                publishProgress((long) (i + BLOCKS_PER_TRANSFER), System.currentTimeMillis());
            }
            return connection;
        }
    }

    private final class AsynchronousTask extends AsyncTask<UsbDeviceConnection, Long, Void> {

        @Override
        protected Void doInBackground(UsbDeviceConnection... usbDevices) {
            try {
                communicateWithDevice(usbDevices[0]);
            } catch (DevicePermissionDenied | IOException e) {
                e.printStackTrace();
            }
            return null;
        }

        @Override
        protected void onProgressUpdate(Long... values) {
            long duration = values[1] - asyncStartTime;
            final String ellapsed = getEllapsedTimeString(duration);
            asyncEllapsedTime.setText(ellapsed);
            int count = Math.toIntExact(values[0]);
            asyncProgress.setProgress(count);
        }

        private void communicateWithDevice(@NonNull UsbDeviceConnection connection) throws DevicePermissionDenied,
            IOException {
            final UsbManager manager = new UsbManager(getApplicationContext());
            connection.resetDevice();
            Arbor.d("Initiating transfer from device: %s", connection.getDevice());
            UsbMassStorageDevice msc = UsbMassStorageDevice.getMassStorageDevice(MainActivity.this, manager, connection);
            if (msc == null) {
                throw new RuntimeException("Received a null MSC device.");
            }
            // before interacting with a device you need to call init()!
            msc.init(true);
            final ScsiBlockDevice block = (ScsiBlockDevice) msc.getBlockDevice();
            int blockSize = block.getBlockSize();
            int lastBlockAddress = block.getLastBlockAddress();
            final byte[] data = new byte[BLOCKS_PER_TRANSFER * blockSize];
            final ByteBuffer buffer = ByteBuffer.wrap(data);
            asyncProgress.setMax(lastBlockAddress);
            asyncStartTime = System.currentTimeMillis();
            for (int i = 0; i < lastBlockAddress; i += BLOCKS_PER_TRANSFER) {
                buffer.rewind();
                block.read(i, buffer);
                publishProgress((long) (i + BLOCKS_PER_TRANSFER), System.currentTimeMillis());
            }
        }
    }

    public static String getEllapsedTimeString(long ellapsed_ms) {
        long second = (ellapsed_ms / 1000) % 60;
        long minute = (ellapsed_ms / (1000 * 60)) % 60;
        long hour = (ellapsed_ms / (1000 * 60 * 60)) % 24;

        return String.format(Locale.US, "%02d:%02d:%02d", hour, minute, second);
    }

    private static final String ACTION_USB_PERMISSION = "com.android.example.USB_PERMISSION";

    private final BroadcastReceiver usbReceiver = new BroadcastReceiver() {

        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (ACTION_USB_PERMISSION.equals(action)) {
                synchronized (this) {
                    UsbDevice device = (UsbDevice) intent.getParcelableExtra(android.hardware.usb.UsbManager.EXTRA_DEVICE);

                    if (intent.getBooleanExtra(android.hardware.usb.UsbManager.EXTRA_PERMISSION_GRANTED, false)) {
                        if (device != null) {
                            syncTask = new SynchronousTask();
                            syncTask.execute(device);
                        }
                    } else {
                        Arbor.d("Permission denied for device %s", device);
                    }
                }
            }
        }
    };
}
