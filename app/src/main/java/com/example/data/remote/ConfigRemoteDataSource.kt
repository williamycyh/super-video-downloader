package com.example.data.remote

import com.example.data.local.room.entity.SupportedPage
import com.example.data.remote.service.ConfigService
import com.example.data.repository.ConfigRepository
import io.reactivex.rxjava3.core.Flowable
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ConfigRemoteDataSource @Inject constructor(
    private val configService: ConfigService
) : ConfigRepository {
    override fun getSupportedPages(): Flowable<List<SupportedPage>> {
        return configService.getSupportedPages()
    }

    override fun saveSupportedPages(supportedPages: List<SupportedPage>) {
    }
}