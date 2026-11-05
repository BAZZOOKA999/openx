package com.example.openx.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.Hardware
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.openx.data.db.entities.GgufModelEntity
import com.example.openx.engine.llm.DeviceMemoryStatus
import com.example.openx.engine.llm.DeviceStorageStatus
import com.example.openx.engine.llm.DownloadProgressUpdate
import com.example.openx.engine.llm.LlmInferenceParams
import com.example.openx.engine.llm.LocalInferenceStats
import com.example.openx.engine.llm.PromptPresetType
import com.example.openx.engine.llm.SystemResourceUtil
import com.example.openx.ui.theme.OpenXBackground
import com.example.openx.ui.theme.OpenXError
import com.example.openx.ui.theme.OpenXErrorContainer
import com.example.openx.ui.theme.OpenXInfo
import com.example.openx.ui.theme.OpenXInfoContainer
import com.example.openx.ui.theme.OpenXRed
import com.example.openx.ui.theme.OpenXRedContainer
import com.example.openx.ui.theme.OpenXRedLight
import com.example.openx.ui.theme.OpenXSuccess
import com.example.openx.ui.theme.OpenXSuccessContainer
import com.example.openx.ui.theme.OpenXSurface
import com.example.openx.ui.theme.OpenXSurfaceBorder
import com.example.openx.ui.theme.OpenXSurfaceVariant
import com.example.openx.ui.theme.OpenXTextPrimary
import com.example.openx.ui.theme.OpenXTextSecondary
import com.example.openx.ui.theme.OpenXWarning
import com.example.openx.ui.theme.OpenXWarningContainer

@Composable
fun ModelHubScreen(
    models: List<GgufModelEntity>,
    selectedModel: GgufModelEntity?,
    downloadUpdates: Map<Long, DownloadProgressUpdate>,
    localInferenceStats: LocalInferenceStats,
    inferenceParams: LlmInferenceParams,
    memoryStatus: DeviceMemoryStatus,
    storageStatus: DeviceStorageStatus,
    cpuCores: Int,
    onDownloadModel: (Long) -> Unit,
    onPauseDownload: (Long) -> Unit,
    onSelectModel: (Long) -> Unit,
    onDeleteModel: (Long) -> Unit,
    onVerifySha256: (Long) -> Unit,
    onImportModel: (Uri, String) -> Unit,
    onUpdateInferenceParams: (LlmInferenceParams) -> Unit,
    onRefreshResources: () -> Unit
) {
    val context = LocalContext.current
    var showSettingsDialog by remember { mutableStateOf(false) }

    val safLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri != null) {
            onImportModel(uri, "Modelo GGUF Importado")
        }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(OpenXBackground)
            .padding(horizontal = 16.dp),
        contentPadding = PaddingValues(vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Section 1: Header & Quick Actions
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Central de Modelos Locais (GGUF)",
                        style = MaterialTheme.typography.titleLarge,
                        color = OpenXTextPrimary,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Modelos compactos para execução 100% offline via llama.cpp",
                        style = MaterialTheme.typography.bodyMedium,
                        color = OpenXTextSecondary
                    )
                }
                IconButton(
                    onClick = onRefreshResources,
                    modifier = Modifier.testTag("refresh_resources_button")
                ) {
                    Icon(Icons.Default.Refresh, contentDescription = "Atualizar Diagnóstico", tint = OpenXTextSecondary)
                }
            }
        }

        // Section 2: Hardware Resources Card (RAM, Storage, CPU)
        item {
            HardwareResourceCard(
                memoryStatus = memoryStatus,
                storageStatus = storageStatus,
                cpuCores = cpuCores,
                localInferenceStats = localInferenceStats
            )
        }

        // Section 3: Diagnostic Status Banner (NDK / JNI / Local HTTP / Fallback)
        item {
            EngineDiagnosticBanner(
                localInferenceStats = localInferenceStats,
                selectedModel = selectedModel,
                inferenceParams = inferenceParams,
                onOpenSettings = { showSettingsDialog = true }
            )
        }

        // Section 4: Import Existing GGUF Action Bar
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = OpenXSurface),
                border = androidx.compose.foundation.BorderStroke(1.dp, OpenXSurfaceBorder),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Importar Modelo Existente",
                            style = MaterialTheme.typography.titleMedium,
                            color = OpenXTextPrimary,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Já possui um arquivo .gguf no aparelho? Importe via Storage Access Framework (SAF) sem baixar novamente.",
                            style = MaterialTheme.typography.bodySmall,
                            color = OpenXTextSecondary
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Button(
                        onClick = { safLauncher.launch(arrayOf("*/*")) },
                        colors = ButtonDefaults.buttonColors(containerColor = OpenXRed),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.testTag("import_gguf_saf_button")
                    ) {
                        Icon(Icons.Default.FolderOpen, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Importar .gguf", fontSize = 13.sp)
                    }
                }
            }
        }

        // Section 5: Model Catalog Title
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Catálogo de Modelos Recomendados",
                    style = MaterialTheme.typography.titleMedium,
                    color = OpenXTextPrimary,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "${models.count { it.isDownloaded }} instalados de ${models.size}",
                    style = MaterialTheme.typography.bodySmall,
                    color = OpenXTextSecondary
                )
            }
        }

        // Section 6: Model Cards
        items(models, key = { it.id }) { model ->
            val update = downloadUpdates[model.id]
            val compat = SystemResourceUtil.checkModelCompatibility(
                context = context,
                ramRequirementBytes = model.ramRequirementBytes,
                sizeBytes = model.sizeBytes
            )

            GgufModelCard(
                model = model,
                isSelected = model.id == selectedModel?.id,
                update = update,
                compatReport = compat,
                onDownload = { onDownloadModel(model.id) },
                onPause = { onPauseDownload(model.id) },
                onSelect = { onSelectModel(model.id) },
                onDelete = { onDeleteModel(model.id) },
                onVerifySha = { onVerifySha256(model.id) }
            )
        }
    }

    if (showSettingsDialog) {
        LlmInferenceSettingsDialog(
            currentParams = inferenceParams,
            availableCores = cpuCores,
            models = models,
            onDismiss = { showSettingsDialog = false },
            onSave = { updated ->
                onUpdateInferenceParams(updated)
                showSettingsDialog = false
            }
        )
    }
}

