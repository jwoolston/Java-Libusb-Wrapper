package com.jwoolston.libusb;

import com.jwoolston.libusb.util.Preconditions;
import java.nio.ByteBuffer;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * @author Jared Woolston (Jared.Woolston@gmail.com)
 */
public class LibUsbDeviceDescriptor {

    private final ByteBuffer nativeObject;

    private boolean isValid = true;

    @NotNull
    static LibUsbDeviceDescriptor getDeviceDescriptor(@NotNull UsbDevice device) {
        return new LibUsbDeviceDescriptor(nativeGetDeviceDescriptor(device.getNativeObject()));
    }

    @Nullable
    private static native ByteBuffer nativeGetDeviceDescriptor(@NotNull ByteBuffer device);

    private static native void nativeDestroy(@NotNull ByteBuffer descriptor);

    private LibUsbDeviceDescriptor(ByteBuffer nativeObject) {
        Preconditions.checkNotNull(nativeObject, "LibUsbDeviceDescriptor Initialization failed.");
        this.nativeObject = nativeObject;
    }

    private void isValidOrThrow() {
        if (!isValid) {
            throw new IllegalStateException("Descriptor is no longer valid.");
        }
    }

    @NotNull
    ByteBuffer getNativeObject() {
        isValidOrThrow();
        return nativeObject;
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

    public int getVendorId() {
        isValidOrThrow();
        return nativeObject.getShort(9);
    }

    public int getProductId() {
        isValidOrThrow();
        return nativeObject.getShort(11);
    }

    public int getDeviceClass() {
        isValidOrThrow();
        return nativeObject.get(5);
    }

    public int getDeviceSubclass() {
        isValidOrThrow();
        return nativeObject.get(6);
    }

    public int getDeviceProtocol() {
        isValidOrThrow();
        return nativeObject.get(7);
    }
}
