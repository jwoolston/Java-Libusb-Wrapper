package com.jwoolston.libusb.async;


import com.jwoolston.libusb.UsbEndpoint;
import java.nio.ByteBuffer;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * @author Jared Woolston (Jared.Woolston@gmail.com)
 */
public abstract class AsyncTransfer {

    private final UsbEndpoint endpoint;

    private ByteBuffer nativeObject;

    public AsyncTransfer(@NotNull UsbEndpoint endpoint) {
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

    @NotNull
    public ByteBuffer getNativeObject() {
        return nativeObject;
    }

    @NotNull
    public UsbEndpoint getEndpoint() {
        return endpoint;
    }

    @Override
    protected void finalize() throws Throwable {
        nativeDestroy(nativeObject);
        super.finalize();
    }

    private native void nativeDestroy(@NotNull ByteBuffer nativeObject);
}
