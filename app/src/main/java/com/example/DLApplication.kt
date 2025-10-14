package com.example

import android.content.Context
import androidx.work.Configuration
import androidx.work.WorkManager
import com.example.di.component.DaggerAppComponent
import com.example.tubedown.rereads.AdCenter.Companion.initMobpuAd
import com.example.util.AppLogger
import com.example.util.ContextUtils
import com.example.util.FileUtil
import com.example.util.SharedPrefHelper
import com.example.util.downloaders.generic_downloader.DaggerWorkerFactory
import com.google.firebase.FirebaseApp
import com.btdlp.BtdJava
import dagger.android.AndroidInjector
import dagger.android.DaggerApplication
import io.reactivex.rxjava3.plugins.RxJavaPlugins
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.File
import javax.inject.Inject

open class DLApplication : DaggerApplication() {
    companion object {
        const val DEBUG_TAG: String = "BUBE_DEBUG_TAG"
    }

    private lateinit var androidInjector: AndroidInjector<out DaggerApplication>

    @Inject
    lateinit var workerFactory: DaggerWorkerFactory

    @Inject
    lateinit var sharedPrefHelper: SharedPrefHelper

    @Inject
    lateinit var fileUtil: FileUtil

    override fun attachBaseContext(base: Context?) {
        super.attachBaseContext(base)

        androidInjector = DaggerAppComponent.builder().application(this).build()
    }

    public override fun applicationInjector(): AndroidInjector<out DaggerApplication> =
        androidInjector

    override fun onCreate() {
        super.onCreate()

        ContextUtils.initApplicationContext(applicationContext)

        initializeFileUtils()

        val file: File = fileUtil.folderDir
        val ctx = applicationContext

        WorkManager.initialize(
            ctx,
            Configuration.Builder()
                .setWorkerFactory(workerFactory).build()
        )

        RxJavaPlugins.setErrorHandler { error: Throwable? ->
            AppLogger.e("RxJavaError unhandled $error")
        }

        CoroutineScope(Dispatchers.Default).launch {
            if (!file.exists()) {
                file.mkdirs()
            }

            initializeBtdJava()
        }

        // initialize firebase
        FirebaseApp.initializeApp(this)
        initMobpuAd(this)
    }

    private fun initializeFileUtils() {
        val isExternal = sharedPrefHelper.getIsExternalUse()
        val isAppDir = sharedPrefHelper.getIsAppDirUse()

        FileUtil.IS_EXTERNAL_STORAGE_USE = isExternal
        FileUtil.IS_APP_DATA_DIR_USE = isAppDir
        FileUtil.INITIIALIZED = true
    }

    private fun initializeBtdJava() {
        try {
            // Initialize custom Bube download Java library
            AppLogger.d("Custom Bube download Java library initialized successfully")
        } catch (e: Exception) {
            AppLogger.e("failed to initialize Bube download Java library $e")
        }
    }
}

