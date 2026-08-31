package com.algo1127.mytask.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.algo1127.mytask.ui.models.CountdownItem
import com.algo1127.mytask.ui.models.EventItem
import com.algo1127.mytask.ui.models.ReminderItem
import com.algo1127.mytask.ui.models.Task

@Database(
    entities = [CompletionRecord::class, Task::class, ReminderItem::class, EventItem::class, CountdownItem::class],
    version = 4,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class MyTaskDatabase : RoomDatabase() {
    abstract fun completionDao(): CompletionDao
    abstract fun taskDao(): TaskDao
    abstract fun reminderDao(): ReminderDao
    abstract fun eventDao(): EventDao
    abstract fun countdownDao(): CountdownDao

    companion object {
        @Volatile
        private var INSTANCE: MyTaskDatabase? = null

        fun getDatabase(context: Context): MyTaskDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    MyTaskDatabase::class.java,
                    "mytask_database"
                )
                .fallbackToDestructiveMigration() // For simplicity in this migration
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
