package com.jwoolston.libusb;

import android.os.Parcel;
import android.os.Parcelable;

import org.jetbrains.annotations.Nullable;

public class UsbInterface extends BaseUsbInterface implements Parcelable {

    public static final Parcelable.Creator<UsbInterface> CREATOR =
            new Parcelable.Creator<UsbInterface>() {
                public UsbInterface createFromParcel(Parcel in) {
                    int id = in.readInt();
                    int alternateSetting = in.readInt();
                    String name = in.readString();
                    int Class = in.readInt();
                    int subClass = in.readInt();
                    int protocol = in.readInt();
                    Parcelable[] endpoints = in.readParcelableArray(UsbInterface.class.getClassLoader());
                    UsbInterface intf = new UsbInterface(id, alternateSetting, name, Class, subClass, protocol);
                    intf.setEndpoints((UsbEndpoint[]) endpoints);
                    return intf;
                }

                public UsbInterface[] newArray(int size) {
                    return new UsbInterface[size];
                }
            };

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

    public int describeContents() {
        return 0;
    }

    public void writeToParcel(Parcel parcel, int flags) {
        parcel.writeInt(id);
        parcel.writeInt(alternateSetting);
        parcel.writeString(name);
        parcel.writeInt(interfaceClass);
        parcel.writeInt(subclass);
        parcel.writeInt(protocol);
        parcel.writeParcelableArray((Parcelable[]) endpoints, 0);
    }

    @Override
    public UsbEndpoint getEndpoint(int index) {
        return (UsbEndpoint) super.getEndpoint(index);
    }
}
