package com.example.openx.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.NetworkCheck
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.filled.WifiOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.openx.data.db.entities.UserSessionEntity
import com.example.openx.engine.mlkit.LanguageModelStatus
import com.example.openx.engine.translation.EngineConnectionTestResult
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

data class EngineOption(
    val id: String,
    val name: String,
    val description: String,
    val isZeroCost: Boolean,
    val isOffline: Boolean,
    val isExperimental: Boolean,
    val rateLimitText: String
)

@Composable
fun AiCenterScreen(
    userSession: UserSessionEntity?,
    mlKitStatuses: List<LanguageModelStatus>,
    tmCount: Int,
    isTestingConnection: Boolean,
    connectionTestResult: EngineConnectionTestResult?,
    onSelectEngine: (String, String) -> Unit,
    onTestConnection: (String, String, String) -> Unit,
    onDownloadMlKitModel: (String) -> Unit,
    onDeleteMlKitModel: (String) -> Unit,
    onSaveApiKeys: (gemini: String, groq: String, openRouter: String) -> Unit,
    onClearTm: () -> Unit,
    onNavigateToModelHub: () -> Unit = {}
) {
    val engines = remember {
        listOf(
            EngineOption(
                id = "local_gguf",
                name = "IA Local GGUF (llama.cpp Offline)",
                description = "Modelos compactos Qwen2.5/Qwen3 executados diretamente na CPU/GPU do seu aparelho sem internet.",
                isZeroCost = true,
                isOffline = true,
                isExperimental = false,
                rateLimitText = "Ilimitado • Zero Custo • 100% Offline"
            ),
            EngineOption(
                id = "mlkit",
                name = "Google ML Kit (No Dispositivo)",
                description = "Motor de tradução neural oficial do Google executado 100% no hardware local.",
                isZeroCost = true,
                isOffline = true,
                isExperimental = false,
                rateLimitText = "Ilimitado • Zero Custo • Sem conexão"
            ),
            EngineOption(
                id = "firebase_ai",
                name = "Google Gemini (Firebase AI Logic)",
                description = "IA generativa multimodal com preservação de tom e nuances para visual novels.",
                isZeroCost = false,
                isOffline = false,
                isExperimental = false,
                rateLimitText = "Free Tier: ~15 RPM • 1M tokens/min"
            ),
            EngineOption(
                id = "groq",
                name = "Groq LPU (Experimental)",
                description = "Inferência ultra-rápida de modelos abertos (Llama 3.3 70B e Mixtral).",
                isZeroCost = false,
                isOffline = false,
                isExperimental = true,
                rateLimitText = "Free Tier: 30 RPM • Chave pessoal necessária"
            ),
            EngineOption(
                id = "openrouter",
                name = "OpenRouter (Experimental)",
                description = "Hub multi-provedores com modelos livres (Llama 3.3, Mistral, Gemini Exp).",
                isZeroCost = false,
                isOffline = false,
                isExperimental = true,
                rateLimitText = "Modelos gratuitos com limite de taxa da comunidade"
            ),
            EngineOption(
                id = "offline_rule",
                name = "Regras & Dicionário Visual Novel",
                description = "Substituição determinística de vocabulário de jogos, menus e termos protegidos.",
                isZeroCost = true,
                isOffline = true,
                isExperimental = false,
                rateLimitText = "Ilimitado • Instantâneo • 100% Offline"
            )
        )
    }

    val selectedEngineId = userSession?.selectedEngineId ?: "mlkit"

    var geminiKey by remember(userSession?.geminiApiKey) { mutableStateOf(userSession?.geminiApiKey ?: "") }
    var groqKey by remember(userSession?.groqApiKey) { mutableStateOf(userSession?.groqApiKey ?: "") }
    var openRouterKey by remember(userSession?.openRouterApiKey) { mutableStateOf(userSession?.openRouterApiKey ?: "") }

    var showGeminiKey by remember { mutableStateOf(false) }
    var showGroqKey by remember { mutableStateOf(false) }
    var showOpenRouterKey by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(OpenXBackground)
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Spacer(modifier = Modifier.height(4.dp))
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .border(1.dp, OpenXRed.copy(alpha = 0.5f), RoundedCornerShape(16.dp)),
                colors = CardDefaults.cardColors(containerColor = OpenXSurface)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(OpenXRed),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.AutoAwesome, null, tint = Color.White)
                        }
                        Column {
                            Text(
                                text = "Central de Inteligência Artificial",
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Black,
                                    color = OpenXTextPrimary
                                )
                            )
                            Text(
                                text = "Arquitetura Híbrida: Custo Zero On-Device + Nuvem Opcional",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    color = OpenXTextSecondary
                                )
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = "Prioridade absoluta para o Google ML Kit no dispositivo. Nenhuma credencial de desenvolvedor é embutida no aplicativo. Provedores externos requerem chave pessoal configurada pelo usuário e nunca realizam cobranças automáticas.",
                        color = OpenXTextSecondary,
                        fontSize = 12.sp,
                        lineHeight = 17.sp
                    )
                }
            }
        }

        // Shortcut to Central de Modelos GGUF (llama.cpp)
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .border(1.dp, OpenXRed, RoundedCornerShape(14.dp)),
                colors = CardDefaults.cardColors(containerColor = OpenXRedContainer.copy(alpha = 0.25f))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Central de Modelos Locais (GGUF)",
                            style = MaterialTheme.typography.titleSmall,
                            color = OpenXTextPrimary,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Baixe modelos Qwen2.5/Qwen3, importe via SAF e monitore RAM/CPU.",
                            style = MaterialTheme.typography.bodySmall,
                            color = OpenXTextSecondary
                        )
                    }
                    Button(
                        onClick = onNavigateToModelHub,
                        colors = ButtonDefaults.buttonColors(containerColor = OpenXRed),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.testTag("open_model_hub_button")
                    ) {
                        Text("Gerenciar GGUF", fontSize = 12.sp)
                    }
                }
            }
        }

        // Engine Selection Cards
        item {
            Text("Selecione o Motor de Tradução:", fontWeight = FontWeight.Bold, color = OpenXTextPrimary, fontSize = 14.sp)
        }

        items(engines) { engine ->
            val isSelected = engine.id == selectedEngineId
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .border(
                        1.dp,
                        if (isSelected) OpenXRed else OpenXSurfaceBorder,
                        RoundedCornerShape(12.dp)
                    )
                    .clickable { onSelectEngine(engine.id, engine.name) }
                    .testTag("engine_card_${engine.id}"),
                colors = CardDefaults.cardColors(
                    containerColor = if (isSelected) OpenXSurfaceVariant else OpenXSurface
                )
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(
                                text = engine.name,
                                fontWeight = FontWeight.Bold,
                                color = if (isSelected) OpenXRedLight else OpenXTextPrimary,
                                fontSize = 14.sp
                            )
                            if (isSelected) {
                                Icon(Icons.Default.CheckCircle, null, tint = OpenXRedLight, modifier = Modifier.size(16.dp))
                            }
                        }

                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            if (engine.isZeroCost) {
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(4.dp))
                                        .background(OpenXSuccessContainer)
                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                ) {
                                    Text("Custo Zero", color = OpenXSuccess, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                            if (engine.isOffline) {
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(4.dp))
                                        .background(OpenXInfoContainer)
                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                ) {
                                    Text("Offline", color = OpenXInfo, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                            if (engine.isExperimental) {
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(4.dp))
                                        .background(OpenXWarningContainer)
                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                ) {
                                    Text("Experimental", color = OpenXWarning, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))
                    Text(engine.description, color = OpenXTextSecondary, fontSize = 12.sp)

                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Limites: ${engine.rateLimitText}",
                        color = if (engine.isZeroCost) OpenXSuccess else OpenXTextSecondary,
                        fontSize = 11.sp
                    )
                }
            }
        }

        // Live Connection Test
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .border(1.dp, OpenXSurfaceBorder, RoundedCornerShape(12.dp)),
                colors = CardDefaults.cardColors(containerColor = OpenXSurface)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Icon(Icons.Default.NetworkCheck, null, tint = OpenXRedLight)
                            Text("Diagnóstico de Conexão", fontWeight = FontWeight.Bold, color = OpenXTextPrimary)
                        }

                        Button(
                            onClick = {
                                val key = when (selectedEngineId) {
                                    "firebase_ai" -> geminiKey
                                    "groq" -> groqKey
                                    "openrouter" -> openRouterKey
                                    else -> ""
                                }
                                val model = when (selectedEngineId) {
                                    "firebase_ai" -> userSession?.geminiModel ?: "gemini-1.5-flash"
                                    "groq" -> userSession?.groqModel ?: "llama-3.3-70b-versatile"
                                    "openrouter" -> userSession?.openRouterModel ?: "meta-llama/llama-3.3-70b-instruct:free"
                                    else -> ""
                                }
                                onTestConnection(selectedEngineId, key, model)
                            },
                            enabled = !isTestingConnection,
                            colors = ButtonDefaults.buttonColors(containerColor = OpenXRed),
                            modifier = Modifier.testTag("btn_test_connection")
                        ) {
                            if (isTestingConnection) {
                                CircularProgressIndicator(color = Color.White, modifier = Modifier.size(16.dp))
                            } else {
                                Text("Testar Motor Ativo", fontSize = 12.sp)
                            }
                        }
                    }

                    if (connectionTestResult != null) {
                        Spacer(modifier = Modifier.height(10.dp))
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (connectionTestResult.isSuccess) OpenXSuccessContainer else OpenXErrorContainer)
                                .padding(10.dp)
                        ) {
                            Column {
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Icon(
                                        imageVector = if (connectionTestResult.isSuccess) Icons.Default.CheckCircle else Icons.Default.Error,
                                        contentDescription = null,
                                        tint = if (connectionTestResult.isSuccess) OpenXSuccess else OpenXError,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Text(
                                        text = connectionTestResult.message,
                                        color = if (connectionTestResult.isSuccess) OpenXSuccess else OpenXError,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 12.sp
                                    )
                                }
                                if (connectionTestResult.latencyMs > 0) {
                                    Text(
                                        text = "Latência: ${connectionTestResult.latencyMs}ms",
                                        color = OpenXTextPrimary,
                                        fontSize = 11.sp
                                    )
                                }
                                if (connectionTestResult.availableModels.isNotEmpty()) {
                                    Text(
                                        text = "Modelos confirmados: ${connectionTestResult.availableModels.joinToString(", ")}",
                                        color = OpenXTextSecondary,
                                        fontSize = 10.sp
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // ML Kit Model Package Manager (On-device models)
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .border(1.dp, OpenXSurfaceBorder, RoundedCornerShape(12.dp)),
                colors = CardDefaults.cardColors(containerColor = OpenXSurface)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text(
                        text = "Pacotes de Idiomas Google ML Kit (On-Device)",
                        fontWeight = FontWeight.Bold,
                        color = OpenXTextPrimary,
                        fontSize = 13.sp
                    )
                    Text(
                        text = "Baixe modelos para o armazenamento interno para traduzir sem consumir internet.",
                        color = OpenXTextSecondary,
                        fontSize = 11.sp
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    mlKitStatuses.forEach { status ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(status.displayName, color = OpenXTextPrimary, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                                Text(
                                    text = if (status.isDownloaded) "Instalado no aparelho" else "Não baixado",
                                    color = if (status.isDownloaded) OpenXSuccess else OpenXTextSecondary,
                                    fontSize = 10.sp
                                )
                            }

                            if (status.isDownloading) {
                                CircularProgressIndicator(color = OpenXRed, modifier = Modifier.size(20.dp))
                            } else if (status.isDownloaded) {
                                OutlinedButton(
                                    onClick = { onDeleteMlKitModel(status.languageCode) },
                                    colors = ButtonDefaults.outlinedButtonColors(contentColor = OpenXError),
                                    modifier = Modifier.height(28.dp)
                                ) {
                                    Text("Excluir", fontSize = 10.sp)
                                }
                            } else {
                                Button(
                                    onClick = { onDownloadMlKitModel(status.languageCode) },
                                    colors = ButtonDefaults.buttonColors(containerColor = OpenXRed),
                                    modifier = Modifier.height(28.dp)
                                ) {
                                    Text("Baixar", fontSize = 10.sp)
                                }
                            }
                        }
                    }
                }
            }
        }

        // API Keys Configuration (User-provided, securely saved to local Room db)
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .border(1.dp, OpenXSurfaceBorder, RoundedCornerShape(12.dp)),
                colors = CardDefaults.cardColors(containerColor = OpenXSurface)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Icon(Icons.Default.Security, null, tint = OpenXRedLight)
                        Text("Configuração Segura de Chaves de Provedores", fontWeight = FontWeight.Bold, color = OpenXTextPrimary)
                    }
                    Text(
                        text = "As chaves são armazenadas exclusivamente no banco de dados local do seu dispositivo e nunca são compartilhadas.",
                        color = OpenXTextSecondary,
                        fontSize = 11.sp
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    // Gemini Key
                    OutlinedTextField(
                        value = geminiKey,
                        onValueChange = { geminiKey = it },
                        label = { Text("Chave Gemini API") },
                        singleLine = true,
                        visualTransformation = if (showGeminiKey) VisualTransformation.None else PasswordVisualTransformation(),
                        trailingIcon = {
                            IconButton(onClick = { showGeminiKey = !showGeminiKey }) {
                                Icon(if (showGeminiKey) Icons.Default.VisibilityOff else Icons.Default.Visibility, null, tint = OpenXTextSecondary)
                            }
                        },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = OpenXRed,
                            unfocusedBorderColor = OpenXSurfaceBorder,
                            focusedTextColor = OpenXTextPrimary,
                            unfocusedTextColor = OpenXTextPrimary
                        ),
                        modifier = Modifier.fillMaxWidth().testTag("input_gemini_key")
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    // Groq Key
                    OutlinedTextField(
                        value = groqKey,
                        onValueChange = { groqKey = it },
                        label = { Text("Chave Groq API") },
                        singleLine = true,
                        visualTransformation = if (showGroqKey) VisualTransformation.None else PasswordVisualTransformation(),
                        trailingIcon = {
                            IconButton(onClick = { showGroqKey = !showGroqKey }) {
                                Icon(if (showGroqKey) Icons.Default.VisibilityOff else Icons.Default.Visibility, null, tint = OpenXTextSecondary)
                            }
                        },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = OpenXRed,
                            unfocusedBorderColor = OpenXSurfaceBorder,
                            focusedTextColor = OpenXTextPrimary,
                            unfocusedTextColor = OpenXTextPrimary
                        ),
                        modifier = Modifier.fillMaxWidth().testTag("input_groq_key")
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    // OpenRouter Key
                    OutlinedTextField(
                        value = openRouterKey,
                        onValueChange = { openRouterKey = it },
                        label = { Text("Chave OpenRouter API") },
                        singleLine = true,
                        visualTransformation = if (showOpenRouterKey) VisualTransformation.None else PasswordVisualTransformation(),
                        trailingIcon = {
                            IconButton(onClick = { showOpenRouterKey = !showOpenRouterKey }) {
                                Icon(if (showOpenRouterKey) Icons.Default.VisibilityOff else Icons.Default.Visibility, null, tint = OpenXTextSecondary)
                            }
                        },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = OpenXRed,
                            unfocusedBorderColor = OpenXSurfaceBorder,
                            focusedTextColor = OpenXTextPrimary,
                            unfocusedTextColor = OpenXTextPrimary
                        ),
                        modifier = Modifier.fillMaxWidth().testTag("input_openrouter_key")
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    Button(
                        onClick = { onSaveApiKeys(geminiKey, groqKey, openRouterKey) },
                        colors = ButtonDefaults.buttonColors(containerColor = OpenXRed),
                        modifier = Modifier.fillMaxWidth().testTag("btn_save_keys")
                    ) {
                        Text("Salvar Chaves Localmente")
                    }
                }
            }
        }

        // Translation Memory (TM) Summary
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .border(1.dp, OpenXSurfaceBorder, RoundedCornerShape(12.dp)),
                colors = CardDefaults.cardColors(containerColor = OpenXSurface)
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(Icons.Default.Storage, null, tint = OpenXSuccess)
                        Column {
                            Text("Memória de Tradução Local", fontWeight = FontWeight.Bold, color = OpenXTextPrimary, fontSize = 13.sp)
                            Text("$tmCount segmentos memorizados (reutilização com custo zero)", color = OpenXTextSecondary, fontSize = 11.sp)
                        }
                    }

                    OutlinedButton(
                        onClick = onClearTm,
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = OpenXError),
                        modifier = Modifier.height(30.dp)
                    ) {
                        Text("Limpar", fontSize = 11.sp)
                    }
                }
            }
        }

        // Error History / Recovery Log
        if (userSession?.lastErrorLog?.isNotBlank() == true) {
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .border(1.dp, OpenXWarning.copy(alpha = 0.5f), RoundedCornerShape(12.dp)),
                    colors = CardDefaults.cardColors(containerColor = OpenXWarningContainer.copy(alpha = 0.3f))
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Icon(Icons.Default.Info, null, tint = OpenXWarning, modifier = Modifier.size(16.dp))
                            Text("Último Registro de Limite / Erro", fontWeight = FontWeight.Bold, color = OpenXWarning, fontSize = 12.sp)
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(userSession.lastErrorLog, color = OpenXTextPrimary, fontSize = 11.sp)
                    }
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(40.dp))
        }
    }
}
