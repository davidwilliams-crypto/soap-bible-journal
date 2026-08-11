package com.soapjournal.app.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "soap_entries")
data class SoapEntryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val createdAtEpochMs: Long,
    val updatedAtEpochMs: Long,
    val entryDateEpochDay: Long,
    val scriptureReference: String = "",
    val scriptureText: String = "",
    val tags: String = "",
    val isDraft: Boolean = true,
    val applicationFollowThrough: Boolean = false,
    val prayerFollowThrough: Boolean = false,
    val readingPlanDay: Int? = null
)
