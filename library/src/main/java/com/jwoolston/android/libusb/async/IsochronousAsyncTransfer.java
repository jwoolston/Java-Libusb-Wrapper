package com.jwoolston.android.libusb.async;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import com.jwoolston.android.libusb.LibusbError;
import com.jwoolston.android.libusb.UsbDeviceConnection;
import com.jwoolston.android.libusb.UsbEndpoint;
import java.io.IOException;
import java.nio.ByteBuffer;

/**
 * @author Jared Woolston (Jared.Woolston@gmail.com)
 */
public class IsochronousAsyncTransfer extends AsyncTransfer {

    private static final String TAG = "IsochronousAsyncTransfer";

    private final IsochronousTransferCallback callback;
    private final UsbDeviceConnection         connection;
    private final int                         packetCount;
    private final int                         packetSize;

    public IsochronousAsyncTransfer(@NonNull IsochronousTransferCallback callback, @NonNull UsbEndpoint endpoint,
                                    @NonNull UsbDeviceConnection connection, int packetCount)
            throws IOException {
        super(endpoint);
        this.callback = callback;
        this.connection = connection;
        setNativeObject(nativeAllocate(packetCount));
        int size = nativeSetupPackets(connection.getDevice().getNativeObject(), getNativeObject(),
                                      endpoint.getAddress());
        LibusbError result = size > 0 ? LibusbError.LIBUSB_SUCCESS : LibusbError.fromNative(size);
        if (result != LibusbError.LIBUSB_SUCCESS) {
            throw new IOException("Failed to setup packets: " + result);
        }
        this.packetCount = packetCount;
        packetSize = size;
    }

    public void submit(@NonNull ByteBuffer buffer, int timeout) throws IllegalStateException {
        if (!buffer.isDirect()) {
            throw new IllegalArgumentException("ByteBuffers passed to this method must be direct allocations.");
        }

        if ((packetCount * packetSize) > buffer.capacity()) {
            throw new IllegalArgumentException("The provided byte buffer is of insufficient capacity. Required: "
                                               + (packetSize * packetCount) + " Bytes. Provided: " + buffer.capacity()
                                               + " Bytes.");
        }

        LibusbError result = LibusbError.fromNative(connection.isochronousTransfer(callback, this, getEndpoint(),
                                                                                   buffer, timeout));
        if (result != LibusbError.LIBUSB_SUCCESS) {
            throw new IllegalStateException("Failed to submit isochronous transfer: " + result);
        }
    }

    @Nullable
    private native ByteBuffer nativeAllocate(int numberPackets);

    private native int nativeSetupPackets(@NonNull ByteBuffer nativeDevice, @NonNull ByteBuffer nativeObject,
                                          int endpoint);

}
