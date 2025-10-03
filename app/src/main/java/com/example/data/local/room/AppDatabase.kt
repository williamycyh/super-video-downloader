package com.example.data.local.room

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.example.data.local.room.dao.AdHostDao
import com.example.data.local.room.dao.ConfigDao
import com.example.data.local.room.dao.HistoryDao
import com.example.data.local.room.dao.PageDao
import com.example.data.local.room.dao.ProgressDao
import com.example.data.local.room.dao.VideoDao
import com.example.data.local.room.entity.AdHost
import com.example.data.local.room.entity.DownloadUrlsConverter
import com.example.data.local.room.entity.FormatsConverter
import com.example.data.local.room.entity.HistoryItem
import com.example.data.local.room.entity.PageInfo
import com.example.data.local.room.entity.ProgressInfo
import com.example.data.local.room.entity.SupportedPage
import com.example.data.local.room.entity.VideoInfo

const val DB_VERSION = 5

@Database(
    entities = [PageInfo::class, SupportedPage::class, VideoInfo::class, ProgressInfo::class, HistoryItem::class, AdHost::class],
    version = DB_VERSION,
)
@TypeConverters(FormatsConverter::class, DownloadUrlsConverter::class)
abstract class AppDatabase : RoomDatabase() {

    abstract fun configDao(): ConfigDao

    abstract fun videoDao(): VideoDao

    abstract fun progressDao(): ProgressDao

    abstract fun pageDao(): PageDao

    abstract fun historyDao(): HistoryDao

    abstract fun adHostDao(): AdHostDao
}