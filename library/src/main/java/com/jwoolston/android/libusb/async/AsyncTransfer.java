package com.jwoolston.android.libusb.async;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.jwoolston.android.libusb.UsbEndpoint;

import java.nio.ByteBuffer;

/**
 * @author Jared Woolston (Jared.Woolston@gmail.com)
 */
public abstract class AsyncTransfer {

    private final UsbEndpoint endpoint;

    private ByteBuffer nativeObject;

    public AsyncTransfer(@NonNull UsbEndpoint endpoint) {
        this.endpoint = endpoint;
    }

    protected void setNativeObject(@Nullable ByteBuffer nativeObject) {
        if (this.nativeObject != null) {
            throw new IllegalStateException("The native object may only be set once!");
        }
        if (nativeObject == null) {
            throw new IllegalArgumentException("The native object may not be null!");
        }
        this.nativeObject = nativeObject;
    }

    @NonNull
    public ByteBuffer getNativeObject() {
        return nativeObject;
    }

    @NonNull
    public UsbEndpoint getEndpoint() {
        return endpoint;
    }

    @Override
    protected void finalize() throws Throwable {
        nativeDestroy(nativeObject);
        super.finalize();
    }

    private native void nativeDestroy(@NonNull ByteBuffer nativeObject);
}
