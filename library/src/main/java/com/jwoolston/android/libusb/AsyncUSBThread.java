package com.jwoolston.android.libusb;

import android.support.annotation.NonNull;
import android.util.Log;

import java.nio.ByteBuffer;

/**
 * @author Jared Woolston (Jared.Woolston@gmail.com)
 */
public class AsyncUSBThread extends Thread {

    private static final String TAG = "AsyncThread";

    private static final String THREAD_NAME = "Async USB Handler";

    private volatile boolean keepRunning = true;

    @NonNull
    private final LibUsbContext context;

    AsyncUSBThread(@NonNull LibUsbContext context) {
        super(THREAD_NAME);
        this.context = context;
    }

    void shutdown() {
        keepRunning = false;
    }

    @Override
    public void run() {
        while(keepRunning) {
            try {
                LibusbError result = LibusbError.fromNative(nativeHandleEvents(context.getNativeObject()));
            } catch (Exception e) {
                Log.e(TAG, "Async USB handling detected exception.", e);
            }
        }
    }

    @Override
    public void interrupt() {
        keepRunning = false;
        super.interrupt();
    }

    private static native int nativeHandleEvents(@NonNull ByteBuffer context);
}
