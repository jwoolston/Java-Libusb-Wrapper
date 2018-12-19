package com.jwoolston.libusb;

import com.jwoolston.libusb.util.Preconditions;

import java.nio.ByteBuffer;
import org.jetbrains.annotations.NotNull;

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
