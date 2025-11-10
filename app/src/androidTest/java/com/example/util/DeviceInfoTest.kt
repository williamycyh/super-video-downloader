package com.example.util

import android.os.Build
import android.util.Log
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.google.android.gms.ads.identifier.AdvertisingIdClient
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class DeviceInfoTest {

    @Test
    fun logDeviceInfoAndAdvertisingId() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext

        val manufacturer = Build.MANUFACTURER
        val model = Build.MODEL
        val sdkInt = Build.VERSION.SDK_INT

        runCatching {
            AdvertisingIdClient.getAdvertisingIdInfo(context)
        }.onSuccess { info ->
            Log.i(TAG, "Manufacturer: $manufacturer")
            Log.i(TAG, "Model: $model")
            Log.i(TAG, "SDK: $sdkInt")
            Log.i(TAG, "GAID: ${info?.id.orEmpty()}")
            Log.i(TAG, "LimitAdTrackingEnabled: ${info?.isLimitAdTrackingEnabled ?: false}")
        }.onFailure { error ->
            Log.e(TAG, "Failed to retrieve GAID: ${error.message}", error)
        }
    }

    companion object {
        private const val TAG = "DeviceInfoTest"
    }
}

