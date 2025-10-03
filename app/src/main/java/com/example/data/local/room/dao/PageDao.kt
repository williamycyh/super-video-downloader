package com.example.data.local.room.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.data.local.room.entity.PageInfo
import io.reactivex.rxjava3.core.Observable

@Dao
interface PageDao {

    @Query("SELECT * FROM PageInfo ORDER BY `order` ASC")
    fun getPageInfos(): Observable<List<PageInfo>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertProgressInfo(progressInfo: PageInfo)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertAllProgressInfo(progressInfos: List<PageInfo>)

    @Delete
    fun deleteProgressInfo(progressInfo: PageInfo)

    @Query("DELETE FROM PageInfo")
    fun deleteAll()
}