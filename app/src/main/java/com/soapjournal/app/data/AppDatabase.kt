package com.soapjournal.app.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.soapjournal.app.data.memory.MemoryVerseDao
import com.soapjournal.app.data.memory.MemoryVerseEntity

@Database(
    entities = [SoapEntryEntity::class, MemoryVerseEntity::class],
    version = 2,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun soapEntryDao(): SoapEntryDao
    abstract fun memoryVerseDao(): MemoryVerseDao

    companion object {
        @Volatile
        private var instance: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            return instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "soap_journal.db"
                )
                    .fallbackToDestructiveMigration()
                    .build()
                    .also { instance = it }
            }
        }
    }
}
