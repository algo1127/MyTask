package com.algo1127.mytask.data

import androidx.room.*
import com.algo1127.mytask.ui.models.EventItem

@Dao
interface EventDao {
    @Query("SELECT * FROM events")
    suspend fun getAllEvents(): List<EventItem>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEvent(event: EventItem)

    @Delete
    suspend fun deleteEvent(event: EventItem)

    @Query("DELETE FROM events WHERE id = :eventId")
    suspend fun deleteEventById(eventId: Long)

    @Query("SELECT * FROM events WHERE id = :eventId")
    suspend fun getEventById(eventId: Long): EventItem?
}
