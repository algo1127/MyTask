package com.algo1127.mytask.data

import androidx.room.*
import com.algo1127.mytask.ui.models.ReminderItem

@Dao
interface ReminderDao {
    @Query("SELECT * FROM reminders")
    suspend fun getAllReminders(): List<ReminderItem>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertReminder(reminder: ReminderItem)

    @Delete
    suspend fun deleteReminder(reminder: ReminderItem)

    @Query("DELETE FROM reminders WHERE id = :reminderId")
    suspend fun deleteReminderById(reminderId: Long)
}
