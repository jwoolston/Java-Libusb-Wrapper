package com.jwoolston.android.libusb;

import org.jetbrains.annotations.NotNull;

/**
 * Speed codes. Indicates the speed at which the device is operating.
 *
 * @author Jared Woolston (Jared.Woolston@gmail.com)
 */
public enum LibusbSpeed {

    /** The OS doesn't report or know the device speed */
    LIBUSB_SPEED_UNKNOWN (0),

    /** The device is operating at low speed (1.5MBit/s) */
    LIBUSB_SPEED_LOW (1),

    /** The device is operating at full speed (12MBit/s) */
    LIBUSB_SPEED_FULL (2),

    /** The device is operating at high speed (480MBit/s) */
    LIBUSB_SPEED_HIGH (3),

    /** The device is operating at super speed (5000MBit/s) */
    LIBUSB_SPEED_SUPER (4);

    public final int code;

    LibusbSpeed(int code) {
        this.code = code;
    }

    @NotNull
    public static LibusbSpeed fromNative(int code) {
        for (LibusbSpeed error : values()) {
            if (error.code == code) {
                return error;
            }
        }
        return LIBUSB_SPEED_UNKNOWN;
    }
}
