package com.example.openx.player.ui

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Archive
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.Games
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Translate
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.openx.player.data.RenpyGameEntity
import com.example.openx.player.engine.ZipExtractionProgress
import com.example.openx.ui.theme.OpenXBackground
import com.example.openx.ui.theme.OpenXRed
import com.example.openx.ui.theme.OpenXRedContainer
import com.example.openx.ui.theme.OpenXRedLight
import com.example.openx.ui.theme.OpenXSuccess
import com.example.openx.ui.theme.OpenXSurface
import com.example.openx.ui.theme.OpenXSurfaceBorder
import com.example.openx.ui.theme.OpenXSurfaceVariant
import com.example.openx.ui.theme.OpenXTextPrimary
import com.example.openx.ui.theme.OpenXTextSecondary

@Composable
fun GameLibraryScreen(
    games: List<RenpyGameEntity>,
    extractionProgress: ZipExtractionProgress?,
    onSelectGame: (RenpyGameEntity) -> Unit,
    onPlayGame: (RenpyGameEntity) -> Unit,
    onTranslateGame: (RenpyGameEntity) -> Unit,
    onImportGameZip: (Uri) -> Unit,
    onImportGameFolder: (Uri) -> Unit,
    onPlayDemo: () -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }

    val zipPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri != null) {
            onImportGameZip(uri)
        }
    }

    val folderPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocumentTree()
    ) { uri: Uri? ->
        if (uri != null) {
            onImportGameFolder(uri)
        }
    }

    val filteredGames = remember(games, searchQuery) {
        if (searchQuery.isBlank()) games
        else games.filter { it.title.contains(searchQuery, ignoreCase = true) }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(OpenXBackground)
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Spacer(modifier = Modifier.height(4.dp))
            // Hero Banner OpenX Player
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(20.dp))
                    .border(1.dp, OpenXRed.copy(alpha = 0.4f), RoundedCornerShape(20.dp)),
                colors = CardDefaults.cardColors(containerColor = OpenXSurface)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            Brush.verticalGradient(
                                listOf(OpenXRedContainer.copy(alpha = 0.5f), OpenXSurface)
                            )
                        )
                        .padding(20.dp)
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(44.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(OpenXRed),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.Games, null, tint = Color.White, modifier = Modifier.size(26.dp))
                            }
                            Column {
                                Text(
                                    text = "OpenX Player",
                                    style = MaterialTheme.typography.titleLarge.copy(
                                        fontWeight = FontWeight.Black,
                                        color = OpenXTextPrimary
                                    )
                                )
                                Text(
                                    text = "Biblioteca de Jogos Ren'Py PC no Android",
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        color = OpenXTextSecondary
                                    )
                                )
                            }
                        }

                        Text(
                            text = "Importe visual novels de computador em ZIP ou pasta. Execução nativa ARM64, isolamento de saves e tradução offline integrada.",
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = OpenXTextSecondary,
                                lineHeight = 18.sp
                            )
                        )

                        // Quick stats pills
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            StatPill(label = "Jogos", value = "${games.size}")
                            StatPill(label = "Com Tradução", value = "${games.count { it.hasPortugueseTl }}")
                            StatPill(label = "Motor", value = "Ren'Py 8/7")
                        }
                    }
                }
            }
        }

        // Import Actions Bar
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Button(
                    onClick = { zipPicker.launch(arrayOf("application/zip", "*/*")) },
                    colors = ButtonDefaults.buttonColors(containerColor = OpenXRed),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.weight(1f).testTag("btn_import_game_zip")
                ) {
                    Icon(Icons.Default.Archive, null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Importar ZIP", fontSize = 13.sp)
                }

                OutlinedButton(
                    onClick = { folderPicker.launch(null) },
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = OpenXTextPrimary),
                    border = androidx.compose.foundation.BorderStroke(1.dp, OpenXSurfaceBorder),
                    modifier = Modifier.weight(1f).testTag("btn_import_game_folder")
                ) {
                    Icon(Icons.Default.FolderOpen, null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Importar Pasta", fontSize = 13.sp)
                }
            }
        }

        // Search Bar
        item {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text("Buscar visual novel na biblioteca...", color = OpenXTextSecondary) },
                leadingIcon = { Icon(Icons.Default.Search, null, tint = OpenXTextSecondary) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth().testTag("search_game_input"),
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = OpenXTextPrimary,
                    unfocusedTextColor = OpenXTextPrimary,
                    focusedBorderColor = OpenXRed,
                    unfocusedBorderColor = OpenXSurfaceBorder,
                    focusedContainerColor = OpenXSurface,
                    unfocusedContainerColor = OpenXSurface
                )
            )
        }

        // Games List
        if (filteredGames.isEmpty()) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 20.dp),
                    colors = CardDefaults.cardColors(containerColor = OpenXSurface)
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Icon(Icons.Default.Games, null, tint = OpenXRedLight, modifier = Modifier.size(48.dp))
                        Text(
                            text = "Nenhum jogo encontrado",
                            color = OpenXTextPrimary,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Importe um arquivo ZIP ou pasta de jogo Ren'Py, ou experimente o jogo oficial demonstrativo.",
                            color = OpenXTextSecondary,
                            fontSize = 12.sp,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                        Button(
                            onClick = onPlayDemo,
                            colors = ButtonDefaults.buttonColors(containerColor = OpenXRed),
                            modifier = Modifier.testTag("btn_play_demo_fallback")
                        ) {
                            Text("Carregar Demonstração Ren'Py 8")
                        }
                    }
                }
            }
        } else {
            items(filteredGames) { game ->
                GameCardItem(
                    game = game,
                    onPlay = { onPlayGame(game) },
                    onTranslate = { onTranslateGame(game) },
                    onDetails = { onSelectGame(game) }
                )
            }
        }

        item {
            Spacer(modifier = Modifier.height(20.dp))
        }
    }

    // Extraction Progress Dialog (Zip Slip & Zip Bomb protection indicator)
    if (extractionProgress != null && !extractionProgress.isComplete) {
        AlertDialog(
            onDismissRequest = {},
            title = {
                Text(
                    text = "Importando Visual Novel Ren'Py",
                    color = OpenXTextPrimary,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "Arquivo: ${extractionProgress.currentFileName}",
                        color = OpenXTextSecondary,
                        fontSize = 12.sp,
                        maxLines = 1
                    )
                    LinearProgressIndicator(
                        modifier = Modifier.fillMaxWidth(),
                        color = OpenXRed,
                        trackColor = OpenXSurfaceVariant
                    )
                    Text(
                        text = "${extractionProgress.filesExtracted} arquivos extraídos com segurança (proteção ativa contra Zip Slip e Zip Bomb).",
                        color = OpenXTextSecondary,
                        fontSize = 11.sp
                    )
                }
            },
            confirmButton = {},
            containerColor = OpenXSurface
        )
    }
}

