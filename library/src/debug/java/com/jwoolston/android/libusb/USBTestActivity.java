package com.jwoolston.android.libusb;

import android.app.Activity;
import android.os.Bundle;
import android.util.Log;

/**
 * @author Jared Woolston (Jared.Woolston@gmail.com)
 */
public class USBTestActivity extends Activity {

    private static final String TAG = "USBTestActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d(TAG, "Creating USBTestActivity.");
    }

    @Override
    protected void onPause() {
        overridePendingTransition(0, 0);
        super.onPause();
    }
}
