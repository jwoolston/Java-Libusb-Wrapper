package com.jwoolston.libusb;

import com.toxicbakery.logging.Arbor;

import org.jetbrains.annotations.NotNull;

import java.nio.ByteBuffer;

/**
 * @author Jared Woolston (Jared.Woolston@gmail.com)
 */
public class AsyncUSBThread extends Thread {

    private static final String TAG = "AsyncThread";

    private static final String THREAD_NAME = "Async USB Handler";

    private volatile boolean keepRunning = true;

    @NotNull
    private final LibUsbContext context;

    AsyncUSBThread(@NotNull LibUsbContext context) {
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
                Arbor.e(e, "Async USB handling detected exception.");
            }
        }
    }

    @Override
    public void interrupt() {
        keepRunning = false;
        super.interrupt();
    }

    private static native int nativeHandleEvents(@NotNull ByteBuffer context);
}
