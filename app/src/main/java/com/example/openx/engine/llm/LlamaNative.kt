package com.example.openx.engine.llm

import android.util.Log

interface TokenStreamCallback {
    fun onToken(token: String)
}

object LlamaNative {
    private const val TAG = "OpenX-LlamaNative"
    private const val LIB_NAME = "llama-android"

    var isNativeLoaded: Boolean = false
        private set

    var nativeLoadError: String? = null
        private set

    init {
        try {
            System.loadLibrary(LIB_NAME)
            isNativeLoaded = true
            safeLog(1, "Native library '$LIB_NAME' successfully loaded.")
        } catch (e: UnsatisfiedLinkError) {
            isNativeLoaded = false
            nativeLoadError = e.message ?: "Biblioteca nativa $LIB_NAME não compilada no APK atual."
            safeLog(2, "Native library '$LIB_NAME' not found or could not be loaded: ${e.message}")
        } catch (e: Throwable) {
            isNativeLoaded = false
            nativeLoadError = e.message
            safeLog(3, "Unexpected error loading native library: ${e.message}")
        }
    }

    private fun safeLog(level: Int, msg: String) {
        try {
            when (level) {
                1 -> Log.i(TAG, msg)
                2 -> Log.w(TAG, msg)
                else -> Log.e(TAG, msg)
            }
        } catch (_: Throwable) {
            // In unit tests running on desktop JVM without android.util.Log mock
            println("[$TAG] $msg")
        }
    }

    external fun nativeIsGpuSupported(): Boolean
    external fun nativeInitModel(modelPath: String, nThreads: Int, nContext: Int, useGpu: Boolean): Long
    external fun nativeFreeModel(contextPtr: Long)
    external fun nativeGenerate(
        contextPtr: Long,
        prompt: String,
        temperature: Float,
        topP: Float,
        maxTokens: Int,
        callback: TokenStreamCallback?
    ): String
    external fun nativeTokenize(contextPtr: Long, text: String): Int

    fun isReadyForInference(): Boolean = isNativeLoaded
}
