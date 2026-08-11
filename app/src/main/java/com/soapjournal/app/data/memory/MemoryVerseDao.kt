package com.soapjournal.app.data.memory

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface MemoryVerseDao {
    @Query("SELECT * FROM memory_verses ORDER BY createdAtEpochMs DESC")
    fun observeAll(): Flow<List<MemoryVerseEntity>>

    @Query("SELECT * FROM memory_verses ORDER BY id ASC")
    suspend fun getAll(): List<MemoryVerseEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(verse: MemoryVerseEntity): Long

    @Query("DELETE FROM memory_verses WHERE id = :id")
    suspend fun delete(id: Long)

    @Query("DELETE FROM memory_verses")
    suspend fun deleteAll()

    @Query(
        """
        UPDATE memory_verses
        SET masteryLevel = masteryLevel + 1,
            lastReviewedEpochMs = :reviewedAt
        WHERE id = :id
        """
    )
    suspend fun markReviewed(id: Long, reviewedAt: Long = System.currentTimeMillis())
}
