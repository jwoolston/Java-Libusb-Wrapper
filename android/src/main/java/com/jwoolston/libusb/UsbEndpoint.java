package com.jwoolston.libusb;

import android.os.Parcel;
import android.os.Parcelable;

public class UsbEndpoint extends BaseUsbEndpoint implements Parcelable {

    public static final Parcelable.Creator<UsbEndpoint> CREATOR =
            new Parcelable.Creator<UsbEndpoint>() {
                public UsbEndpoint createFromParcel(Parcel in) {
                    int address = in.readInt();
                    int attributes = in.readInt();
                    int maxPacketSize = in.readInt();
                    int interval = in.readInt();
                    return new UsbEndpoint(address, attributes, maxPacketSize, interval);
                }

                public UsbEndpoint[] newArray(int size) {
                    return new UsbEndpoint[size];
                }
            };

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

    public int describeContents() {
        return 0;
    }

    public void writeToParcel(Parcel parcel, int flags) {
        parcel.writeInt(address);
        parcel.writeInt(attributes);
        parcel.writeInt(maxPacketSize);
        parcel.writeInt(interval);
    }
}
