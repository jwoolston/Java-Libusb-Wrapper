package com.jwoolston.libusb;

import com.jwoolston.libusb.util.Preconditions;

import org.jetbrains.annotations.NotNull;

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

    @NotNull
    public ByteBuffer getNativeObject() {
        return nativeObject;
    }
}
