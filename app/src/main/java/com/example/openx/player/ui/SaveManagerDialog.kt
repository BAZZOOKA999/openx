package com.example.openx.player.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Archive
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.openx.player.data.RenpyGameEntity
import com.example.openx.player.engine.SaveSlotInfo
import com.example.openx.ui.theme.OpenXError
import com.example.openx.ui.theme.OpenXRed
import com.example.openx.ui.theme.OpenXRedLight
import com.example.openx.ui.theme.OpenXSuccess
import com.example.openx.ui.theme.OpenXSurface
import com.example.openx.ui.theme.OpenXSurfaceBorder
import com.example.openx.ui.theme.OpenXSurfaceVariant
import com.example.openx.ui.theme.OpenXTextPrimary
import com.example.openx.ui.theme.OpenXTextSecondary

@Composable
fun SaveManagerDialog(
    game: RenpyGameEntity,
    saveSlots: List<SaveSlotInfo>,
    onDismiss: () -> Unit,
    onBackupSaves: () -> Unit,
    onRestoreSaves: () -> Unit,
    onDeleteSave: (SaveSlotInfo) -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(OpenXRed),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.Save, null, tint = Color.White, modifier = Modifier.size(20.dp))
                }
                Column {
                    Text(
                        text = "Gerenciador de Saves",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = OpenXTextPrimary
                        )
                    )
                    Text(
                        text = game.title,
                        style = MaterialTheme.typography.bodySmall.copy(color = OpenXRedLight),
                        maxLines = 1
                    )
                }
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Info Banner
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = OpenXSurfaceVariant)
                ) {
                    Row(
                        modifier = Modifier.padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(Icons.Default.Info, null, tint = OpenXRedLight, modifier = Modifier.size(18.dp))
                        Text(
                            text = "Seus saves ficam isolados em game/saves/ e nunca são apagados ao traduzir scripts.",
                            style = MaterialTheme.typography.bodySmall.copy(color = OpenXTextSecondary)
                        )
                    }
                }

                // Action buttons: Backup & Restore
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = onBackupSaves,
                        colors = ButtonDefaults.buttonColors(containerColor = OpenXRed),
                        modifier = Modifier.weight(1f).testTag("btn_backup_saves")
                    ) {
                        Icon(Icons.Default.Archive, null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Backup ZIP", fontSize = 12.sp)
                    }

                    OutlinedButton(
                        onClick = onRestoreSaves,
                        modifier = Modifier.weight(1f).testTag("btn_restore_saves")
                    ) {
                        Icon(Icons.Default.Refresh, null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Restaurar", fontSize = 12.sp)
                    }
                }

                Text(
                    text = "Saves Existentes (${saveSlots.size})",
                    style = MaterialTheme.typography.labelMedium.copy(
                        color = OpenXTextPrimary,
                        fontWeight = FontWeight.Bold
                    )
                )

                if (saveSlots.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Nenhum save encontrado para este jogo.",
                            color = OpenXTextSecondary,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(200.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        items(saveSlots) { slot ->
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(containerColor = OpenXSurface),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(8.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = slot.slotName,
                                            fontWeight = FontWeight.Bold,
                                            color = OpenXTextPrimary,
                                            fontSize = 13.sp
                                        )
                                        Text(
                                            text = "${slot.formattedDate} • ${slot.sizeBytes / 1024} KB",
                                            color = OpenXTextSecondary,
                                            fontSize = 11.sp
                                        )
                                    }
                                    IconButton(
                                        onClick = { onDeleteSave(slot) },
                                        modifier = Modifier.size(32.dp)
                                    ) {
                                        Icon(Icons.Default.Delete, null, tint = OpenXError, modifier = Modifier.size(18.dp))
                                    }
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onDismiss,
                colors = ButtonDefaults.buttonColors(containerColor = OpenXSurfaceVariant)
            ) {
                Text("Fechar", color = OpenXTextPrimary)
            }
        },
        containerColor = OpenXSurface
    )
}
