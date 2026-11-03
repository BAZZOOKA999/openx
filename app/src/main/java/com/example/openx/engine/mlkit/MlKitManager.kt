package com.example.openx.engine.mlkit

import com.google.android.gms.tasks.Task
import com.google.mlkit.common.model.DownloadConditions
import com.google.mlkit.common.model.RemoteModelManager
import com.google.mlkit.nl.translate.TranslateLanguage
import com.google.mlkit.nl.translate.TranslateRemoteModel
import com.google.mlkit.nl.translate.Translation
import com.google.mlkit.nl.translate.Translator
import com.google.mlkit.nl.translate.TranslatorOptions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import java.util.concurrent.ConcurrentHashMap
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

data class LanguageModelStatus(
    val languageCode: String,
    val displayName: String,
    val isDownloaded: Boolean,
    val isDownloading: Boolean = false,
    val errorMessage: String? = null
)

object MlKitManager {

    private val modelManager = RemoteModelManager.getInstance()
    private val translatorCache = ConcurrentHashMap<String, Translator>()

    private val _supportedLanguages = MutableStateFlow<List<LanguageModelStatus>>(emptyList())
    val supportedLanguages: StateFlow<List<LanguageModelStatus>> = _supportedLanguages.asStateFlow()

    private val KEY_LANGUAGES = listOf(
        Pair("pt", "Português (Brasil) - Padrão"),
        Pair("en", "Inglês (English)"),
        Pair("ja", "Japonês (日本語)"),
        Pair("es", "Espanhol (Español)"),
        Pair("fr", "Francês (Français)"),
        Pair("de", "Alemão (Deutsch)"),
        Pair("it", "Italiano"),
        Pair("ru", "Russo (Русский)"),
        Pair("ko", "Coreano (한국어)"),
        Pair("zh", "Chinês (中文)")
    )

    suspend fun refreshModelStatuses() = withContext(Dispatchers.IO) {
        try {
            val downloadedModels = modelManager.getDownloadedModels(TranslateRemoteModel::class.java).awaitTask()
            val downloadedCodes = downloadedModels.map { it.language }.toSet()

            val list = KEY_LANGUAGES.map { (code, name) ->
                LanguageModelStatus(
                    languageCode = code,
                    displayName = name,
                    isDownloaded = downloadedCodes.contains(code),
                    isDownloading = false
                )
            }
            _supportedLanguages.value = list
        } catch (e: Exception) {
            // Fallback status
            _supportedLanguages.value = KEY_LANGUAGES.map { (code, name) ->
                LanguageModelStatus(languageCode = code, displayName = name, isDownloaded = false)
            }
        }
    }

    suspend fun isModelDownloaded(languageCode: String): Boolean = withContext(Dispatchers.IO) {
        try {
            val model = TranslateRemoteModel.Builder(languageCode).build()
            modelManager.isModelDownloaded(model).awaitTask()
        } catch (_: Exception) {
            false
        }
    }

    suspend fun downloadLanguageModel(languageCode: String): Boolean = withContext(Dispatchers.IO) {
        updateDownloadingState(languageCode, true)
        try {
            val model = TranslateRemoteModel.Builder(languageCode).build()
            val conditions = DownloadConditions.Builder().build()
            modelManager.download(model, conditions).awaitTask()
            updateDownloadingState(languageCode, false, isDownloaded = true)
            true
        } catch (e: Exception) {
            updateDownloadingState(languageCode, false, errorMessage = e.message)
            false
        }
    }

    suspend fun deleteLanguageModel(languageCode: String): Boolean = withContext(Dispatchers.IO) {
        try {
            val model = TranslateRemoteModel.Builder(languageCode).build()
            modelManager.deleteDownloadedModel(model).awaitTask()
            
            // Invalidate cached translators using this language
            val keysToRemove = translatorCache.keys.filter { it.contains(languageCode) }
            for (k in keysToRemove) {
                translatorCache.remove(k)?.close()
            }
            
            updateDownloadingState(languageCode, false, isDownloaded = false)
            true
        } catch (_: Exception) {
            false
        }
    }

    suspend fun translateOnDevice(
        text: String,
        sourceLangCode: String = "en",
        targetLangCode: String = "pt"
    ): String = withContext(Dispatchers.IO) {
        if (text.isBlank()) return@withContext ""

        val normalizedSource = normalizeLangCode(sourceLangCode)
        val normalizedTarget = normalizeLangCode(targetLangCode)

        val pairKey = "$normalizedSource->$normalizedTarget"
        val translator = translatorCache.getOrPut(pairKey) {
            val options = TranslatorOptions.Builder()
                .setSourceLanguage(normalizedSource)
                .setTargetLanguage(normalizedTarget)
                .build()
            Translation.getClient(options)
        }

        // Ensure models are downloaded if needed
        val conditions = DownloadConditions.Builder().build()
        translator.downloadModelIfNeeded(conditions).awaitTask()

        // Execute fast zero-cost on-device translation
        translator.translate(text).awaitTask()
    }

    fun normalizeLangCode(code: String): String {
        val lower = code.lowercase().trim()
        return when {
            lower.startsWith("pt") -> TranslateLanguage.PORTUGUESE
            lower.startsWith("en") -> TranslateLanguage.ENGLISH
            lower.startsWith("ja") -> TranslateLanguage.JAPANESE
            lower.startsWith("es") -> TranslateLanguage.SPANISH
            lower.startsWith("fr") -> TranslateLanguage.FRENCH
            lower.startsWith("de") -> TranslateLanguage.GERMAN
            lower.startsWith("it") -> TranslateLanguage.ITALIAN
            lower.startsWith("ru") -> TranslateLanguage.RUSSIAN
            lower.startsWith("ko") -> TranslateLanguage.KOREAN
            lower.startsWith("zh") -> TranslateLanguage.CHINESE
            else -> TranslateLanguage.fromLanguageTag(lower) ?: TranslateLanguage.PORTUGUESE
        }
    }

    private fun updateDownloadingState(
        languageCode: String,
        isDownloading: Boolean,
        isDownloaded: Boolean? = null,
        errorMessage: String? = null
    ) {
        val current = _supportedLanguages.value.toMutableList()
        val index = current.indexOfFirst { it.languageCode == languageCode }
        if (index != -1) {
            val old = current[index]
            current[index] = old.copy(
                isDownloading = isDownloading,
                isDownloaded = isDownloaded ?: old.isDownloaded,
                errorMessage = errorMessage
            )
            _supportedLanguages.value = current
        }
    }

    suspend fun <T> Task<T>.awaitTask(): T = suspendCancellableCoroutine { cont ->
        addOnSuccessListener { result ->
            if (cont.isActive) cont.resume(result)
        }
        addOnFailureListener { exception ->
            if (cont.isActive) cont.resumeWithException(exception)
        }
        addOnCanceledListener {
            if (cont.isActive) cont.cancel()
        }
    }
}
