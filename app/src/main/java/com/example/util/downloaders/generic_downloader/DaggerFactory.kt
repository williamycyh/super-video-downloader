package com.example.util.downloaders.generic_downloader

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerFactory
import androidx.work.WorkerParameters
import com.example.data.repository.ProgressRepository
import com.example.util.FileUtil
import com.example.util.NotificationsHelper
import com.example.util.SharedPrefHelper
import com.example.util.downloaders.generic_downloader.workers.GenericDownloadWorker
import com.example.util.downloaders.generic_downloader.workers.GenericDownloadWorkerWrapper
import com.example.util.proxy_utils.CustomProxyController
import com.example.util.proxy_utils.OkHttpProxyClient
import javax.inject.Inject


class DaggerWorkerFactory @Inject constructor(
    private val progress: ProgressRepository,
    private val fileUtil: FileUtil,
    private val notificationsHelper: NotificationsHelper,
    private val proxyController: CustomProxyController,
    private val okHttpProxyClient: OkHttpProxyClient,
    private val sharedPrefHelper: SharedPrefHelper
) : WorkerFactory() {

    override fun createWorker(
        appContext: Context, workerClassName: String, workerParameters: WorkerParameters
    ): CoroutineWorker? {

        val workerKlass =
            Class.forName(workerClassName).asSubclass(GenericDownloadWorker::class.java)
        val constructor =
            workerKlass.getDeclaredConstructor(Context::class.java, WorkerParameters::class.java)
        val instance = constructor.newInstance(appContext, workerParameters)

        when (instance) {
            is GenericDownloadWorkerWrapper -> {
                instance.sharedPrefHelper = sharedPrefHelper
                instance.progressRepository = progress
                instance.fileUtil = fileUtil
                instance.notificationsHelper = notificationsHelper
                instance.proxyController = proxyController
                instance.proxyOkHttpClient = okHttpProxyClient
            }
        }

        return instance
    }
}