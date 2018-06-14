package com.jwoolston.android.libusb;

import android.support.annotation.NonNull;

import com.jwoolston.android.libusb.util.Preconditions;

import java.nio.ByteBuffer;

/**
 * @author Jared Woolston (Jared.Woolston@gmail.com)
 */
public class LibUsbContext {

    private final ByteBuffer buffer;

    LibUsbContext(ByteBuffer buffer) {
        Preconditions.checkNotNull(buffer, "LibUSB Initialization failed.");
        this.buffer = buffer;
    }

    @NonNull
    ByteBuffer getBuffer() {
        return buffer;
    }
}
