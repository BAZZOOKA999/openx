package com.example.openx.data.db.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "user_sessions")
data class UserSessionEntity(
    @PrimaryKey
    val userId: String = "local_guest",
    val displayName: String = "Tradutor OpenX",
    val email: String = "offline@openx.local",
    val isOfflineMode: Boolean = true,
    val pinCodeHash: String = "",
    val requirePinOnLaunch: Boolean = false,
    val preferredLanguage: String = "pt-BR",
    val autoBackupEnabled: Boolean = true,
    val selectedEngineId: String = "mlkit", // "mlkit", "firebase_ai", "groq", "openrouter", "offline_rule"
    val selectedModelName: String = "Google ML Kit On-Device",
    val geminiApiKey: String = "",
    val geminiModel: String = "gemini-1.5-flash",
    val groqApiKey: String = "",
    val groqModel: String = "llama-3.3-70b-versatile",
    val openRouterApiKey: String = "",
    val openRouterModel: String = "meta-llama/llama-3.3-70b-instruct:free",
    val allowOnlineFallback: Boolean = false,
    val tmAutoLookupEnabled: Boolean = true,
    val aiTranslationStyle: String = "Natural (Visual Novel)",
    val lastErrorLog: String = "",
    val onlineCallsConsumed: Int = 0,
    val lastActiveTimestamp: Long = System.currentTimeMillis()
)
