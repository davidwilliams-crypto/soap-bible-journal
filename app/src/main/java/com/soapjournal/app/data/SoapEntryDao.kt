package com.soapjournal.app.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface SoapEntryDao {
    @Query("SELECT * FROM soap_entries ORDER BY entryDateEpochDay DESC, updatedAtEpochMs DESC")
    fun observeAll(): Flow<List<SoapEntryEntity>>

    @Query("SELECT * FROM soap_entries ORDER BY id ASC")
    suspend fun getAll(): List<SoapEntryEntity>

    @Query("SELECT * FROM soap_entries WHERE id = :id LIMIT 1")
    suspend fun getById(id: Long): SoapEntryEntity?

    @Query("SELECT * FROM soap_entries WHERE entryDateEpochDay = :epochDay ORDER BY updatedAtEpochMs DESC LIMIT 1")
    suspend fun getForDate(epochDay: Long): SoapEntryEntity?

    @Query(
        """
        SELECT * FROM soap_entries
        WHERE scriptureReference LIKE '%' || :query || '%'
           OR tags LIKE '%' || :query || '%'
           OR scriptureText LIKE '%' || :query || '%'
        ORDER BY entryDateEpochDay DESC
        """
    )
    fun search(query: String): Flow<List<SoapEntryEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entry: SoapEntryEntity): Long

    @Update
    suspend fun update(entry: SoapEntryEntity)

    @Query("DELETE FROM soap_entries WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("DELETE FROM soap_entries")
    suspend fun deleteAll()

    @Query(
        """
        SELECT COUNT(*) FROM soap_entries
        WHERE isDraft = 0 AND entryDateEpochDay >= :fromDay
        """
    )
    suspend fun completedCountSince(fromDay: Long): Int
}
