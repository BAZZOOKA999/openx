package com.example.openx.engine.llm

import android.app.ActivityManager
import android.content.Context
import android.os.Environment
import android.os.StatFs
import java.io.File
import java.util.Locale

data class DeviceMemoryStatus(
    val availMemBytes: Long,
    val totalMemBytes: Long,
    val isLowMemory: Boolean,
    val thresholdBytes: Long,
    val usedMemBytes: Long
)

data class DeviceStorageStatus(
    val availableBytes: Long,
    val totalBytes: Long,
    val usedBytes: Long
)

data class ModelCompatibilityReport(
    val isCompatible: Boolean,
    val hasEnoughRam: Boolean,
    val hasEnoughStorage: Boolean,
    val warningMessage: String? = null,
    val freeRamFormatted: String,
    val freeStorageFormatted: String,
    val requiredRamFormatted: String,
    val requiredStorageFormatted: String
)

object SystemResourceUtil {

    fun getMemoryInfo(context: Context): DeviceMemoryStatus {
        val am = context.getSystemService(Context.ACTIVITY_SERVICE) as? ActivityManager
        val memoryInfo = ActivityManager.MemoryInfo()
        if (am != null) {
            am.getMemoryInfo(memoryInfo)
            val used = memoryInfo.totalMem - memoryInfo.availMem
            return DeviceMemoryStatus(
                availMemBytes = memoryInfo.availMem,
                totalMemBytes = memoryInfo.totalMem,
                isLowMemory = memoryInfo.lowMemory,
                thresholdBytes = memoryInfo.threshold,
                usedMemBytes = used
            )
        }
        val runtimeMax = Runtime.getRuntime().maxMemory()
        val runtimeTotal = Runtime.getRuntime().totalMemory()
        val runtimeFree = Runtime.getRuntime().freeMemory()
        return DeviceMemoryStatus(
            availMemBytes = runtimeFree,
            totalMemBytes = runtimeMax,
            isLowMemory = false,
            thresholdBytes = 0L,
            usedMemBytes = runtimeTotal - runtimeFree
        )
    }

    fun getStorageInfo(context: Context): DeviceStorageStatus {
        val path: File = context.filesDir
        val stat = StatFs(path.path)
        val blockSize = stat.blockSizeLong
        val availableBlocks = stat.availableBlocksLong
        val totalBlocks = stat.blockCountLong

        val available = availableBlocks * blockSize
        val total = totalBlocks * blockSize
        val used = total - available

        return DeviceStorageStatus(
            availableBytes = available,
            totalBytes = total,
            usedBytes = used
        )
    }

    fun getCpuCores(): Int {
        return Runtime.getRuntime().availableProcessors().coerceAtLeast(1)
    }

    fun formatBytes(bytes: Long): String {
        if (bytes <= 0) return "0 B"
        val kb = bytes / 1024.0
        val mb = kb / 1024.0
        val gb = mb / 1024.0
        return when {
            gb >= 1.0 -> String.format(Locale.US, "%.1f GB", gb)
            mb >= 1.0 -> String.format(Locale.US, "%.0f MB", mb)
            kb >= 1.0 -> String.format(Locale.US, "%.0f KB", kb)
            else -> "$bytes B"
        }
    }

    fun checkModelCompatibility(
        context: Context,
        ramRequirementBytes: Long,
        sizeBytes: Long
    ): ModelCompatibilityReport {
        val mem = getMemoryInfo(context)
        val storage = getStorageInfo(context)

        // Allow some headroom for system and translation buffer
        val hasEnoughRam = mem.availMemBytes >= (ramRequirementBytes * 0.75)
        val hasEnoughStorage = storage.availableBytes >= (sizeBytes * 1.2)
        val isCompatible = hasEnoughRam && hasEnoughStorage

        val warnings = mutableListOf<String>()
        if (!hasEnoughRam) {
            warnings.add("RAM livre insuficiente (${formatBytes(mem.availMemBytes)} livre, recomendado ${formatBytes(ramRequirementBytes)}). O app pode sofrer encerramento por OOM.")
        }
        if (!hasEnoughStorage) {
            warnings.add("Armazenamento livre insuficiente (${formatBytes(storage.availableBytes)} livre, necessário ${formatBytes(sizeBytes)}).")
        }

        return ModelCompatibilityReport(
            isCompatible = isCompatible,
            hasEnoughRam = hasEnoughRam,
            hasEnoughStorage = hasEnoughStorage,
            warningMessage = if (warnings.isNotEmpty()) warnings.joinToString(" ") else null,
            freeRamFormatted = formatBytes(mem.availMemBytes),
            freeStorageFormatted = formatBytes(storage.availableBytes),
            requiredRamFormatted = formatBytes(ramRequirementBytes),
            requiredStorageFormatted = formatBytes(sizeBytes)
        )
    }
}
