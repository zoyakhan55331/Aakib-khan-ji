package com.example.data

import android.content.Context
import android.content.SharedPreferences
import com.example.data.db.ConversationEntity
import com.example.data.db.MahiDao
import com.example.data.db.MahiDatabase
import com.example.data.db.MemoryEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first

class MemoryRepository(context: Context) {
    private val dao: MahiDao = MahiDatabase.getInstance(context).mahiDao()
    private val prefs: SharedPreferences = context.getSharedPreferences("mahi_settings", Context.MODE_PRIVATE)

    var isMemoryEnabled: Boolean
        get() = prefs.getBoolean("memory_enabled", true)
        set(value) = prefs.edit().putBoolean("memory_enabled", value).apply()

    var sassLevel: Float // 0.0 = gentle, 0.5 = witty & sassy, 1.0 = maximum savage
        get() = prefs.getFloat("sass_level", 0.7f)
        set(value) = prefs.edit().putFloat("sass_level", value).apply()

    var speechRate: Float
        get() = prefs.getFloat("speech_rate", 1.05f)
        set(value) = prefs.edit().putFloat("speech_rate", value).apply()

    var speechPitch: Float
        get() = prefs.getFloat("speech_pitch", 1.15f)
        set(value) = prefs.edit().putFloat("speech_pitch", value).apply()

    var backgroundVoiceEnabled: Boolean
        get() = prefs.getBoolean("bg_voice_enabled", false)
        set(value) = prefs.edit().putBoolean("bg_voice_enabled", value).apply()

    var languagePreference: String // "auto", "hinglish", "english", "hindi"
        get() = prefs.getString("language_pref", "auto") ?: "auto"
        set(value) = prefs.edit().putString("language_pref", value).apply()

    var selectedTheme: String // "cute_neon", "original", "minimal_dark"
        get() = prefs.getString("selected_theme", "cute_neon") ?: "cute_neon"
        set(value) = prefs.edit().putString("selected_theme", value).apply()

    fun getAllMemories(userId: String = "default_user"): Flow<List<MemoryEntity>> {
        return dao.getAllMemories(userId)
    }

    suspend fun saveMemory(category: String, key: String, value: String, userId: String = "default_user") {
        if (!isMemoryEnabled) return
        dao.insertMemory(
            MemoryEntity(
                userId = userId,
                category = category,
                key = key,
                value = value
            )
        )
    }

    suspend fun deleteMemory(memory: MemoryEntity) {
        dao.deleteMemory(memory)
    }

    suspend fun deleteMemoryById(id: Long) {
        dao.deleteMemoryById(id)
    }

    suspend fun clearAllMemories(userId: String = "default_user") {
        dao.clearAllMemories(userId)
    }

    fun getRecentConversations(limit: Int = 20): Flow<List<ConversationEntity>> {
        return dao.getRecentConversations(limit)
    }

    suspend fun recordConversation(sender: String, text: String, actionTaken: String? = null) {
        dao.insertConversation(
            ConversationEntity(
                sender = sender,
                text = text,
                actionTaken = actionTaken
            )
        )
    }

    suspend fun clearConversationHistory() {
        dao.clearConversationHistory()
    }

    suspend fun getFormattedMemoryContext(userId: String = "default_user"): String {
        if (!isMemoryEnabled) return "Memory is currently disabled by user."
        val memories = dao.getAllMemories(userId).first()
        if (memories.isEmpty()) {
            return "No previous memories saved."
        }
        return memories.joinToString("\n") { "- [${it.category}] ${it.key}: ${it.value}" }
    }
}
