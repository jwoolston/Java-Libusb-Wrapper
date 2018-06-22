package com.jwoolston.android.libusb;

import android.support.annotation.NonNull;

import com.jwoolston.android.libusb.util.Preconditions;

import java.nio.ByteBuffer;

/**
 * @author Jared Woolston (Jared.Woolston@gmail.com)
 */
public class LibUsbContext {

    private final ByteBuffer nativeObject;

    LibUsbContext(ByteBuffer nativeObject) {
        Preconditions.checkNotNull(nativeObject, "LibUSB Initialization failed.");
        this.nativeObject = nativeObject;
    }

    @NonNull
    public ByteBuffer getNativeObject() {
        return nativeObject;
    }
}
