package com.example.di.module

import android.app.Application
import android.app.DownloadManager
import android.content.Context
import com.example.DLApplication
import com.example.util.AppUtil
import com.example.util.FileUtil
import com.example.util.IntentUtil
import com.example.util.NotificationsHelper
import com.example.util.SharedPrefHelper
import com.example.util.SystemUtil
import dagger.Module
import dagger.Provides
import javax.inject.Singleton

@Module
class UtilModule {

    @Singleton
    @Provides
    fun bindDownloadManager(application: Application): DownloadManager =
        application.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager

    @Singleton
    @Provides
    fun bindFileUtil() = FileUtil()

    @Singleton
    @Provides
    fun bindSystemUtil() = SystemUtil()

    @Singleton
    @Provides
    fun bindIntentUtil(fileUtil: FileUtil) = IntentUtil(fileUtil)

    @Singleton
    @Provides
    fun bindAppUtil() = AppUtil()

    @Singleton
    @Provides
    fun provideNotificationsHelper(dlApplication: DLApplication): NotificationsHelper {
        return NotificationsHelper(dlApplication.applicationContext)
    }

    @Singleton
    @Provides
    fun provideSharedPrefHelper(dlApplication: DLApplication, appUtil: AppUtil): SharedPrefHelper {
        return SharedPrefHelper(dlApplication.applicationContext, appUtil)
    }
}