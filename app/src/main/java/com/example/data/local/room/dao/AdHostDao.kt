package com.example.data.local.room.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.data.local.room.entity.AdHost

@Dao
interface AdHostDao {

    @Query("SELECT COUNT(*) FROM AdHost")
    suspend fun getHostsCount(): Int

    @Query("SELECT * FROM AdHost")
    suspend fun getAdHosts(): List<AdHost>

    @Query("SELECT EXISTS(SELECT * FROM AdHost WHERE host = :host)")
    fun isAdHost(host: String): Boolean

    @Insert(onConflict = OnConflictStrategy.NONE)
    suspend fun insertAdHost(adHost: AdHost)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAdHosts(adHosts: Set<AdHost>)

    @Delete
    suspend fun deleteAdHosts(adHosts: Set<AdHost>)

    @Query("DELETE FROM AdHost")
    suspend fun deleteAllAdHosts()

    @Delete
    suspend fun deleteAdHost(host: AdHost)
}
