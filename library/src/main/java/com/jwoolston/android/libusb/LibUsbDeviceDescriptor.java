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

    @NonNull
    static LibUsbDeviceDescriptor getDeviceDescriptor(@NonNull UsbDevice device) {
        return new LibUsbDeviceDescriptor(nativeGetDeviceDescriptor(device));
    }

    @Nullable
    private static native ByteBuffer nativeGetDeviceDescriptor(@NonNull UsbDevice device);

    private LibUsbDeviceDescriptor(ByteBuffer nativeObject) {
        Preconditions.checkNotNull(nativeObject, "LibUsbDeviceDescriptor Initialization failed.");
        this.nativeObject = nativeObject;
    }

    @NonNull
    ByteBuffer getNativeObject() {
        return nativeObject;
    }
}
