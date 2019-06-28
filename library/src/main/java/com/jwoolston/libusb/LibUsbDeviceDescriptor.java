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
        return new LibUsbDeviceDescriptor(nativeGetDeviceDescriptorFromHandle(device.getNativeObject()));
    }

    @NotNull
    static LibUsbDeviceDescriptor getDeviceDescriptor(long nativePointer) {
        return new LibUsbDeviceDescriptor(nativeGetDeviceDescriptorFromDevice(nativePointer));
    }

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

    public int getVendorId() {
        return nativeGetVendorId(getNativeObject());
    }

    public int getProductId() {
        return nativeGetProductId(getNativeObject());
    }

    public int getDeviceClass() {
        return nativeGetDeviceClass(getNativeObject());
    }

    public int getDeviceSubclass() {
        return nativeGetDeviceSubclass(getNativeObject());
    }

    public int getDeviceProtocol() {
        return nativeGetDeviceProtocol(getNativeObject());
    }

    @Nullable
    private static native ByteBuffer nativeGetDeviceDescriptorFromHandle(@NotNull ByteBuffer device);

    @Nullable
    private static native ByteBuffer nativeGetDeviceDescriptorFromDevice(long nativePointer);

    private static native void nativeDestroy(@NotNull ByteBuffer descriptor);

    private static native int nativeGetVendorId(@NotNull ByteBuffer descriptor);

    private static native int nativeGetProductId(@NotNull ByteBuffer descriptor);

    private static native int nativeGetDeviceClass(@NotNull ByteBuffer descriptor);

    private static native int nativeGetDeviceSubclass(@NotNull ByteBuffer descriptor);

    private static native int nativeGetDeviceProtocol(@NotNull ByteBuffer descriptor);
}
