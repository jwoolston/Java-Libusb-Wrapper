package com.jwoolston.libusb;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class UsbConfiguration extends BaseUsbConfiguration {

    /**
     * UsbConfiguration should only be instantiated by UsbService implementation
     *
     * @param id
     * @param name
     * @param attributes
     * @param maxPower
     */
    public UsbConfiguration(int id, @Nullable String name, int attributes, int maxPower) {
        super(id, name, attributes, maxPower);
    }

    @Override
    @NotNull
    public UsbInterface getInterface(int index) {
        return (UsbInterface) super.getInterface(index);
    }
}
