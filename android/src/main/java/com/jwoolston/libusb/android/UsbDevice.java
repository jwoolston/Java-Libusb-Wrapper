package com.jwoolston.libusb.android;

import android.hardware.usb.UsbDeviceConnection;
import com.jwoolston.libusb.LibUsbContext;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.nio.ByteBuffer;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * @author Jared Woolston (jwoolston@keywcorp.com)
 */
public class UsbDevice extends com.jwoolston.libusb.UsbDevice {

    @NotNull
    private final android.hardware.usb.UsbDevice device;

    private final android.hardware.usb.UsbDeviceConnection connection;

    private int fileDescriptor;

    @NotNull
    static UsbDevice fromAndroidDevice(@NotNull LibUsbContext context, @NotNull android.hardware.usb.UsbDevice device,
                                                            @NotNull android.hardware.usb.UsbDeviceConnection connection) {
        return new UsbDevice(connection, device, wrapDevice(context.getNativeObject(), connection.getFileDescriptor()));
    }

    private UsbDevice(@NotNull android.hardware.usb.UsbDeviceConnection connection,
                      @NotNull android.hardware.usb.UsbDevice device, @Nullable ByteBuffer nativeObject) {
        super(nativeObject);
        this.device = device;
        this.connection = connection;
        fileDescriptor = connection.getFileDescriptor();
    }

    @NotNull
    public android.hardware.usb.UsbDevice getAndroidDevice() {
        return device;
    }

    @NotNull
    public UsbDeviceConnection getConnection() {
        return connection;
    }

    public int getFileDescriptor() {
        return fileDescriptor;
    }

    /**
     * Returns a unique integer ID for the device. This is a convenience for clients that want to use an integer to
     * represent the device, rather than the device name. IDs are not persistent across USB disconnects.
     *
     * @return the device ID
     */
    public int getDeviceId() {
        return device.getDeviceId();
    }

    @Override
    protected void writeOut(ObjectOutput out) throws IOException {
        out.writeInt(fileDescriptor);
    }

    @Override
    protected void readIn(ObjectInput in) throws IOException {
        fileDescriptor = in.readInt();
    }
}
