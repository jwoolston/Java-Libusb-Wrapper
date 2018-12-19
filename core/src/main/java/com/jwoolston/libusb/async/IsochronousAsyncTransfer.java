package com.jwoolston.libusb.async;

import com.jwoolston.libusb.LibusbError;
import com.jwoolston.libusb.UsbDeviceConnection;
import com.jwoolston.libusb.UsbEndpoint;
import java.io.IOException;
import java.nio.ByteBuffer;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * @author Jared Woolston (Jared.Woolston@gmail.com)
 */
public class IsochronousAsyncTransfer extends AsyncTransfer {

    private static final String TAG = "IsochronousAsyncTransfer";

    private final IsochronousTransferCallback callback;
    private final UsbDeviceConnection         connection;
    private final int                         packetCount;
    private final int                         packetSize;

    public IsochronousAsyncTransfer(@NotNull IsochronousTransferCallback callback, @NotNull UsbEndpoint endpoint,
                                    @NotNull UsbDeviceConnection connection, int packetSize, int packetCount)
            throws IOException {
        super(endpoint);
        this.callback = callback;
        this.connection = connection;
        setNativeObject(nativeAllocate(packetCount));
        int size = nativeSetupPackets(connection.getDevice().getNativeObject(), getNativeObject(),
                                      endpoint.getAddress(), packetSize);
        LibusbError result = size > 0 ? LibusbError.LIBUSB_SUCCESS : LibusbError.fromNative(size);
        if (result != LibusbError.LIBUSB_SUCCESS) {
            throw new IOException("Failed to setup packets: " + result);
        }
        this.packetCount = packetCount;
        this.packetSize = packetSize;
    }

    public void submit(@NotNull ByteBuffer buffer, int timeout) throws IllegalStateException {
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

    private native int nativeSetupPackets(@NotNull ByteBuffer nativeDevice, @NotNull ByteBuffer nativeObject,
                                          int endpoint, int packetSize);

}
