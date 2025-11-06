package com.example.data.local

import com.example.data.local.room.dao.ConfigDao
import com.example.data.local.room.entity.SupportedPage
import com.example.data.repository.ConfigRepository
import io.reactivex.rxjava3.core.Flowable
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ConfigLocalDataSource @Inject constructor(
    private val configDao: ConfigDao
) : ConfigRepository {

    override fun getSupportedPages(): Flowable<List<SupportedPage>> {
        return configDao.getSupportedPages().toFlowable()
    }

    override fun saveSupportedPages(supportedPages: List<SupportedPage>) {
        supportedPages.map { configDao.insertSupportedPage(it) }
    }
}