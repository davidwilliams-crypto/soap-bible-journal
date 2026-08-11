package com.soapjournal.app.data.memory

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "memory_verses")
data class MemoryVerseEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val reference: String,
    val text: String,
    val source: String = "manual",
    val createdAtEpochMs: Long = System.currentTimeMillis(),
    val lastReviewedEpochMs: Long = 0L,
    val masteryLevel: Int = 0
)
