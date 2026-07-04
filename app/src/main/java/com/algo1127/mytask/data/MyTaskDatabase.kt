package com.algo1127.mytask.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(entities = [CompletionRecord::class], version = 1, exportSchema = false)
abstract class MyTaskDatabase : RoomDatabase() {
    abstract fun completionDao(): CompletionDao

    companion object {
        @Volatile
        private var INSTANCE: MyTaskDatabase? = null

        fun getDatabase(context: Context): MyTaskDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    MyTaskDatabase::class.java,
                    "mytask_database"
                ).build()
                INSTANCE = instance
                instance
            }
        }
    }
}
