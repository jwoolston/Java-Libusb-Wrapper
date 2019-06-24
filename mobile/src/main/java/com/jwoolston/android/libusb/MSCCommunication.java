package com.jwoolston.android.libusb;

import com.jwoolston.libusb.LibusbError;
import com.jwoolston.libusb.UsbDeviceConnection;
import com.jwoolston.libusb.UsbEndpoint;
import com.jwoolston.libusb.async.BulkTransferCallback;
import com.jwoolston.android.libusb.msc_test_core.usb.UsbCommunication;
import java.io.IOException;
import java.nio.ByteBuffer;

/**
 * @author Jared Woolston (Jared.Woolston@gmail.com)
 */
public class MSCCommunication implements UsbCommunication {

    private UsbDeviceConnection deviceConnection;
    private UsbEndpoint outEndpoint;
    private UsbEndpoint inEndpoint;

    MSCCommunication(UsbDeviceConnection deviceConnection, UsbEndpoint outEndpoint, UsbEndpoint inEndpoint) {
        this.deviceConnection = deviceConnection;
        this.outEndpoint = outEndpoint;
        this.inEndpoint = inEndpoint;
    }

    @Override
    public int bulkOutTransfer(ByteBuffer src) throws IOException {
        int result = deviceConnection.bulkTransfer(outEndpoint,
                                                   src.array(), src.position(), src.remaining(), TRANSFER_TIMEOUT);

        if (result < 0) {
            throw new IOException("Could not write to device, result == " + LibusbError.fromNative(result));
        }

        src.position(src.position() + result);
        return result;
    }

    @Override
    public int bulkInTransfer(ByteBuffer dest) throws IOException {
        int result = deviceConnection.bulkTransfer(inEndpoint,
                                                   dest.array(), dest.position(), dest.remaining(), TRANSFER_TIMEOUT);

        if (result < 0) {
            throw new IOException("Could not read from device, result == " + LibusbError.fromNative(result));
        }

        dest.position(dest.position() + result);
        return result;
    }

    /**
     * Performs a bulk out transfer beginning at the offset specified in the
     * <code>buffer</code> of length <code>buffer#remaining()</code>.
     *
     * @param src The data to transfer.
     *
     * @return Bytes transmitted if successful.
     */
    @Override
    public LibusbError asyncBulkOutTransfer(BulkTransferCallback callback, ByteBuffer src) throws IOException {
        LibusbError result = deviceConnection.bulkTransferAsync(callback, outEndpoint, src.array(), src.position(), src.remaining(), TRANSFER_TIMEOUT);

        if (result != LibusbError.LIBUSB_SUCCESS) {
            throw new IOException("Could not write to device, result == " + result);
        }

        return result;
    }

    /**
     * Performs a bulk in transfer beginning at offset zero in the
     * <code>buffer</code> of length <code>buffer#remaining()</code>.
     *
     * @param dest The buffer where data should be transferred.
     *
     * @return Bytes read if successful.
     */
    @Override
    public LibusbError asyncBulkInTransfer(BulkTransferCallback callback, ByteBuffer dest) throws IOException {
        LibusbError result = deviceConnection.bulkTransferAsync(callback, inEndpoint, dest.array(), dest.position(), dest.remaining(), TRANSFER_TIMEOUT);

        if (result != LibusbError.LIBUSB_SUCCESS) {
            throw new IOException("Could not read from device, result == " + result);
        }

        return result;
    }
}
