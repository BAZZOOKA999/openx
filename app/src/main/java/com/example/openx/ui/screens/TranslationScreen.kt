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
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Book
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.FindReplace
import androidx.compose.material.icons.filled.HourglassEmpty
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.openx.data.db.entities.GlossaryEntity
import com.example.openx.data.db.entities.ProjectEntity
import com.example.openx.data.db.entities.TranslationStatus
import com.example.openx.data.db.entities.TranslationUnitEntity
import com.example.openx.engine.export.ExportReport
import com.example.openx.engine.renpy.RenpyValidator
import com.example.openx.engine.translation.TranslationProgressState
import com.example.openx.engine.translation.TranslationStyle
import com.example.openx.ui.TranslationFilterState
import com.example.openx.ui.components.StatusChip
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
fun TranslationScreen(
    activeProject: ProjectEntity?,
    units: List<TranslationUnitEntity>,
    glossary: List<GlossaryEntity>,
    filterState: TranslationFilterState,
    progressState: TranslationProgressState,
    exportReport: ExportReport?,
    onQueryChange: (String) -> Unit,
    onStatusFilterChange: (TranslationStatus?) -> Unit,
    onUpdateTranslation: (Long, String, TranslationStatus) -> Unit,
    onSetStatus: (Long, TranslationStatus) -> Unit,
    onStartBatchTranslate: (TranslationStyle) -> Unit,
    onPauseBatchTranslate: () -> Unit,
    onResumeBatchTranslate: () -> Unit,
    onCancelBatchTranslate: () -> Unit,
    onReplaceAll: (String, String, Boolean) -> Unit,
    onPrepareExportReport: () -> Unit,
    onDismissExportReport: () -> Unit,
    onAddGlossaryTerm: (String, String, Boolean, String, String, String, String, String, String, String) -> Unit,
    onDeleteGlossaryTerm: (Long) -> Unit,
    onTranslateSingleUnit: ((TranslationUnitEntity) -> Unit)? = null
) {
    var showFindReplaceDialog by remember { mutableStateOf(false) }
    var showBatchStyleDialog by remember { mutableStateOf(false) }
    var showGlossaryDialog by remember { mutableStateOf(false) }

    // Filter units based on search query and status filter
    val filteredUnits = remember(units, filterState.query, filterState.statusFilter) {
        units.filter { unit ->
            val matchesQuery = if (filterState.query.isBlank()) true else {
                unit.originalText.contains(filterState.query, ignoreCase = true) ||
                unit.translatedText.contains(filterState.query, ignoreCase = true) ||
                unit.speaker.contains(filterState.query, ignoreCase = true) ||
                unit.keyOrContext.contains(filterState.query, ignoreCase = true)
            }
            val matchesStatus = filterState.statusFilter == null || unit.status == filterState.statusFilter
            matchesQuery && matchesStatus
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(OpenXBackground)
    ) {
        // Top Toolbar
        Surface(
            color = OpenXSurface,
            tonalElevation = 2.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                // Search Input & Quick Actions
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = filterState.query,
                        onValueChange = onQueryChange,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("search_units_input"),
                        placeholder = { Text("Pesquisar no original ou tradução...", color = OpenXTextSecondary, fontSize = 13.sp) },
                        leadingIcon = { Icon(Icons.Default.Search, null, tint = OpenXTextSecondary, modifier = Modifier.size(18.dp)) },
                        trailingIcon = {
                            if (filterState.query.isNotBlank()) {
                                IconButton(onClick = { onQueryChange("") }) {
                                    Icon(Icons.Default.Close, null, tint = OpenXTextSecondary, modifier = Modifier.size(16.dp))
                                }
                            }
                        },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = OpenXRed,
                            unfocusedBorderColor = OpenXSurfaceBorder,
                            focusedTextColor = OpenXTextPrimary,
                            unfocusedTextColor = OpenXTextPrimary
                        ),
                        shape = RoundedCornerShape(10.dp)
                    )

                    // Find & Replace Button
                    IconButton(
                        onClick = { showFindReplaceDialog = true },
                        modifier = Modifier
                            .clip(RoundedCornerShape(10.dp))
                            .background(OpenXSurfaceVariant)
                            .testTag("btn_find_replace")
                    ) {
                        Icon(Icons.Default.FindReplace, contentDescription = "Localizar e Substituir", tint = OpenXTextPrimary)
                    }

                    // Glossary Button (Character & Terms)
                    IconButton(
                        onClick = { showGlossaryDialog = true },
                        modifier = Modifier
                            .clip(RoundedCornerShape(10.dp))
                            .background(OpenXSurfaceVariant)
                            .testTag("btn_open_glossary")
                    ) {
                        Icon(Icons.Default.Person, contentDescription = "Glossário de Personagens", tint = OpenXTextPrimary)
                    }

                    // Batch Translate Button
                    IconButton(
                        onClick = { showBatchStyleDialog = true },
                        modifier = Modifier
                            .clip(RoundedCornerShape(10.dp))
                            .background(OpenXRed)
                            .testTag("btn_batch_translate")
                    ) {
                        Icon(Icons.Default.AutoAwesome, contentDescription = "Traduzir Lote", tint = Color.White)
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Status Filter Chips
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    item {
                        FilterChip(
                            selected = filterState.statusFilter == null,
                            onClick = { onStatusFilterChange(null) },
                            label = { Text("Todos (${units.size})", fontSize = 11.sp) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = OpenXRed,
                                selectedLabelColor = Color.White,
                                containerColor = OpenXSurfaceVariant,
                                labelColor = OpenXTextSecondary
                            )
                        )
                    }
                    item {
                        val count = units.count { it.status == TranslationStatus.PENDING }
                        FilterChip(
                            selected = filterState.statusFilter == TranslationStatus.PENDING,
                            onClick = { onStatusFilterChange(TranslationStatus.PENDING) },
                            label = { Text("Pendentes ($count)", fontSize = 11.sp) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = OpenXWarningContainer,
                                selectedLabelColor = OpenXWarning,
                                containerColor = OpenXSurfaceVariant,
                                labelColor = OpenXTextSecondary
                            )
                        )
                    }
                    item {
                        val count = units.count { it.status == TranslationStatus.TRANSLATED }
                        FilterChip(
                            selected = filterState.statusFilter == TranslationStatus.TRANSLATED,
                            onClick = { onStatusFilterChange(TranslationStatus.TRANSLATED) },
                            label = { Text("Traduzidos ($count)", fontSize = 11.sp) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = OpenXRedContainer,
                                selectedLabelColor = OpenXRedLight,
                                containerColor = OpenXSurfaceVariant,
                                labelColor = OpenXTextSecondary
                            )
                        )
                    }
                    item {
                        val count = units.count { it.status == TranslationStatus.REVIEWED }
                        FilterChip(
                            selected = filterState.statusFilter == TranslationStatus.REVIEWED,
                            onClick = { onStatusFilterChange(TranslationStatus.REVIEWED) },
                            label = { Text("Revisados / TM ($count)", fontSize = 11.sp) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = OpenXSuccessContainer,
                                selectedLabelColor = OpenXSuccess,
                                containerColor = OpenXSurfaceVariant,
                                labelColor = OpenXTextSecondary
                            )
                        )
                    }
                    item {
                        val count = units.count { it.status == TranslationStatus.ERROR || it.validationError != null }
                        FilterChip(
                            selected = filterState.statusFilter == TranslationStatus.ERROR,
                            onClick = { onStatusFilterChange(TranslationStatus.ERROR) },
                            label = { Text("Avisos/Erros ($count)", fontSize = 11.sp) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = OpenXErrorContainer,
                                selectedLabelColor = OpenXError,
                                containerColor = OpenXSurfaceVariant,
                                labelColor = OpenXTextSecondary
                            )
                        )
                    }
                }
            }
        }

        // Active Batch Translation Progress Banner (real controls: Pause, Resume, Cancel + TM metrics)
        AnimatedVisibility(visible = progressState.isRunning || progressState.isPaused) {
            val progress = if (progressState.totalCount > 0) {
                progressState.currentIndex.toFloat() / progressState.totalCount
            } else 0f

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp),
                colors = CardDefaults.cardColors(containerColor = OpenXSurfaceVariant)
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (progressState.isPaused) "Fila Pausada no Checkpoint" else "Processando Fila • ${progressState.activeEngineName}",
                            style = MaterialTheme.typography.bodyMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = if (progressState.isPaused) OpenXWarning else OpenXRedLight
                            )
                        )
                        Text(
                            text = "${progressState.currentIndex} / ${progressState.totalCount}",
                            style = MaterialTheme.typography.bodySmall.copy(color = OpenXTextPrimary)
                        )
                    }

                    Spacer(modifier = Modifier.height(6.dp))
                    LinearProgressIndicator(
                        progress = { progress },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(6.dp)
                            .clip(RoundedCornerShape(3.dp)),
                        color = OpenXRed,
                        trackColor = OpenXSurface
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    // Real-time hybrid metrics: TM reused + Online calls
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "TM Reutilizada: ${progressState.reusedFromMemoryCount} • Chamadas online: ${progressState.onlineCallsCount} • Erros: ${progressState.errorCount}",
                            style = MaterialTheme.typography.labelSmall.copy(color = OpenXSuccess),
                            fontSize = 11.sp
                        )
                    }

                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = progressState.statusMessage,
                        style = MaterialTheme.typography.labelSmall.copy(color = OpenXTextSecondary),
                        maxLines = 1
                    )

                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (progressState.isPaused) {
                            Button(
                                onClick = onResumeBatchTranslate,
                                colors = ButtonDefaults.buttonColors(containerColor = OpenXSuccess),
                                modifier = Modifier.height(34.dp)
                            ) {
                                Icon(Icons.Default.PlayArrow, null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Retomar", fontSize = 12.sp)
                            }
                        } else {
                            OutlinedButton(
                                onClick = onPauseBatchTranslate,
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = OpenXWarning),
                                modifier = Modifier.height(34.dp)
                            ) {
                                Icon(Icons.Default.Pause, null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Pausar", fontSize = 12.sp)
                            }
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(
                            onClick = onCancelBatchTranslate,
                            colors = ButtonDefaults.buttonColors(containerColor = OpenXError),
                            modifier = Modifier.height(34.dp)
                        ) {
                            Icon(Icons.Default.Stop, null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Cancelar", fontSize = 12.sp)
                        }
                    }
                }
            }
        }

        // Units List
        if (filteredUnits.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = null,
                        tint = OpenXTextSecondary,
                        modifier = Modifier.size(48.dp)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "Nenhum segmento encontrado",
                        style = MaterialTheme.typography.titleMedium.copy(color = OpenXTextPrimary)
                    )
                    Text(
                        text = "Tente alterar os filtros ou pesquisar outro termo.",
                        style = MaterialTheme.typography.bodySmall.copy(color = OpenXTextSecondary)
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 12.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                item {
                    Spacer(modifier = Modifier.height(4.dp))
                }

                items(filteredUnits, key = { it.id }) { unit ->
                    TranslationCardItem(
                        unit = unit,
                        onUpdate = { newText, newStatus ->
                            onUpdateTranslation(unit.id, newText, newStatus)
                        },
                        onSetStatus = { newStatus ->
                            onSetStatus(unit.id, newStatus)
                        },
                        onRetranslate = {
                            onTranslateSingleUnit?.invoke(unit)
                        }
                    )
                }

                item {
                    Spacer(modifier = Modifier.height(60.dp))
                }
            }
        }
    }

    // Batch Translation Style Chooser Dialog
    if (showBatchStyleDialog) {
        var selectedStyle by remember { mutableStateOf(TranslationStyle.NATURAL) }

        AlertDialog(
            onDismissRequest = { showBatchStyleDialog = false },
            containerColor = OpenXSurface,
            title = {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(Icons.Default.AutoAwesome, null, tint = OpenXRedLight)
                    Text("Traduzir Fila com IA Híbrida", color = OpenXTextPrimary, fontWeight = FontWeight.Bold)
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        "Selecione o estilo da narrativa. Placeholders [variável] e tags {b} são preservados com precisão.",
                        color = OpenXTextSecondary,
                        fontSize = 13.sp
                    )

                    TranslationStyle.values().forEach { style ->
                        val isChosen = selectedStyle == style
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .border(
                                    1.dp,
                                    if (isChosen) OpenXRed else OpenXSurfaceBorder,
                                    RoundedCornerShape(10.dp)
                                )
                                .clickable { selectedStyle = style },
                            colors = CardDefaults.cardColors(
                                containerColor = if (isChosen) OpenXSurfaceVariant else OpenXSurface
                            )
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Text(
                                    text = style.label,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isChosen) OpenXRedLight else OpenXTextPrimary,
                                    fontSize = 14.sp
                                )
                                Text(
                                    text = style.promptInstruction,
                                    color = OpenXTextSecondary,
                                    fontSize = 11.sp,
                                    lineHeight = 15.sp
                                )
                            }
                        }
                    }

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(OpenXSuccessContainer)
                            .padding(8.dp)
                    ) {
                        Text(
                            "Prioridade: Memória de Tradução (TM) local reutilizada a custo zero. Fallback on-device para Google ML Kit em caso de instabilidade de rede.",
                            color = OpenXSuccess,
                            fontSize = 11.sp
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        showBatchStyleDialog = false
                        onStartBatchTranslate(selectedStyle)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = OpenXRed)
                ) {
                    Text("Iniciar Tradução")
                }
            },
            dismissButton = {
                TextButton(onClick = { showBatchStyleDialog = false }) {
                    Text("Cancelar", color = OpenXTextSecondary)
                }
            }
        )
    }

    // Find and Replace Dialog
    if (showFindReplaceDialog) {
        var findText by remember { mutableStateOf("") }
        var replaceText by remember { mutableStateOf("") }
        var matchCase by remember { mutableStateOf(false) }

        AlertDialog(
            onDismissRequest = { showFindReplaceDialog = false },
            containerColor = OpenXSurface,
            title = {
                Text("Localizar e Substituir", color = OpenXTextPrimary, fontWeight = FontWeight.Bold)
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = findText,
                        onValueChange = { findText = it },
                        label = { Text("Localizar no texto traduzido") },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = OpenXRed,
                            unfocusedBorderColor = OpenXSurfaceBorder,
                            focusedTextColor = OpenXTextPrimary,
                            unfocusedTextColor = OpenXTextPrimary
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = replaceText,
                        onValueChange = { replaceText = it },
                        label = { Text("Substituir por") },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = OpenXRed,
                            unfocusedBorderColor = OpenXSurfaceBorder,
                            focusedTextColor = OpenXTextPrimary,
                            unfocusedTextColor = OpenXTextPrimary
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        FilterChip(
                            selected = matchCase,
                            onClick = { matchCase = !matchCase },
                            label = { Text("Diferenciar maiúsculas/minúsculas", fontSize = 11.sp) }
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (findText.isNotBlank()) {
                            onReplaceAll(findText, replaceText, matchCase)
                            showFindReplaceDialog = false
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = OpenXRed)
                ) {
                    Text("Substituir Tudo")
                }
            },
            dismissButton = {
                TextButton(onClick = { showFindReplaceDialog = false }) {
                    Text("Cancelar", color = OpenXTextSecondary)
                }
            }
        )
    }

    // Advanced Character & Terms Glossary Dialog
    if (showGlossaryDialog) {
        var newSource by remember { mutableStateOf("") }
        var newTarget by remember { mutableStateOf("") }
        var isDnt by remember { mutableStateOf(false) }
        var category by remember { mutableStateOf("Personagem") }
        var gender by remember { mutableStateOf("Não especificado") }
        var characterRole by remember { mutableStateOf("") }
        var relationship by remember { mutableStateOf("") }
        var personality by remember { mutableStateOf("") }
        var speechStyle by remember { mutableStateOf("") }

        AlertDialog(
            onDismissRequest = { showGlossaryDialog = false },
            containerColor = OpenXSurface,
            title = {
                Text("Glossário de Personagens & Termos", color = OpenXTextPrimary, fontWeight = FontWeight.Bold)
            },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(420.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        "Cadastre nomes, apelidos, gênero gramatical, relações e personalidade para guiar o contexto da tradução.",
                        fontSize = 11.sp,
                        color = OpenXTextSecondary
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        OutlinedTextField(
                            value = newSource,
                            onValueChange = { newSource = it },
                            placeholder = { Text("Nome / Termo Original", fontSize = 11.sp) },
                            singleLine = true,
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(8.dp)
                        )
                        OutlinedTextField(
                            value = if (isDnt) "(Não traduzir)" else newTarget,
                            onValueChange = { if (!isDnt) newTarget = it },
                            enabled = !isDnt,
                            placeholder = { Text("Tradução / Grafia em PT", fontSize = 11.sp) },
                            singleLine = true,
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(8.dp)
                        )
                    }

                    // Character attributes row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        OutlinedTextField(
                            value = relationship,
                            onValueChange = { relationship = it },
                            placeholder = { Text("Relação (ex: Irmã)", fontSize = 10.sp) },
                            singleLine = true,
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(8.dp)
                        )
                        OutlinedTextField(
                            value = speechStyle,
                            onValueChange = { speechStyle = it },
                            placeholder = { Text("Fala (ex: Formal, Gírias)", fontSize = 10.sp) },
                            singleLine = true,
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(8.dp)
                        )
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        FilterChip(
                            selected = isDnt,
                            onClick = { isDnt = !isDnt },
                            label = { Text("Bloquear (DNT)", fontSize = 11.sp) }
                        )
                        FilterChip(
                            selected = gender == "Feminino",
                            onClick = { gender = if (gender == "Feminino") "Não especificado" else "Feminino" },
                            label = { Text("Fem", fontSize = 11.sp) }
                        )
                        FilterChip(
                            selected = gender == "Masculino",
                            onClick = { gender = if (gender == "Masculino") "Não especificado" else "Masculino" },
                            label = { Text("Masc", fontSize = 11.sp) }
                        )
                        Spacer(modifier = Modifier.weight(1f))
                        Button(
                            onClick = {
                                if (newSource.isNotBlank()) {
                                    onAddGlossaryTerm(
                                        newSource,
                                        if (isDnt) newSource else newTarget,
                                        isDnt,
                                        category,
                                        characterRole,
                                        gender,
                                        relationship,
                                        personality,
                                        speechStyle,
                                        ""
                                    )
                                    newSource = ""
                                    newTarget = ""
                                    relationship = ""
                                    speechStyle = ""
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = OpenXRed),
                            modifier = Modifier.height(32.dp)
                        ) {
                            Text("Adicionar", fontSize = 11.sp)
                        }
                    }

                    Text("Termos & Personagens (${glossary.size}):", fontWeight = FontWeight.Bold, color = OpenXTextPrimary, fontSize = 12.sp)

                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                            .clip(RoundedCornerShape(8.dp))
                            .background(OpenXSurfaceVariant)
                            .padding(8.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        items(glossary) { item ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "${item.sourceTerm} → ${if (item.doNotTranslate) "[DNT]" else item.targetTerm}",
                                        color = if (item.doNotTranslate) OpenXWarning else OpenXTextPrimary,
                                        fontWeight = FontWeight.SemiBold,
                                        fontSize = 12.sp
                                    )
                                    val details = listOfNotNull(
                                        if (item.gender != "Não especificado") item.gender else null,
                                        item.relationship.ifBlank { null },
                                        item.speechStyle.ifBlank { null }
                                    ).joinToString(" • ")
                                    if (details.isNotBlank()) {
                                        Text(text = details, color = OpenXTextSecondary, fontSize = 10.sp)
                                    }
                                }
                                IconButton(
                                    onClick = { onDeleteGlossaryTerm(item.id) },
                                    modifier = Modifier.size(24.dp)
                                ) {
                                    Icon(Icons.Default.Close, null, tint = OpenXError, modifier = Modifier.size(14.dp))
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = { showGlossaryDialog = false },
                    colors = ButtonDefaults.buttonColors(containerColor = OpenXRed)
                ) {
                    Text("Fechar")
                }
            }
        )
    }
}

