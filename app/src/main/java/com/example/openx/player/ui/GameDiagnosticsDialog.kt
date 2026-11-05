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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Assessment
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
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
import com.example.openx.ui.theme.OpenXError
import com.example.openx.ui.theme.OpenXRed
import com.example.openx.ui.theme.OpenXRedLight
import com.example.openx.ui.theme.OpenXSuccess
import com.example.openx.ui.theme.OpenXSurface
import com.example.openx.ui.theme.OpenXSurfaceBorder
import com.example.openx.ui.theme.OpenXSurfaceVariant
import com.example.openx.ui.theme.OpenXTextPrimary
import com.example.openx.ui.theme.OpenXTextSecondary
import com.example.openx.ui.theme.OpenXWarning

@Composable
fun GameDiagnosticsDialog(
    game: RenpyGameEntity,
    onDismiss: () -> Unit
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
                    Icon(Icons.Default.Assessment, null, tint = Color.White, modifier = Modifier.size(20.dp))
                }
                Column {
                    Text(
                        text = "Diagnóstico & Compatibilidade",
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
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Score Banner
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = OpenXSurfaceVariant)
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(if (game.confidenceScore >= 80) OpenXSuccess else OpenXWarning),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "${game.confidenceScore}%",
                                color = Color.White,
                                fontWeight = FontWeight.Black,
                                fontSize = 14.sp
                            )
                        }
                        Column {
                            Text("Grau de Confiança do Motor", fontWeight = FontWeight.Bold, color = OpenXTextPrimary, fontSize = 13.sp)
                            Text(game.estimatedEngineVersion, color = OpenXRedLight, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                        }
                    }
                }

                // Compatibility Items List
                DiagnosticItem(
                    label = "Motor Selecionado",
                    value = game.selectedEngineVersion,
                    icon = Icons.Default.CheckCircle,
                    tint = OpenXSuccess
                )

                DiagnosticItem(
                    label = "Arquitetura Alvo",
                    value = "Android ARM64 / AArch64 Nativo (Sem emulação x86)",
                    icon = Icons.Default.CheckCircle,
                    tint = OpenXSuccess
                )

                DiagnosticItem(
                    label = "Binários Windows (.exe / .dll)",
                    value = "Descartados com segurança. Não executados no Android.",
                    icon = Icons.Default.Security,
                    tint = OpenXRedLight
                )

                DiagnosticItem(
                    label = "Tradução Português (game/tl/)",
                    value = if (game.hasPortugueseTl) "Detectada e pronta para execução" else "Pendente (clique em Traduzir com IA)",
                    icon = if (game.hasPortugueseTl) Icons.Default.CheckCircle else Icons.Default.Warning,
                    tint = if (game.hasPortugueseTl) OpenXSuccess else OpenXWarning
                )

                DiagnosticItem(
                    label = "Conformidade Google Play",
                    value = "Segurança estrita: sem download de .so/DEX externos não autorizados.",
                    icon = Icons.Default.Security,
                    tint = OpenXSuccess
                )

                if (game.compatibilityDetails.isNotBlank()) {
                    Text(
                        text = "Relatório Detalhado da Inspeção",
                        style = MaterialTheme.typography.labelMedium.copy(
                            color = OpenXTextPrimary,
                            fontWeight = FontWeight.Bold
                        )
                    )
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = OpenXSurface),
                        border = androidx.compose.foundation.BorderStroke(1.dp, OpenXSurfaceBorder)
                    ) {
                        Text(
                            text = game.compatibilityDetails,
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = OpenXTextSecondary,
                                lineHeight = 18.sp
                            ),
                            modifier = Modifier.padding(12.dp)
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onDismiss,
                colors = ButtonDefaults.buttonColors(containerColor = OpenXRed)
            ) {
                Text("Entendido", color = Color.White)
            }
        },
        containerColor = OpenXSurface
    )
}

@Composable
private fun DiagnosticItem(
    label: String,
    value: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    tint: Color
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(OpenXSurfaceVariant.copy(alpha = 0.5f))
            .padding(10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Icon(icon, null, tint = tint, modifier = Modifier.size(20.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(label, fontWeight = FontWeight.Bold, color = OpenXTextPrimary, fontSize = 12.sp)
            Text(value, color = OpenXTextSecondary, fontSize = 11.sp)
        }
    }
}
