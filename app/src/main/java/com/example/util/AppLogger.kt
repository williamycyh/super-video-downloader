package com.example.util

import android.util.Log
import com.example.BuildConfig
import com.example.DLApplication

class AppLogger {

    companion object {

        private const val TAG = DLApplication.DEBUG_TAG

        fun d(message: String) {
            if (BuildConfig.DEBUG) {
                Log.d(TAG, message)
            }
        }

        fun i(message: String) {
            if (BuildConfig.DEBUG) {
                Log.i(TAG, message)
            }
        }

        fun w(message: String) {
            if (BuildConfig.DEBUG) {
                Log.w(TAG, message)
            }
        }

        fun e(message: String) {
            if (BuildConfig.DEBUG) {
                Log.e(TAG, message)
            }
        }
    }
}