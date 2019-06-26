package com.jwoolston.libusb;

import org.jetbrains.annotations.Nullable;

public class UsbInterface extends BaseUsbInterface {

    /**
     * UsbInterface should only be instantiated by BaseUsbManager implementation
     *
     * @param id
     * @param alternateSetting
     * @param name
     * @param interfaceClass
     * @param subClass
     * @param protocol
     */
    UsbInterface(int id, int alternateSetting, @Nullable String name, int interfaceClass, int subClass, int protocol) {
        super(id, alternateSetting, name, interfaceClass, subClass, protocol);
    }

    @Override
    public UsbEndpoint getEndpoint(int index) {
        return (UsbEndpoint) super.getEndpoint(index);
    }
}
