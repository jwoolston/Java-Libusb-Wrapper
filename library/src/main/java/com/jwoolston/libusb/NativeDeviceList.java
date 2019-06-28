package com.jwoolston.libusb;

import org.jetbrains.annotations.NotNull;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

class NativeDeviceList {

    static {
        if (!nativeInitialize()) {
            throw new RuntimeException("Failed to initialize native layer for NativeDeviceList.");
        }
    }

    private long nativeObject;

    private List<BaseUsbDevice> devices = new ArrayList<>();

    @NotNull
    public static NativeDeviceList fromNativeObject(long nativeObject) {
        return new NativeDeviceList(nativeObject);
    }

    private NativeDeviceList(long nativeObject) {
        this.nativeObject = nativeObject;
        nativePopulateDeviceList(nativeObject, devices);
    }

    public void release() {
        nativeRelease(nativeObject);
        nativeObject = 0;
    }

    private static native boolean nativeInitialize();

    private static native void nativeGetDeviceList(@NotNull ByteBuffer nativeContext);

    private static native void nativePopulateDeviceList(long nativeObject,
                                                        @NotNull List<BaseUsbDevice> devices);

    private native void nativeRelease(long nativeObject);
}
