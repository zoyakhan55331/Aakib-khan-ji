package com.example.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "mahi_memories")
data class MemoryEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val userId: String = "default_user",
    val category: String, // USER_PROFILE, PREFERENCES, CONVERSATION_CONTEXT, ASSISTANT_PREFERENCES, CUSTOM_COMMANDS
    val key: String,
    val value: String,
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "conversation_history")
data class ConversationEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val sender: String, // "user" or "mahi"
    val text: String,
    val actionTaken: String? = null,
    val timestamp: Long = System.currentTimeMillis()
)