@Composable
fun TranslationCardItem(
    unit: TranslationUnitEntity,
    onUpdate: (String, TranslationStatus) -> Unit,
    onSetStatus: (TranslationStatus) -> Unit,
    onRetranslate: (() -> Unit)? = null
) {
    var textValue by remember(unit.id, unit.translatedText) { mutableStateOf(unit.translatedText) }
    var isDirty by remember(unit.id, unit.translatedText) { mutableStateOf(false) }
    val undoStack = remember(unit.id) { mutableListOf<String>() }
    val redoStack = remember(unit.id) { mutableListOf<String>() }
    var showComparisonDialog by remember { mutableStateOf(false) }

    val placeholders = remember(unit.originalText) {
        RenpyValidator.extractPlaceholders(unit.originalText).distinct()
    }

    val tags = remember(unit.originalText) {
        RenpyValidator.extractTags(unit.originalText).distinct()
    }

    val validation = remember(unit.originalText, textValue) {
        RenpyValidator.validateTranslation(unit.originalText, textValue)
    }

    fun handleTextChange(newVal: String) {
        if (newVal != textValue) {
            undoStack.add(textValue)
            redoStack.clear()
            textValue = newVal
            isDirty = true
        }
    }

    fun handleUndo() {
        if (undoStack.isNotEmpty()) {
            val prev = undoStack.removeAt(undoStack.lastIndex)
            redoStack.add(textValue)
            textValue = prev
            isDirty = (textValue != unit.translatedText)
        }
    }

    fun handleRedo() {
        if (redoStack.isNotEmpty()) {
            val next = redoStack.removeAt(redoStack.lastIndex)
            undoStack.add(textValue)
            textValue = next
            isDirty = (textValue != unit.translatedText)
        }
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .border(
                1.dp,
                if (validation.isValid) OpenXSurfaceBorder else OpenXError.copy(alpha = 0.6f),
                RoundedCornerShape(12.dp)
            )
            .testTag("translation_card_${unit.id}"),
        colors = CardDefaults.cardColors(containerColor = OpenXSurface)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            // Header: speaker, order, block type, status
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = "#${unit.orderIndex + 1}",
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = OpenXTextSecondary,
                            fontWeight = FontWeight.Bold
                        )
                    )
                    if (unit.speaker.isNotBlank()) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(OpenXRedContainer)
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = unit.speaker,
                                style = MaterialTheme.typography.labelSmall.copy(
                                    color = OpenXRedLight,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 11.sp
                                )
                            )
                        }
                    }
                    if (unit.keyOrContext.isNotBlank() && unit.speaker != unit.keyOrContext) {
                        Text(
                            text = unit.keyOrContext,
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = OpenXTextSecondary,
                                fontSize = 10.sp
                            )
                        )
                    }
                }

                StatusChip(unit.status)
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Original Text Box
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(OpenXSurfaceVariant)
                    .padding(10.dp)
            ) {
                Text(
                    text = unit.originalText,
                    style = MaterialTheme.typography.bodyMedium.copy(
                        color = OpenXTextPrimary,
                        fontSize = 14.sp,
                        lineHeight = 20.sp,
                        fontFamily = FontFamily.SansSerif
                    )
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Quick Toolbar Row (Copy, Undo, Redo, Compare, Retranslate, Placeholders, Tags)
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                item {
                    // Copy original button
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(OpenXSurfaceBorder)
                            .clickable {
                                handleTextChange(unit.originalText)
                            }
                            .padding(horizontal = 6.dp, vertical = 3.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.ContentCopy, null, tint = OpenXTextSecondary, modifier = Modifier.size(12.dp))
                            Spacer(modifier = Modifier.width(3.dp))
                            Text("Copiar", color = OpenXTextSecondary, fontSize = 10.sp)
                        }
                    }
                }

                // Undo Button
                item {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(if (undoStack.isNotEmpty()) OpenXRedContainer else OpenXSurfaceBorder.copy(alpha = 0.5f))
                            .clickable(enabled = undoStack.isNotEmpty()) { handleUndo() }
                            .padding(horizontal = 6.dp, vertical = 3.dp)
                    ) {
                        Text(
                            text = "Desfazer (${undoStack.size})",
                            color = if (undoStack.isNotEmpty()) OpenXRedLight else OpenXTextSecondary,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                // Redo Button
                item {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(if (redoStack.isNotEmpty()) OpenXRedContainer else OpenXSurfaceBorder.copy(alpha = 0.5f))
                            .clickable(enabled = redoStack.isNotEmpty()) { handleRedo() }
                            .padding(horizontal = 6.dp, vertical = 3.dp)
                    ) {
                        Text(
                            text = "Refazer (${redoStack.size})",
                            color = if (redoStack.isNotEmpty()) OpenXRedLight else OpenXTextSecondary,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                // Compare Versions Button
                item {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(OpenXSurfaceVariant)
                            .border(1.dp, OpenXSurfaceBorder, RoundedCornerShape(6.dp))
                            .clickable { showComparisonDialog = true }
                            .padding(horizontal = 6.dp, vertical = 3.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.FindReplace, null, tint = OpenXTextPrimary, modifier = Modifier.size(12.dp))
                            Spacer(modifier = Modifier.width(3.dp))
                            Text("Comparar Versões", color = OpenXTextPrimary, fontSize = 10.sp)
                        }
                    }
                }

                // Retranslate Single Unit Button
                if (onRetranslate != null) {
                    item {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(OpenXRedContainer)
                                .clickable { onRetranslate() }
                                .padding(horizontal = 6.dp, vertical = 3.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.AutoAwesome, null, tint = OpenXRed, modifier = Modifier.size(12.dp))
                                Spacer(modifier = Modifier.width(3.dp))
                                Text("Retraduzir IA", color = OpenXRed, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }

                // Placeholders quick click
                items(placeholders) { p ->
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(OpenXInfoContainer)
                            .border(1.dp, OpenXInfo.copy(alpha = 0.4f), RoundedCornerShape(6.dp))
                            .clickable {
                                val nextText = if (textValue.endsWith(" ") || textValue.isEmpty()) "$textValue$p" else "$textValue $p"
                                handleTextChange(nextText)
                            }
                            .padding(horizontal = 6.dp, vertical = 3.dp)
                    ) {
                        Text(text = "+$p", color = OpenXInfo, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    }
                }

                // Tags quick click
                items(tags) { t ->
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(OpenXRedContainer)
                            .border(1.dp, OpenXRed.copy(alpha = 0.4f), RoundedCornerShape(6.dp))
                            .clickable {
                                handleTextChange("$textValue$t")
                            }
                            .padding(horizontal = 6.dp, vertical = 3.dp)
                    ) {
                        Text(text = "+$t", color = OpenXRedLight, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Translated Text Field
            OutlinedTextField(
                value = textValue,
                onValueChange = { handleTextChange(it) },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("translation_input_${unit.id}"),
                placeholder = { Text("Insira a tradução em português...", color = OpenXTextSecondary, fontSize = 13.sp) },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = if (validation.isValid) OpenXRed else OpenXError,
                    unfocusedBorderColor = if (validation.isValid) OpenXSurfaceBorder else OpenXError.copy(alpha = 0.6f),
                    focusedTextColor = OpenXTextPrimary,
                    unfocusedTextColor = OpenXTextPrimary
                ),
                shape = RoundedCornerShape(8.dp)
            )

            // Validation Warning Badge
            if (!validation.isValid && validation.errorMessage != null) {
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(OpenXErrorContainer)
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Icon(Icons.Default.Warning, null, tint = OpenXError, modifier = Modifier.size(14.dp))
                    Text(
                        text = validation.errorMessage,
                        color = OpenXError,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Action Row: Save, Mark Reviewed, Revert
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    // Mark as Reviewed button (also saves to TM)
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(if (unit.status == TranslationStatus.REVIEWED) OpenXSuccessContainer else OpenXSurfaceVariant)
                            .clickable {
                                onSetStatus(if (unit.status == TranslationStatus.REVIEWED) TranslationStatus.TRANSLATED else TranslationStatus.REVIEWED)
                            }
                            .padding(horizontal = 8.dp, vertical = 5.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Check, null, tint = OpenXSuccess, modifier = Modifier.size(13.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Revisado (TM)", color = OpenXSuccess, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }

                    // Mark as Error button
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(if (unit.status == TranslationStatus.ERROR) OpenXErrorContainer else OpenXSurfaceVariant)
                            .clickable {
                                onSetStatus(if (unit.status == TranslationStatus.ERROR) TranslationStatus.PENDING else TranslationStatus.ERROR)
                            }
                            .padding(horizontal = 8.dp, vertical = 5.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Error, null, tint = OpenXError, modifier = Modifier.size(13.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Erro", color = OpenXError, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                // Save button
                Button(
                    onClick = {
                        val status = if (textValue.isBlank()) TranslationStatus.PENDING else TranslationStatus.TRANSLATED
                        onUpdate(textValue, status)
                        isDirty = false
                    },
                    enabled = isDirty,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = OpenXRed,
                        disabledContainerColor = OpenXSurfaceVariant
                    ),
                    shape = RoundedCornerShape(6.dp),
                    modifier = Modifier
                        .height(32.dp)
                        .testTag("btn_save_unit_${unit.id}")
                ) {
                    Text("Salvar", fontSize = 11.sp)
                }
            }
        }
    }

    // Version Comparison Dialog
    if (showComparisonDialog) {
        AlertDialog(
            onDismissRequest = { showComparisonDialog = false },
            containerColor = OpenXSurface,
            title = {
                Text(
                    text = "Comparação de Versões #${unit.orderIndex + 1}",
                    color = OpenXTextPrimary,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text("Original (Fonte):", color = OpenXTextSecondary, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(6.dp))
                            .background(OpenXSurfaceVariant)
                            .padding(8.dp)
                    ) {
                        Text(unit.originalText, color = OpenXTextPrimary, fontSize = 12.sp)
                    }

                    Text("Versão Anterior Salva:", color = OpenXTextSecondary, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(6.dp))
                            .background(OpenXSurfaceVariant)
                            .padding(8.dp)
                    ) {
                        Text(
                            text = unit.translatedText.ifBlank { "(Nenhuma tradução prévia salva)" },
                            color = if (unit.translatedText.isBlank()) OpenXTextSecondary else OpenXTextPrimary,
                            fontSize = 12.sp
                        )
                    }

                    Text("Edição Atual / Rascunho:", color = OpenXTextSecondary, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(6.dp))
                            .background(OpenXRedContainer.copy(alpha = 0.25f))
                            .border(1.dp, OpenXRed.copy(alpha = 0.5f), RoundedCornerShape(6.dp))
                            .padding(8.dp)
                    ) {
                        Text(
                            text = textValue.ifBlank { "(Vazio)" },
                            color = OpenXTextPrimary,
                            fontSize = 12.sp
                        )
                    }

                    // Tag integrity status
                    if (validation.isValid) {
                        Text(
                            text = "Integridade: Todas as tags e variáveis foram preservadas com sucesso.",
                            color = OpenXSuccess,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    } else {
                        Text(
                            text = "Aviso de Sintaxe: ${validation.errorMessage}",
                            color = OpenXError,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        textValue = unit.translatedText
                        isDirty = false
                        undoStack.clear()
                        redoStack.clear()
                        showComparisonDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = OpenXSurfaceBorder)
                ) {
                    Text("Restaurar Salvo", color = OpenXTextPrimary, fontSize = 11.sp)
                }
            },
            dismissButton = {
                Button(
                    onClick = { showComparisonDialog = false },
                    colors = ButtonDefaults.buttonColors(containerColor = OpenXRed)
                ) {
                    Text("Fechar", fontSize = 11.sp)
                }
            }
        )
    }
}
