package com.example.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [MemoryEntity::class, ConversationEntity::class],
    version = 1,
    exportSchema = false
)
abstract class MahiDatabase : RoomDatabase() {
    abstract fun mahiDao(): MahiDao

    companion object {
        @Volatile
        private var INSTANCE: MahiDatabase? = null

        fun getInstance(context: Context): MahiDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    MahiDatabase::class.java,
                    "mahi_ai.db"
                ).fallbackToDestructiveMigration(dropAllTables = true).build()
                INSTANCE = instance
                instance
            }
        }
    }
}
