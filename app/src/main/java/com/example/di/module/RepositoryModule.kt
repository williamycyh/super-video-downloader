package com.example.di.module

import com.example.data.local.AdBlockHostsLocalDataSource
import com.example.data.local.ConfigLocalDataSource
import com.example.data.local.HistoryLocalDataSource
import com.example.data.local.ProgressLocalDataSource
import com.example.data.local.TopPagesLocalDataSource
import com.example.data.local.VideoLocalDataSource
import com.example.data.remote.AdBlockHostsRemoteDataSource
import com.example.data.remote.ConfigRemoteDataSource
import com.example.data.remote.TopPagesRemoteDataSource
import com.example.data.remote.VideoRemoteDataSource
import com.example.data.repository.AdBlockHostsRepository
import com.example.data.repository.AdBlockHostsRepositoryImpl
import com.example.data.repository.ConfigRepository
import com.example.data.repository.ConfigRepositoryImpl
import com.example.data.repository.HistoryRepository
import com.example.data.repository.HistoryRepositoryImpl
import com.example.data.repository.ProgressRepository
import com.example.data.repository.ProgressRepositoryImpl
import com.example.data.repository.TopPagesRepository
import com.example.data.repository.TopPagesRepositoryImpl
import com.example.data.repository.VideoRepository
import com.example.data.repository.VideoRepositoryImpl
import com.example.di.qualifier.LocalData
import com.example.di.qualifier.RemoteData
import dagger.Binds
import dagger.Module
import javax.inject.Singleton

@Module
abstract class RepositoryModule {

    @Singleton
    @Binds
    @LocalData
    abstract fun bindConfigLocalDataSource(localDataSource: ConfigLocalDataSource): ConfigRepository

    @Singleton
    @Binds
    @RemoteData
    abstract fun bindConfigRemoteDataSource(remoteDataSource: ConfigRemoteDataSource): ConfigRepository

    @Singleton
    @Binds
    abstract fun bindConfigRepositoryImpl(configRepository: ConfigRepositoryImpl): ConfigRepository

    @Singleton
    @Binds
    @LocalData
    abstract fun bindTopPagesLocalDataSource(localDataSource: TopPagesLocalDataSource): TopPagesRepository

    @Singleton
    @Binds
    @RemoteData
    abstract fun bindTopPagesRemoteDataSource(remoteDataSource: TopPagesRemoteDataSource): TopPagesRepository

    @Singleton
    @Binds
    abstract fun bindTopPagesRepositoryImpl(topPagesRepository: TopPagesRepositoryImpl): TopPagesRepository

    @Singleton
    @Binds
    @LocalData
    abstract fun bindVideoLocalDataSource(localDataSource: VideoLocalDataSource): VideoRepository

    @Singleton
    @Binds
    @RemoteData
    abstract fun bindVideoRemoteDataSource(remoteDataSource: VideoRemoteDataSource): VideoRepository

    @Singleton
    @Binds
    abstract fun bindVideoRepositoryImpl(videoRepository: VideoRepositoryImpl): VideoRepository

    @Singleton
    @Binds
    @LocalData
    abstract fun bindProgressLocalDataSource(localDataSource: ProgressLocalDataSource): ProgressRepository

    @Singleton
    @Binds
    @LocalData
    abstract fun bindHistoryLocalDataSource(localDataSource: HistoryLocalDataSource): HistoryRepository


    @Singleton
    @Binds
    abstract fun bindProgressRepositoryImpl(progressRepository: ProgressRepositoryImpl): ProgressRepository

    @Singleton
    @Binds
    abstract fun bindHistoryRepositoryImpl(historyRepository: HistoryRepositoryImpl): HistoryRepository

    @Singleton
    @Binds
    @LocalData
    abstract fun bindAdBlockHostsLocalDataSource(adBlockHostsLocalDataSource: AdBlockHostsLocalDataSource): AdBlockHostsRepository

    @Singleton
    @Binds
    @RemoteData
    abstract fun bindAdBlockHostsRemoteDataSource(adBlockHostsRemoteDataSource: AdBlockHostsRemoteDataSource): AdBlockHostsRepository

    @Singleton
    @Binds
    abstract fun bindAdBlockHostsRepositoryImpl(adBlockHostsRepository: AdBlockHostsRepositoryImpl): AdBlockHostsRepository

}