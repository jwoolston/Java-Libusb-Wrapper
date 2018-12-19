package com.jwoolston.libusb.async;

import java.io.IOException;
import java.nio.ByteBuffer;
import org.jetbrains.annotations.Nullable;

/**
 * @author Jared Woolston (Jared.Woolston@gmail.com)
 */
public interface InterruptTransferCallback {

    void onInterruptTransferComplete(@Nullable ByteBuffer data, int result) throws IOException;
}
