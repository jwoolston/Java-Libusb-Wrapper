package com.jwoolston.libusb;

public class UsbEndpoint extends BaseUsbEndpoint {

    /**
     * UsbEndpoint should only be instantiated by UsbService implementation
     *
     * @param address
     * @param attributes
     * @param maxPacketSize
     * @param interval
     */
    UsbEndpoint(int address, int attributes, int maxPacketSize, int interval) {
        super(address, attributes, maxPacketSize, interval);
    }
}
