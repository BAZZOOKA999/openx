package com.example.openx.player.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Assessment
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Games
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Translate
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.openx.player.data.RenpyGameEntity
import com.example.openx.player.engine.RenpyEngineManager
import com.example.openx.player.engine.SaveSlotInfo
import com.example.openx.ui.theme.OpenXBackground
import com.example.openx.ui.theme.OpenXError
import com.example.openx.ui.theme.OpenXRed
import com.example.openx.ui.theme.OpenXRedContainer
import com.example.openx.ui.theme.OpenXRedLight
import com.example.openx.ui.theme.OpenXSuccess
import com.example.openx.ui.theme.OpenXSurface
import com.example.openx.ui.theme.OpenXSurfaceBorder
import com.example.openx.ui.theme.OpenXSurfaceVariant
import com.example.openx.ui.theme.OpenXTextPrimary
import com.example.openx.ui.theme.OpenXTextSecondary

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GameDetailScreen(
    game: RenpyGameEntity,
    saveSlots: List<SaveSlotInfo>,
    onBack: () -> Unit,
    onPlayGame: (RenpyGameEntity) -> Unit,
    onTranslateWithAi: (RenpyGameEntity) -> Unit,
    onEditTranslation: (RenpyGameEntity) -> Unit,
    onSelectEngine: (RenpyGameEntity, String) -> Unit,
    onBackupSaves: (RenpyGameEntity) -> Unit,
    onRestoreSaves: (RenpyGameEntity) -> Unit,
    onDeleteSave: (SaveSlotInfo) -> Unit,
    onDeleteGame: (RenpyGameEntity) -> Unit
) {
    var showSavesDialog by remember { mutableStateOf(false) }
    var showDiagnosticsDialog by remember { mutableStateOf(false) }
    var showDeleteConfirmDialog by remember { mutableStateOf(false) }
    var engineMenuExpanded by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(OpenXBackground)
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Back Navigation Bar
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack, modifier = Modifier.testTag("btn_back_library")) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, "Voltar", tint = OpenXTextPrimary)
            }
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "Detalhes do Jogo",
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Bold,
                    color = OpenXTextPrimary
                )
            )
        }

        // Hero Card with stylized banner
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(20.dp))
                .border(1.dp, OpenXRed.copy(alpha = 0.5f), RoundedCornerShape(20.dp)),
            colors = CardDefaults.cardColors(containerColor = OpenXSurface)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        Brush.verticalGradient(
                            listOf(OpenXRedContainer.copy(alpha = 0.6f), OpenXSurface)
                        )
                    )
                    .padding(20.dp)
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(50.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(OpenXRed),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.Games, null, tint = Color.White, modifier = Modifier.size(28.dp))
                        }

                        // Engine Badge
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(OpenXSurfaceVariant)
                                .border(1.dp, OpenXRedLight.copy(alpha = 0.4f), RoundedCornerShape(8.dp))
                                .padding(horizontal = 10.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = game.selectedEngineVersion,
                                style = MaterialTheme.typography.labelSmall.copy(
                                    color = OpenXRedLight,
                                    fontWeight = FontWeight.Bold
                                )
                            )
                        }
                    }

                    Text(
                        text = game.title,
                        style = MaterialTheme.typography.headlineSmall.copy(
                            fontWeight = FontWeight.Black,
                            color = OpenXTextPrimary
                        )
                    )

                    Text(
                        text = "Versão: ${game.version} • Idiomas: ${game.availableLanguages}",
                        style = MaterialTheme.typography.bodySmall.copy(color = OpenXTextSecondary)
                    )

                    Text(
                        text = "Tamanho: ${game.totalSizeBytes / (1024 * 1024)} MB • Saves Registrados: ${saveSlots.size}",
                        style = MaterialTheme.typography.bodySmall.copy(color = OpenXTextSecondary)
                    )
                }
            }
        }

        // 5 Core Buttons Section (Mandatory from User Prompt)
        Text(
            text = "Ações do Jogo",
            style = MaterialTheme.typography.titleMedium.copy(
                fontWeight = FontWeight.Bold,
                color = OpenXTextPrimary
            )
        )

        // 1. JOGAR (Primary Big Red Button)
        Button(
            onClick = { onPlayGame(game) },
            colors = ButtonDefaults.buttonColors(containerColor = OpenXRed),
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp)
                .testTag("btn_play_game")
        ) {
            Icon(Icons.Default.PlayArrow, null, tint = Color.White, modifier = Modifier.size(26.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "JOGAR AGORA",
                fontWeight = FontWeight.Black,
                fontSize = 16.sp,
                color = Color.White
            )
        }

        // 2 & 3: TRADUZIR COM IA & EDITAR TRADUÇÃO
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            ActionTileDetailed(
                modifier = Modifier.weight(1f),
                title = "Traduzir com IA",
                subtitle = "Processar scripts c/ GGUF",
                icon = Icons.Default.Translate,
                tag = "btn_translate_game_ai",
                onClick = { onTranslateWithAi(game) }
            )

            ActionTileDetailed(
                modifier = Modifier.weight(1f),
                title = "Editar Tradução",
                subtitle = "Editor linha por linha",
                icon = Icons.Default.Edit,
                tag = "btn_edit_game_translation",
                onClick = { onEditTranslation(game) }
            )
        }

        // 4 & 5: GERENCIAR SAVES & COMPATIBILIDADE
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            ActionTileDetailed(
                modifier = Modifier.weight(1f),
                title = "Gerenciar Saves",
                subtitle = "${saveSlots.size} saves • Backup ZIP",
                icon = Icons.Default.Save,
                tag = "btn_manage_saves",
                onClick = { showSavesDialog = true }
            )

            ActionTileDetailed(
                modifier = Modifier.weight(1f),
                title = "Compatibilidade",
                subtitle = "${game.confidenceScore}% • Diagnóstico",
                icon = Icons.Default.Assessment,
                tag = "btn_view_diagnostics",
                onClick = { showDiagnosticsDialog = true }
            )
        }

        // Engine Selection Dropdown
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = OpenXSurface),
            border = androidx.compose.foundation.BorderStroke(1.dp, OpenXSurfaceBorder)
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(Icons.Default.Settings, null, tint = OpenXRedLight, modifier = Modifier.size(20.dp))
                    Text(
                        text = "Configuração do Motor de Execução",
                        fontWeight = FontWeight.Bold,
                        color = OpenXTextPrimary,
                        fontSize = 14.sp
                    )
                }

                Text(
                    text = "Alterne o runtime caso o jogo utilize Python 2 (Ren'Py 7) ou Python 3 (Ren'Py 8).",
                    style = MaterialTheme.typography.bodySmall.copy(color = OpenXTextSecondary)
                )

                ExposedDropdownMenuBox(
                    expanded = engineMenuExpanded,
                    onExpandedChange = { engineMenuExpanded = !engineMenuExpanded }
                ) {
                    OutlinedTextField(
                        value = game.selectedEngineVersion,
                        onValueChange = {},
                        readOnly = true,
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = engineMenuExpanded) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor(),
                        colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors(
                            focusedTextColor = OpenXTextPrimary,
                            unfocusedTextColor = OpenXTextPrimary,
                            focusedBorderColor = OpenXRed,
                            unfocusedBorderColor = OpenXSurfaceBorder
                        )
                    )
                    ExposedDropdownMenu(
                        expanded = engineMenuExpanded,
                        onDismissRequest = { engineMenuExpanded = false }
                    ) {
                        RenpyEngineManager.supportedEngines.forEach { engine ->
                            DropdownMenuItem(
                                text = {
                                    Column {
                                        Text(engine.name, fontWeight = FontWeight.Bold, color = OpenXTextPrimary)
                                        Text("${engine.status} • ${engine.supportedVersions}", fontSize = 11.sp, color = OpenXTextSecondary)
                                    }
                                },
                                onClick = {
                                    onSelectEngine(game, engine.name)
                                    engineMenuExpanded = false
                                }
                            )
                        }
                    }
                }
            }
        }

        // Delete Game Button
        OutlinedButton(
            onClick = { showDeleteConfirmDialog = true },
            colors = ButtonDefaults.outlinedButtonColors(contentColor = OpenXError),
            border = androidx.compose.foundation.BorderStroke(1.dp, OpenXError.copy(alpha = 0.5f)),
            modifier = Modifier.fillMaxWidth().testTag("btn_delete_game")
        ) {
            Icon(Icons.Default.Delete, null, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text("Remover Jogo da Biblioteca")
        }
    }

    // Saves Modal
    if (showSavesDialog) {
        SaveManagerDialog(
            game = game,
            saveSlots = saveSlots,
            onDismiss = { showSavesDialog = false },
            onBackupSaves = { onBackupSaves(game) },
            onRestoreSaves = { onRestoreSaves(game) },
            onDeleteSave = { onDeleteSave(it) }
        )
    }

    // Diagnostics Modal
    if (showDiagnosticsDialog) {
        GameDiagnosticsDialog(
            game = game,
            onDismiss = { showDiagnosticsDialog = false }
        )
    }

    // Delete confirmation
    if (showDeleteConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirmDialog = false },
            title = { Text("Remover Jogo", color = OpenXTextPrimary, fontWeight = FontWeight.Bold) },
            text = { Text("Tem certeza que deseja remover '${game.title}'? Seus saves podem ser preservados ou exportados em backup antes.", color = OpenXTextSecondary) },
            confirmButton = {
                Button(
                    onClick = {
                        showDeleteConfirmDialog = false
                        onDeleteGame(game)
                        onBack()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = OpenXError)
                ) {
                    Text("Remover", color = Color.White)
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { showDeleteConfirmDialog = false }) {
                    Text("Cancelar")
                }
            },
            containerColor = OpenXSurface
        )
    }
}

@Composable
private fun ActionTileDetailed(
    modifier: Modifier = Modifier,
    title: String,
    subtitle: String,
    icon: ImageVector,
    tag: String,
    onClick: () -> Unit
) {
    Card(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .border(1.dp, OpenXSurfaceBorder, RoundedCornerShape(12.dp))
            .testTag(tag),
        colors = CardDefaults.cardColors(containerColor = OpenXSurface),
        onClick = onClick
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(OpenXRedContainer),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, null, tint = OpenXRedLight, modifier = Modifier.size(20.dp))
            }
            Text(title, fontWeight = FontWeight.Bold, color = OpenXTextPrimary, fontSize = 13.sp)
            Text(subtitle, color = OpenXTextSecondary, fontSize = 11.sp, maxLines = 1)
        }
    }
}