@Composable
private fun GameCardItem(
    game: RenpyGameEntity,
    onPlay: () -> Unit,
    onTranslate: () -> Unit,
    onDetails: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .border(1.dp, OpenXSurfaceBorder, RoundedCornerShape(16.dp))
            .clickable(onClick = onDetails)
            .testTag("game_card_${game.id}"),
        colors = CardDefaults.cardColors(containerColor = OpenXSurface)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Game Thumbnail / Icon
                Box(
                    modifier = Modifier
                        .size(54.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(OpenXRedContainer),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.Games, null, tint = OpenXRed, modifier = Modifier.size(28.dp))
                }

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = game.title,
                        fontWeight = FontWeight.Bold,
                        color = OpenXTextPrimary,
                        fontSize = 15.sp,
                        maxLines = 1
                    )
                    Text(
                        text = "Versão: ${game.version} • ${game.totalSizeBytes / (1024 * 1024)} MB",
                        color = OpenXTextSecondary,
                        fontSize = 12.sp
                    )

                    Row(
                        modifier = Modifier.padding(top = 4.dp),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        // Engine Tag
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(OpenXSurfaceVariant)
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = game.selectedEngineVersion,
                                color = OpenXRedLight,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        // Translation Tag
                        if (game.hasPortugueseTl) {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(Color(0xFF1E3A2F))
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = "PT-BR Ativo",
                                    color = OpenXSuccess,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }

            // Bottom Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = onPlay,
                    colors = ButtonDefaults.buttonColors(containerColor = OpenXRed),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.weight(1.2f).testTag("btn_play_card_${game.id}")
                ) {
                    Icon(Icons.Default.PlayArrow, null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Jogar", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                }

                OutlinedButton(
                    onClick = onTranslate,
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = OpenXTextPrimary),
                    border = androidx.compose.foundation.BorderStroke(1.dp, OpenXSurfaceBorder),
                    modifier = Modifier.weight(1f).testTag("btn_translate_card_${game.id}")
                ) {
                    Icon(Icons.Default.Translate, null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Traduzir", fontSize = 12.sp)
                }

                OutlinedButton(
                    onClick = onDetails,
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = OpenXTextPrimary),
                    border = androidx.compose.foundation.BorderStroke(1.dp, OpenXSurfaceBorder),
                    modifier = Modifier.weight(0.9f).testTag("btn_details_card_${game.id}")
                ) {
                    Text("Detalhes", fontSize = 12.sp)
                }
            }
        }
    }
}

@Composable
private fun StatPill(label: String, value: String) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(6.dp))
            .background(OpenXSurfaceVariant.copy(alpha = 0.6f))
            .padding(horizontal = 8.dp, vertical = 3.dp)
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(label, color = OpenXTextSecondary, fontSize = 11.sp)
            Text(value, color = OpenXTextPrimary, fontWeight = FontWeight.Bold, fontSize = 11.sp)
        }
    }
}
