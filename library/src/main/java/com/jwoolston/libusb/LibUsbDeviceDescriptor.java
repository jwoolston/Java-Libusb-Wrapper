package com.jwoolston.libusb;

import com.jwoolston.libusb.util.Preconditions;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.nio.ByteBuffer;

/**
 * @author Jared Woolston (Jared.Woolston@gmail.com)
 */
public class LibUsbDeviceDescriptor {

    private final ByteBuffer nativeObject;

    private boolean isValid = true;

    @NotNull
    static LibUsbDeviceDescriptor getDeviceDescriptor(@NotNull BaseUsbDevice device) {
        return new LibUsbDeviceDescriptor(nativeGetDeviceDescriptor(device.getNativeObject()));
    }

    @Nullable
    private static native ByteBuffer nativeGetDeviceDescriptor(@NotNull ByteBuffer device);

    private static native void nativeDestroy(@NotNull ByteBuffer descriptor);

    private LibUsbDeviceDescriptor(ByteBuffer nativeObject) {
        Preconditions.checkNotNull(nativeObject, "LibUsbDeviceDescriptor Initialization failed.");
        this.nativeObject = nativeObject;
    }

    @NotNull
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
