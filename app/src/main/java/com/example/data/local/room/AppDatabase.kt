package com.example.data.local.room

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.example.data.local.room.dao.*
import com.example.data.local.room.entity.*

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