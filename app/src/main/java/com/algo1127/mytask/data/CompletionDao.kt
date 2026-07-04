package com.algo1127.mytask.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface CompletionDao {
    @Query("SELECT * FROM completion_records WHERE date = :date")
    fun getRecordsForDate(date: String): Flow<List<CompletionRecord>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdate(record: CompletionRecord)

    @Query("DELETE FROM completion_records WHERE itemId = :itemId")
    suspend fun deleteRecordsForItem(itemId: Long)

    @Query("SELECT * FROM completion_records WHERE itemId = :itemId AND date = :date")
    suspend fun getRecord(itemId: Long, date: String): CompletionRecord?
}
