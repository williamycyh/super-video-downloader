package com.example.data.repository

import androidx.annotation.VisibleForTesting
import com.example.data.local.room.entity.SupportedPage
import com.example.di.qualifier.LocalData
import com.example.di.qualifier.RemoteData
import io.reactivex.rxjava3.core.Flowable
import javax.inject.Inject
import javax.inject.Singleton

interface ConfigRepository {

    fun getSupportedPages(): Flowable<List<SupportedPage>>

    fun saveSupportedPages(supportedPages: List<SupportedPage>)
}

@Singleton
class ConfigRepositoryImpl @Inject constructor(
    @LocalData private val localDataSource: ConfigRepository,
    @RemoteData private val remoteDataSource: ConfigRepository
) : ConfigRepository {

    @VisibleForTesting
    internal var cachedSupportedPages = listOf<SupportedPage>()

    override fun getSupportedPages(): Flowable<List<SupportedPage>> {

        return if (cachedSupportedPages.isNotEmpty()) {
            Flowable.just(cachedSupportedPages)
        } else {
            getAndCacheLocalSupportedPages()
                .flatMap { supportedPages ->
                    if (supportedPages.isEmpty()) {
                        getAndSaveRemoteSupportedPages()
                    } else {
                        Flowable.just(supportedPages)
                    }
                }
        }
    }

    override fun saveSupportedPages(supportedPages: List<SupportedPage>) {
        remoteDataSource.saveSupportedPages(supportedPages)
        localDataSource.saveSupportedPages(supportedPages)
        cachedSupportedPages = supportedPages
    }

    private fun getAndCacheLocalSupportedPages(): Flowable<List<SupportedPage>> {
        return localDataSource.getSupportedPages()
            .doOnNext { supportedPages ->
                cachedSupportedPages = supportedPages
            }
    }

    private fun getAndSaveRemoteSupportedPages(): Flowable<List<SupportedPage>> {
        return remoteDataSource.getSupportedPages()
            .doOnNext { supportedPages ->
                localDataSource.saveSupportedPages(supportedPages)
                cachedSupportedPages = supportedPages
            }
    }
}