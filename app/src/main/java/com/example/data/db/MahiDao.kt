package com.example.data.db

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface MahiDao {

    @Query("SELECT * FROM mahi_memories WHERE userId = :userId ORDER BY timestamp DESC")
    fun getAllMemories(userId: String = "default_user"): Flow<List<MemoryEntity>>

    @Query("SELECT * FROM mahi_memories WHERE userId = :userId AND category = :category ORDER BY timestamp DESC")
    fun getMemoriesByCategory(category: String, userId: String = "default_user"): Flow<List<MemoryEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMemory(memory: MemoryEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMemories(memories: List<MemoryEntity>)

    @Update
    suspend fun updateMemory(memory: MemoryEntity)

    @Delete
    suspend fun deleteMemory(memory: MemoryEntity)

    @Query("DELETE FROM mahi_memories WHERE userId = :userId")
    suspend fun clearAllMemories(userId: String = "default_user")

    @Query("DELETE FROM mahi_memories WHERE id = :id")
    suspend fun deleteMemoryById(id: Long)

    // Conversation history
    @Query("SELECT * FROM conversation_history ORDER BY timestamp DESC LIMIT :limit")
    fun getRecentConversations(limit: Int = 20): Flow<List<ConversationEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertConversation(item: ConversationEntity): Long

    @Query("DELETE FROM conversation_history")
    suspend fun clearConversationHistory()
}
