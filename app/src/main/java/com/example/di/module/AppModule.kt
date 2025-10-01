package com.example.di.module

import android.app.Application
import android.content.Context
import com.example.DLApplication
import com.example.di.qualifier.ApplicationContext
import com.example.util.downloaders.NotificationReceiver
import com.example.util.scheduler.BaseSchedulers
import com.example.util.scheduler.BaseSchedulersImpl
import dagger.Binds
import dagger.Module
import dagger.android.ContributesAndroidInjector
import javax.inject.Singleton

@Module
abstract class AppModule {

    @Binds
    @ApplicationContext
    abstract fun bindApplicationContext(application: DLApplication): Context

    @Binds
    abstract fun bindApplication(application: DLApplication): Application

    @Singleton
    @Binds
    abstract fun bindBaseSchedulers(baseSchedulers: BaseSchedulersImpl): BaseSchedulers

    @ContributesAndroidInjector
    abstract fun contributesNotificationReceiver(): NotificationReceiver
}