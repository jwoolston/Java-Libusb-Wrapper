package com.jwoolston.libusb;

import android.os.Parcel;
import android.os.Parcelable;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class UsbConfiguration extends BaseUsbConfiguration implements Parcelable {

    public static final Parcelable.Creator<UsbConfiguration> CREATOR =
            new Parcelable.Creator<UsbConfiguration>() {
                public UsbConfiguration createFromParcel(Parcel in) {
                    int id = in.readInt();
                    String name = in.readString();
                    int attributes = in.readInt();
                    int maxPower = in.readInt();
                    UsbInterface[] interfaces = (UsbInterface[]) in.readParcelableArray(UsbInterface.class.getClassLoader());
                    UsbConfiguration configuration = new UsbConfiguration(id, name, attributes, maxPower);
                    configuration.setInterfaces(interfaces);
                    return configuration;
                }

                public UsbConfiguration[] newArray(int size) {
                    return new UsbConfiguration[size];
                }
            };

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

    public int describeContents() {
        return 0;
    }

    public void writeToParcel(Parcel parcel, int flags) {
        parcel.writeInt(id);
        parcel.writeString(name);
        parcel.writeInt(attributes);
        parcel.writeInt(maxPower);
        parcel.writeParcelableArray((Parcelable[]) interfaces, 0);
    }

    @Override
    @NotNull
    public UsbInterface getInterface(int index) {
        return (UsbInterface) super.getInterface(index);
    }
}
