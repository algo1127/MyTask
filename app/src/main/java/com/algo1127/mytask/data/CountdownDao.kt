package com.algo1127.mytask.data

import androidx.room.*
import com.algo1127.mytask.ui.models.CountdownItem
import kotlinx.coroutines.flow.Flow

@Dao
interface CountdownDao {
    @Query("SELECT * FROM countdowns ORDER BY targetDateTime ASC")
    fun getAllCountdowns(): Flow<List<CountdownItem>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCountdown(countdown: CountdownItem)

    @Update
    suspend fun updateCountdown(countdown: CountdownItem)

    @Delete
    suspend fun deleteCountdown(countdown: CountdownItem)

    @Query("SELECT * FROM countdowns WHERE id = :id")
    suspend fun getCountdownById(id: Long): CountdownItem?
}
