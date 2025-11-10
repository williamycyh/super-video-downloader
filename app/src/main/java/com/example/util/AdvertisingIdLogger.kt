package com.example.util

import android.content.Context
import android.os.Build
import android.util.Log
import com.google.android.gms.ads.identifier.AdvertisingIdClient

object AdvertisingIdLogger {

    private const val TAG = "AdvertisingIdLogger"

    fun logDeviceInfoAndGaid(context: Context) {
        Thread {
            runCatching {
                AdvertisingIdClient.getAdvertisingIdInfo(context.applicationContext)
            }.onSuccess { info ->
                if (info == null) {
                    Log.w(TAG, "Advertising ID info is null")
                    return@onSuccess
                }

                Log.i(TAG, "Manufacturer: ${Build.MANUFACTURER}")
                Log.i(TAG, "Model: ${Build.MODEL}")
                Log.i(TAG, "SDK: ${Build.VERSION.SDK_INT}")
                Log.i(TAG, "GAID: ${info.id}")
                Log.i(TAG, "LimitAdTrackingEnabled: ${info.isLimitAdTrackingEnabled}")
            }.onFailure { error ->
                Log.e(TAG, "Failed to retrieve GAID: ${error.message}", error)
            }
        }.start()
    }
}

