package com.algo1127.mytask.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow
import java.time.DayOfWeek

@Dao
interface TimetableDao {
    @Query("SELECT * FROM timetable_slots ORDER BY startTime ASC")
    fun getAllSlotsFlow(): Flow<List<TimetableSlot>>

    @Query("SELECT * FROM timetable_slots ORDER BY startTime ASC")
    suspend fun getAllSlots(): List<TimetableSlot>

    @Query("SELECT * FROM timetable_slots WHERE dayOfWeek = :dayOfWeek ORDER BY startTime ASC")
    fun getSlotsForDayFlow(dayOfWeek: DayOfWeek): Flow<List<TimetableSlot>>

    @Query("SELECT * FROM timetable_slots WHERE dayOfWeek = :dayOfWeek ORDER BY startTime ASC")
    suspend fun getSlotsForDay(dayOfWeek: DayOfWeek): List<TimetableSlot>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSlot(slot: TimetableSlot)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSlots(slots: List<TimetableSlot>)

    @Delete
    suspend fun deleteSlot(slot: TimetableSlot)

    @Query("DELETE FROM timetable_slots WHERE id IN (:slotIds)")
    suspend fun deleteSlotsByIds(slotIds: List<Long>)

    @Query("DELETE FROM timetable_slots WHERE dayOfWeek = :dayOfWeek")
    suspend fun deleteSlotsForDay(dayOfWeek: DayOfWeek)

    @Query("DELETE FROM timetable_slots")
    suspend fun clearAll()
}
