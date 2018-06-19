package com.jwoolston.android.libusb;

import android.util.Log;

import com.jwoolston.android.libusb.msc_test_core.usb.UsbCommunication;
import java.io.IOException;
import java.nio.ByteBuffer;

/**
 * @author Jared Woolston (Jared.Woolston@gmail.com)
 */
public class SynchronousMSC implements UsbCommunication {

    private UsbDeviceConnection deviceConnection;
    private UsbEndpoint outEndpoint;
    private UsbEndpoint inEndpoint;

    SynchronousMSC(UsbDeviceConnection deviceConnection, UsbEndpoint outEndpoint, UsbEndpoint inEndpoint) {
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
        } else {
            Log.v("SynchronousMSC","Read " + result + " bytes from device");
        }

        dest.position(dest.position() + result);
        return result;
    }
}
