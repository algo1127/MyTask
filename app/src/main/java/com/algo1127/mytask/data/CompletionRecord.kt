package com.algo1127.mytask.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "completion_records")
data class CompletionRecord(
    @PrimaryKey val id: String, // format: "item_id:date"
    val itemId: Long,
    val date: String, // ISO format: yyyy-MM-dd
    val isDone: Boolean
)
