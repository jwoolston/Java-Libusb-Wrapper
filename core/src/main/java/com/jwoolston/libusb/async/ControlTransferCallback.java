package com.jwoolston.libusb.async;

import java.io.IOException;
import java.nio.ByteBuffer;
import org.jetbrains.annotations.Nullable;

/**
 * @author Jared Woolston (Jared.Woolston@gmail.com)
 */
public interface ControlTransferCallback {

    void onControlTransferComplete(@Nullable ByteBuffer data, int result) throws IOException;
}
