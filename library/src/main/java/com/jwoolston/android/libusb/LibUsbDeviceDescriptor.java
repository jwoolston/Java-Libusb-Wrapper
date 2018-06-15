package com.jwoolston.android.libusb;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import com.jwoolston.android.libusb.util.Preconditions;
import java.nio.ByteBuffer;

/**
 * @author Jared Woolston (Jared.Woolston@gmail.com)
 */
public class LibUsbDeviceDescriptor {

    private final ByteBuffer nativeObject;

    private boolean isValid = true;

    @NonNull
    static LibUsbDeviceDescriptor getDeviceDescriptor(@NonNull UsbDevice device) {
        return new LibUsbDeviceDescriptor(nativeGetDeviceDescriptor(device.getNativeObject()));
    }

    @Nullable
    private static native ByteBuffer nativeGetDeviceDescriptor(@NonNull ByteBuffer device);

    private static native void nativeDestroy(@NonNull ByteBuffer descriptor);

    private LibUsbDeviceDescriptor(ByteBuffer nativeObject) {
        Preconditions.checkNotNull(nativeObject, "LibUsbDeviceDescriptor Initialization failed.");
        this.nativeObject = nativeObject;
    }

    @NonNull
    ByteBuffer getNativeObject() {
        if (isValid) {
            return nativeObject;
        } else {
            throw new IllegalStateException("Descriptor is no longer valid.");
        }
    }

    public void destroy() {
        nativeDestroy(nativeObject);
        isValid = false;
    }

    @Override
    protected void finalize() throws Throwable {
        if (isValid) {
            destroy();
        }
        super.finalize();
    }
}