@Composable
fun HardwareResourceCard(
    memoryStatus: DeviceMemoryStatus,
    storageStatus: DeviceStorageStatus,
    cpuCores: Int,
    localInferenceStats: LocalInferenceStats
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = OpenXSurface),
        border = androidx.compose.foundation.BorderStroke(1.dp, OpenXSurfaceBorder),
        shape = RoundedCornerShape(14.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Memory, contentDescription = null, tint = OpenXRed, modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Recursos de Hardware do Dispositivo",
                        style = MaterialTheme.typography.titleMedium,
                        color = OpenXTextPrimary,
                        fontWeight = FontWeight.Bold
                    )
                }
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(if (memoryStatus.isLowMemory) OpenXErrorContainer else OpenXSuccessContainer)
                        .padding(horizontal = 8.dp, vertical = 3.dp)
                ) {
                    Text(
                        text = if (memoryStatus.isLowMemory) "Memória Baixa" else "RAM Saudável",
                        color = if (memoryStatus.isLowMemory) OpenXError else OpenXSuccess,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // RAM bar
            val ramUsage = if (memoryStatus.totalMemBytes > 0) {
                memoryStatus.usedMemBytes.toFloat() / memoryStatus.totalMemBytes.toFloat()
            } else 0f
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(
                    text = "RAM Livre: ${SystemResourceUtil.formatBytes(memoryStatus.availMemBytes)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = OpenXTextPrimary,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = "Total: ${SystemResourceUtil.formatBytes(memoryStatus.totalMemBytes)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = OpenXTextSecondary
                )
            }
            Spacer(modifier = Modifier.height(4.dp))
            LinearProgressIndicator(
                progress = { ramUsage.coerceIn(0f, 1f) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(RoundedCornerShape(3.dp)),
                color = if (ramUsage > 0.85f) OpenXError else OpenXRed,
                trackColor = OpenXSurfaceVariant
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Storage bar
            val storageUsage = if (storageStatus.totalBytes > 0) {
                storageStatus.usedBytes.toFloat() / storageStatus.totalBytes.toFloat()
            } else 0f
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(
                    text = "Armazenamento Livre: ${SystemResourceUtil.formatBytes(storageStatus.availableBytes)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = OpenXTextPrimary,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = "Total: ${SystemResourceUtil.formatBytes(storageStatus.totalBytes)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = OpenXTextSecondary
                )
            }
            Spacer(modifier = Modifier.height(4.dp))
            LinearProgressIndicator(
                progress = { storageUsage.coerceIn(0f, 1f) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(RoundedCornerShape(3.dp)),
                color = OpenXRedLight,
                trackColor = OpenXSurfaceVariant
            )

            Spacer(modifier = Modifier.height(12.dp))

            // CPU & Speed
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Núcleos de CPU: $cpuCores cores",
                    style = MaterialTheme.typography.bodySmall,
                    color = OpenXTextSecondary
                )
                Text(
                    text = "Velocidade: %.1f tokens/s".format(localInferenceStats.lastTokensPerSecond),
                    style = MaterialTheme.typography.bodySmall,
                    color = OpenXSuccess,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
fun EngineDiagnosticBanner(
    localInferenceStats: LocalInferenceStats,
    selectedModel: GgufModelEntity?,
    inferenceParams: LlmInferenceParams,
    onOpenSettings: () -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = if (localInferenceStats.nativeLibLoaded) OpenXSuccessContainer else OpenXInfoContainer
        ),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        if (localInferenceStats.nativeLibLoaded) Icons.Default.CheckCircle else Icons.Default.Info,
                        contentDescription = null,
                        tint = if (localInferenceStats.nativeLibLoaded) OpenXSuccess else OpenXInfo,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (localInferenceStats.nativeLibLoaded) "JNI llama.cpp Nativo Conectado" else "Arquitetura Pronta & Fallback Ativo",
                        color = OpenXTextPrimary,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                }
                IconButton(onClick = onOpenSettings) {
                    Icon(Icons.Default.Settings, contentDescription = "Configurar", tint = OpenXTextPrimary)
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = if (localInferenceStats.nativeLibLoaded) {
                    "Motor nativo pronto. Modelo ativo: ${selectedModel?.name ?: "Nenhum selecionado"}. Execução em threads CPU locais."
                } else {
                    "Fontes C++ e CMake do llama.cpp incluídos no projeto. O OpenX suporta conexão direta ao servidor local (Ollama/llama-server em ${inferenceParams.localServerUrl}) e fallback neural on-device Google ML Kit (100% offline)."
                },
                style = MaterialTheme.typography.bodySmall,
                color = OpenXTextSecondary
            )
        }
    }
}

@Composable
fun GgufModelCard(
    model: GgufModelEntity,
    isSelected: Boolean,
    update: DownloadProgressUpdate?,
    compatReport: com.example.openx.engine.llm.ModelCompatibilityReport,
    onDownload: () -> Unit,
    onPause: () -> Unit,
    onSelect: () -> Unit,
    onDelete: () -> Unit,
    onVerifySha: () -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) OpenXRedContainer.copy(alpha = 0.25f) else OpenXSurface
        ),
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            if (isSelected) OpenXRed else OpenXSurfaceBorder
        ),
        shape = RoundedCornerShape(14.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Header: Name & Status
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = model.name,
                        style = MaterialTheme.typography.titleMedium,
                        color = OpenXTextPrimary,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "${model.architecture.uppercase()} • Quantização: ${model.quantization} • Licença: ${model.license}",
                        style = MaterialTheme.typography.bodySmall,
                        color = OpenXTextSecondary
                    )
                }

                if (isSelected) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(OpenXRed)
                            .padding(horizontal = 8.dp, vertical = 3.dp)
                    ) {
                        Text(
                            text = "ATIVO",
                            color = Color.White,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Specs Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Column {
                    Text(text = "Tamanho", style = MaterialTheme.typography.labelSmall, color = OpenXTextSecondary)
                    Text(
                        text = SystemResourceUtil.formatBytes(model.sizeBytes),
                        style = MaterialTheme.typography.bodySmall,
                        color = OpenXTextPrimary,
                        fontWeight = FontWeight.SemiBold
                    )
                }
                Column {
                    Text(text = "RAM Estimada", style = MaterialTheme.typography.labelSmall, color = OpenXTextSecondary)
                    Text(
                        text = SystemResourceUtil.formatBytes(model.ramRequirementBytes),
                        style = MaterialTheme.typography.bodySmall,
                        color = OpenXTextPrimary,
                        fontWeight = FontWeight.SemiBold
                    )
                }
                Column {
                    Text(text = "Compatibilidade", style = MaterialTheme.typography.labelSmall, color = OpenXTextSecondary)
                    Text(
                        text = if (compatReport.isCompatible) "Adequado" else "Pesado",
                        style = MaterialTheme.typography.bodySmall,
                        color = if (compatReport.isCompatible) OpenXSuccess else OpenXWarning,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            // Warning note if not enough RAM
            if (compatReport.warningMessage != null) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = compatReport.warningMessage,
                    style = MaterialTheme.typography.bodySmall,
                    color = OpenXWarning,
                    fontSize = 11.sp
                )
            }

            // Download Progress Bar if downloading
            if (model.downloadStatus == "DOWNLOADING" || update?.status == "DOWNLOADING") {
                val prog = update?.progress ?: model.downloadProgress
                val speed = update?.speedBytesPerSec ?: 0L
                Spacer(modifier = Modifier.height(12.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Baixando: ${(prog * 100).toInt()}%",
                        style = MaterialTheme.typography.bodySmall,
                        color = OpenXTextPrimary
                    )
                    Text(
                        text = "${SystemResourceUtil.formatBytes(speed)}/s",
                        style = MaterialTheme.typography.bodySmall,
                        color = OpenXTextSecondary
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                LinearProgressIndicator(
                    progress = { prog },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(6.dp)
                        .clip(RoundedCornerShape(3.dp)),
                    color = OpenXRed,
                    trackColor = OpenXSurfaceVariant
                )
            }

            // SHA-256 Info
            if (model.sha256Calculated != null) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "SHA-256: ${model.sha256Calculated.take(12)}...${model.sha256Calculated.takeLast(8)} (Verificado)",
                    fontFamily = FontFamily.Monospace,
                    fontSize = 11.sp,
                    color = OpenXSuccess
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Action Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (model.isDownloaded) {
                    if (!isSelected) {
                        Button(
                            onClick = onSelect,
                            colors = ButtonDefaults.buttonColors(containerColor = OpenXRed),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text("Ativar Modelo", fontSize = 13.sp)
                        }
                    } else {
                        OutlinedButton(
                            onClick = onVerifySha,
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Icon(Icons.Default.VerifiedUser, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Verificar SHA-256", fontSize = 12.sp)
                        }
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    IconButton(onClick = onDelete) {
                        Icon(Icons.Default.Delete, contentDescription = "Excluir Modelo", tint = OpenXError)
                    }
                } else {
                    if (model.downloadStatus == "DOWNLOADING") {
                        OutlinedButton(
                            onClick = onPause,
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Icon(Icons.Default.Pause, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Pausar", fontSize = 13.sp)
                        }
                    } else {
                        Button(
                            onClick = onDownload,
                            colors = ButtonDefaults.buttonColors(containerColor = OpenXRed),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Baixar Modelo", fontSize = 13.sp)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun LlmInferenceSettingsDialog(
    currentParams: LlmInferenceParams,
    availableCores: Int,
    models: List<GgufModelEntity> = emptyList(),
    onDismiss: () -> Unit,
    onSave: (LlmInferenceParams) -> Unit
) {
    var selectedPreset by remember { mutableStateOf(currentParams.preset) }
    var customInstruction by remember { mutableStateOf(currentParams.customInstruction) }
    var temperature by remember { mutableFloatStateOf(currentParams.temperature) }
    var topP by remember { mutableFloatStateOf(currentParams.topP) }
    var maxTokens by remember { mutableIntStateOf(currentParams.maxTokens) }
    var nThreads by remember { mutableIntStateOf(currentParams.nThreads) }
    var serverUrl by remember { mutableStateOf(currentParams.localServerUrl) }
    var enableSequentialReview by remember { mutableStateOf(currentParams.enableSequentialReview) }
    var selectedReviewModelId by remember { mutableStateOf(currentParams.reviewModelId) }
    var reviewTemperature by remember { mutableFloatStateOf(currentParams.reviewTemperature) }

    val downloadedModels = remember(models) { models.filter { it.isDownloaded } }

    androidx.compose.material3.AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text("Configurações do Motor Local e Prompts", color = OpenXTextPrimary, fontWeight = FontWeight.Bold)
        },
        text = {
            LazyColumn(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                item {
                    Text(
                        text = "Preset de Tradução Visual Novel",
                        style = MaterialTheme.typography.titleSmall,
                        color = OpenXTextPrimary,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        PromptPresetType.values().forEach { preset ->
                            val isChosen = preset == selectedPreset
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (isChosen) OpenXRedContainer else OpenXSurfaceVariant)
                                    .clickable {
                                        selectedPreset = preset
                                        customInstruction = preset.defaultInstruction
                                    }
                                    .padding(10.dp)
                            ) {
                                Column {
                                    Text(
                                        text = preset.title,
                                        color = if (isChosen) OpenXRed else OpenXTextPrimary,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp
                                    )
                                    Text(
                                        text = preset.description,
                                        color = OpenXTextSecondary,
                                        fontSize = 11.sp
                                    )
                                }
                            }
                        }
                    }
                }

                item {
                    Text(
                        text = "Instrução de Sistema Customizada",
                        style = MaterialTheme.typography.titleSmall,
                        color = OpenXTextPrimary,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    OutlinedTextField(
                        value = customInstruction,
                        onValueChange = { customInstruction = it },
                        modifier = Modifier.fillMaxWidth(),
                        maxLines = 4,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = OpenXRed,
                            unfocusedBorderColor = OpenXSurfaceBorder,
                            focusedTextColor = OpenXTextPrimary,
                            unfocusedTextColor = OpenXTextPrimary
                        )
                    )
                }

                item {
                    Text(
                        text = "Temperatura: ${"%.2f".format(temperature)} (Criatividade)",
                        style = MaterialTheme.typography.titleSmall,
                        color = OpenXTextPrimary
                    )
                    Slider(
                        value = temperature,
                        onValueChange = { temperature = it },
                        valueRange = 0.0f..1.0f,
                        colors = SliderDefaults.colors(thumbColor = OpenXRed, activeTrackColor = OpenXRed)
                    )
                }

                item {
                    Text(
                        text = "Threads de CPU: $nThreads (de $availableCores disponíveis)",
                        style = MaterialTheme.typography.titleSmall,
                        color = OpenXTextPrimary
                    )
                    Slider(
                        value = nThreads.toFloat(),
                        onValueChange = { nThreads = it.toInt() },
                        valueRange = 1f..availableCores.coerceAtLeast(1).toFloat(),
                        steps = (availableCores - 2).coerceAtLeast(0),
                        colors = SliderDefaults.colors(thumbColor = OpenXRed, activeTrackColor = OpenXRed)
                    )
                }

                // Dual Model: Sequential Review Pipeline
                item {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = OpenXSurfaceVariant),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "Revisão Sequencial (2º Estágio)",
                                        style = MaterialTheme.typography.titleSmall,
                                        color = OpenXTextPrimary,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = "Aplica um modelo de polimento gramatical e expressividade após a tradução primária.",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = OpenXTextSecondary,
                                        fontSize = 11.sp
                                    )
                                }
                                androidx.compose.material3.Switch(
                                    checked = enableSequentialReview,
                                    onCheckedChange = { enableSequentialReview = it },
                                    colors = androidx.compose.material3.SwitchDefaults.colors(
                                        checkedThumbColor = Color.White,
                                        checkedTrackColor = OpenXRed
                                    )
                                )
                            }

                            if (enableSequentialReview) {
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = "Modelo para Revisão:",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = OpenXTextPrimary,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                if (downloadedModels.isEmpty()) {
                                    Text(
                                        text = "Nenhum modelo baixado adicional. O mesmo modelo ativo será reutilizado com prompt de revisão.",
                                        color = OpenXWarning,
                                        fontSize = 11.sp
                                    )
                                } else {
                                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                        downloadedModels.forEach { m ->
                                            val isChosen = m.id == selectedReviewModelId
                                            Row(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .clip(RoundedCornerShape(6.dp))
                                                    .background(if (isChosen) OpenXRedContainer else OpenXSurface)
                                                    .clickable { selectedReviewModelId = m.id }
                                                    .padding(8.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Text(
                                                    text = m.name,
                                                    color = if (isChosen) OpenXRed else OpenXTextPrimary,
                                                    fontSize = 12.sp,
                                                    fontWeight = if (isChosen) FontWeight.Bold else FontWeight.Normal
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                item {
                    Text(
                        text = "Servidor Local Alternativo (llama-server / Termux)",
                        style = MaterialTheme.typography.titleSmall,
                        color = OpenXTextPrimary
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    OutlinedTextField(
                        value = serverUrl,
                        onValueChange = { serverUrl = it },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = OpenXRed,
                            unfocusedBorderColor = OpenXSurfaceBorder,
                            focusedTextColor = OpenXTextPrimary,
                            unfocusedTextColor = OpenXTextPrimary
                        )
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onSave(
                        currentParams.copy(
                            preset = selectedPreset,
                            customInstruction = customInstruction,
                            temperature = temperature,
                            topP = topP,
                            maxTokens = maxTokens,
                            nThreads = nThreads,
                            localServerUrl = serverUrl,
                            enableSequentialReview = enableSequentialReview,
                            reviewModelId = selectedReviewModelId,
                            reviewTemperature = reviewTemperature
                        )
                    )
                },
                colors = ButtonDefaults.buttonColors(containerColor = OpenXRed)
            ) {
                Text("Salvar")
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss) {
                Text("Cancelar")
            }
        },
        containerColor = OpenXSurface
    )
}
