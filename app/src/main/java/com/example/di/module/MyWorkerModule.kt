package com.example.di.module

import androidx.work.WorkerFactory
import com.example.data.repository.ProgressRepository
import com.example.util.FileUtil
import com.example.util.NotificationsHelper
import com.example.util.SharedPrefHelper
import com.example.util.downloaders.generic_downloader.DaggerWorkerFactory
import com.example.util.proxy_utils.CustomProxyController
import com.example.util.proxy_utils.OkHttpProxyClient
import dagger.Module
import dagger.Provides
import javax.inject.Singleton

@Module
class MyWorkerModule {
    @Provides
    @Singleton
    fun workerFactory(
        progressRepository: ProgressRepository,
        fileUtil: FileUtil,
        notificationsHelper: NotificationsHelper,
        proxyController: CustomProxyController,
        okHttpProxyClient: OkHttpProxyClient,
        sharedPrefHelper: SharedPrefHelper
    ): WorkerFactory {
        return DaggerWorkerFactory(
            progressRepository,
            fileUtil,
            notificationsHelper,
            proxyController,
            okHttpProxyClient,
            sharedPrefHelper
        )
    }
}

