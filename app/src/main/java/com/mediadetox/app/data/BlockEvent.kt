package com.mediadetox.app.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "block_events")
data class BlockEvent(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val timestamp: Long,
    val eventType: String,   // "BLOCKED", "CAVED", or "DM_BYPASS"
    val appPackage: String
)
