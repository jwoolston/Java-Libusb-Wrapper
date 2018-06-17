package com.jwoolston.android.libusb;

import android.support.annotation.NonNull;
import android.util.Log;

import java.nio.ByteBuffer;

/**
 * @author Jared Woolston (Jared.Woolston@gmail.com)
 */
public class AsyncUSBThread extends Thread {

    private static final String TAG = "AsyncThread";

    private volatile boolean keepRunning = true;

    @NonNull
    private final LibUsbContext context;

    AsyncUSBThread(@NonNull LibUsbContext context) {
        this.context = context;
    }

    void shutdown() {
        keepRunning = false;
    }

    @Override
    public void run() {
        while(keepRunning) {
            try {
                nativeHandleEvents(context.getNativeObject());
            } catch (Exception e) {
                Log.e(TAG, "Async USB handling detected exception.", e);
            }
        }
    }

    private static native void nativeHandleEvents(@NonNull ByteBuffer context);
}
