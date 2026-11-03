package com.example.openx.ui.screens

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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.WifiOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.openx.data.db.entities.UserSessionEntity
import com.example.openx.ui.theme.OpenXBackground
import com.example.openx.ui.theme.OpenXInfo
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
fun SettingsScreen(
    userSession: UserSessionEntity?,
    onToggleOffline: (Boolean) -> Unit,
    onUpdatePin: (String, Boolean) -> Unit,
    onNavigateToAiCenter: () -> Unit = {}
) {
    var showPinDialog by remember { mutableStateOf(false) }
    var pinInput by remember { mutableStateOf("") }
    var showRenpyGuideDialog by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(OpenXBackground)
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Configurações & Ajustes",
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Bold,
                    color = OpenXTextPrimary
                )
            )
            Text(
                text = "Segurança, conectividade, preferências de motor e integridade",
                style = MaterialTheme.typography.bodySmall.copy(color = OpenXTextSecondary)
            )
        }

        // Offline Mode Section
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .border(1.dp, OpenXSurfaceBorder, RoundedCornerShape(14.dp)),
                colors = CardDefaults.cardColors(containerColor = OpenXSurface)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(OpenXWarningContainer),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.WifiOff, null, tint = OpenXWarning)
                            }
                            Column {
                                Text(
                                    text = "Modo 100% Offline",
                                    fontWeight = FontWeight.Bold,
                                    color = OpenXTextPrimary
                                )
                                Text(
                                    text = if (userSession?.isOfflineMode != false) "Ativo: nenhum dado sai do seu aparelho" else "Conectividade de rede permitida",
                                    fontSize = 11.sp,
                                    color = OpenXTextSecondary
                                )
                            }
                        }

                        Switch(
                            checked = userSession?.isOfflineMode != false,
                            onCheckedChange = onToggleOffline,
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = OpenXRed,
                                checkedTrackColor = OpenXRedContainer,
                                uncheckedThumbColor = OpenXTextSecondary,
                                uncheckedTrackColor = OpenXSurfaceVariant
                            ),
                            modifier = Modifier.testTag("switch_offline_mode")
                        )
                    }
                }
            }
        }

        // Security & Local Authentication Section
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .border(1.dp, OpenXSurfaceBorder, RoundedCornerShape(14.dp)),
                colors = CardDefaults.cardColors(containerColor = OpenXSurface)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(OpenXRedContainer),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.Lock, null, tint = OpenXRedLight)
                            }
                            Column {
                                Text(
                                    text = "Bloqueio Seguro por PIN",
                                    fontWeight = FontWeight.Bold,
                                    color = OpenXTextPrimary
                                )
                                Text(
                                    text = if (userSession?.requirePinOnLaunch == true) "PIN local ativo para proteger traduções" else "Acesso livre sem solicitação de PIN",
                                    fontSize = 11.sp,
                                    color = OpenXTextSecondary
                                )
                            }
                        }

                        Switch(
                            checked = userSession?.requirePinOnLaunch == true,
                            onCheckedChange = { checked ->
                                if (checked) {
                                    showPinDialog = true
                                } else {
                                    onUpdatePin("", false)
                                }
                            },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = OpenXRed,
                                checkedTrackColor = OpenXRedContainer,
                                uncheckedThumbColor = OpenXTextSecondary,
                                uncheckedTrackColor = OpenXSurfaceVariant
                            ),
                            modifier = Modifier.testTag("switch_pin_security")
                        )
                    }

                    if (userSession?.requirePinOnLaunch == true) {
                        Spacer(modifier = Modifier.height(10.dp))
                        Button(
                            onClick = { showPinDialog = true },
                            colors = ButtonDefaults.buttonColors(containerColor = OpenXSurfaceVariant),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Alterar Código PIN", color = OpenXTextPrimary, fontSize = 12.sp)
                        }
                    }
                }
            }
        }

        // Translation Engine & AI Logic Transparent Information
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .border(1.dp, OpenXSurfaceBorder, RoundedCornerShape(14.dp)),
                colors = CardDefaults.cardColors(containerColor = OpenXSurface)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(OpenXRedContainer),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.AutoAwesome, null, tint = OpenXRedLight)
                        }
                        Column {
                            Text(
                                text = "Arquitetura do Motor de Tradução",
                                fontWeight = FontWeight.Bold,
                                color = OpenXTextPrimary
                            )
                            Text(
                                text = "Transparência total sobre cotas e integrações",
                                fontSize = 11.sp,
                                color = OpenXTextSecondary
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        text = "• Tradução Manual & Offline: Sempre funcional, gratuita e ilimitada através de banco de dados SQLite/Room local.\n• Assistência por IA: Compatível com Firebase AI Logic (Gemini API) e App Check. Chaves privadas nunca são embutidas diretamente no APK.\n• Aviso de Cotas: Provedores de nuvem possuem cotas de requisição por minuto e limites de token. O OpenX gerencia rate-limits com pausas automáticas.",
                        color = OpenXTextSecondary,
                        fontSize = 12.sp,
                        lineHeight = 18.sp
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Button(
                        onClick = onNavigateToAiCenter,
                        colors = ButtonDefaults.buttonColors(containerColor = OpenXRed),
                        modifier = Modifier.fillMaxWidth().testTag("btn_settings_open_ai_center")
                    ) {
                        Icon(Icons.Default.AutoAwesome, null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Abrir Central de IA & Provedores", fontSize = 12.sp)
                    }
                }
            }
        }

        // Ren'Py Syntax & Integrity Guide
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .border(1.dp, OpenXSurfaceBorder, RoundedCornerShape(14.dp)),
                colors = CardDefaults.cardColors(containerColor = OpenXSurface)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(OpenXSurfaceVariant),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.Code, null, tint = OpenXTextPrimary)
                            }
                            Column {
                                Text(
                                    text = "Sintaxe Ren'Py Suportada",
                                    fontWeight = FontWeight.Bold,
                                    color = OpenXTextPrimary
                                )
                                Text(
                                    text = "Variáveis, tags e regras de indentação",
                                    fontSize = 11.sp,
                                    color = OpenXTextSecondary
                                )
                            }
                        }

                        Button(
                            onClick = { showRenpyGuideDialog = true },
                            colors = ButtonDefaults.buttonColors(containerColor = OpenXSurfaceVariant),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text("Ver Guia", color = OpenXRedLight, fontSize = 12.sp)
                        }
                    }
                }
            }
        }

        // About OpenX
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .border(1.dp, OpenXSurfaceBorder, RoundedCornerShape(14.dp)),
                colors = CardDefaults.cardColors(containerColor = OpenXSurface)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Sobre o OpenX",
                        fontWeight = FontWeight.Bold,
                        color = OpenXTextPrimary
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Versão 1.0 (Build Nativo Kotlin/Compose)\nCompatível com Android 10 (API 29) até Android 17 (API 36).\nProjetado para preservação estrita de integridade de visual novels e jogos mobile.",
                        fontSize = 12.sp,
                        color = OpenXTextSecondary,
                        lineHeight = 18.sp
                    )
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(40.dp))
        }
    }

    // PIN Setup Dialog
    if (showPinDialog) {
        AlertDialog(
            onDismissRequest = { showPinDialog = false },
            containerColor = OpenXSurface,
            title = {
                Text("Definir PIN de Segurança", color = OpenXTextPrimary, fontWeight = FontWeight.Bold)
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        "Digite um PIN numérico (4 a 6 dígitos) para proteger suas sessões locais.",
                        color = OpenXTextSecondary,
                        fontSize = 12.sp
                    )
                    OutlinedTextField(
                        value = pinInput,
                        onValueChange = { if (it.length <= 6 && it.all { c -> c.isDigit() }) pinInput = it },
                        label = { Text("Código PIN") },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = OpenXRed,
                            unfocusedBorderColor = OpenXSurfaceBorder,
                            focusedTextColor = OpenXTextPrimary,
                            unfocusedTextColor = OpenXTextPrimary
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (pinInput.length >= 4) {
                            onUpdatePin(pinInput, true)
                            showPinDialog = false
                            pinInput = ""
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = OpenXRed)
                ) {
                    Text("Salvar PIN")
                }
            },
            dismissButton = {
                TextButton(onClick = { showPinDialog = false }) {
                    Text("Cancelar", color = OpenXTextSecondary)
                }
            }
        )
    }

    // Ren'Py Syntax Guide Dialog
    if (showRenpyGuideDialog) {
        AlertDialog(
            onDismissRequest = { showRenpyGuideDialog = false },
            containerColor = OpenXSurface,
            title = {
                Text("Guia de Sintaxe Ren'Py", color = OpenXTextPrimary, fontWeight = FontWeight.Bold)
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("1. Variáveis e Interpolação:", fontWeight = FontWeight.Bold, color = OpenXRedLight, fontSize = 13.sp)
                    Text("• [player_name] ou [points]: devem permanecer exatamente iguais na tradução.\n• %(name)s ou %1\$s: marcadores de formato também são protegidos pelo validador do OpenX.", fontSize = 12.sp, color = OpenXTextSecondary)

                    Text("2. Tags de Texto:", fontWeight = FontWeight.Bold, color = OpenXRedLight, fontSize = 13.sp)
                    Text("• {b}texto em negrito{/b}\n• {i}texto em itálico{/i}\n• {color=#f00}texto colorido{/color}\n• {w=1.0} pausa na fala\nO OpenX verifica o fechamento de todas as tags antes da exportação.", fontSize = 12.sp, color = OpenXTextSecondary)

                    Text("3. Comandos Ren'Py:", fontWeight = FontWeight.Bold, color = OpenXRedLight, fontSize = 13.sp)
                    Text("Comandos como 'scene', 'show', 'play music', 'jump' e código '$' nunca são traduzidos automaticamente para evitar corromper o script do jogo.", fontSize = 12.sp, color = OpenXTextSecondary)
                }
            },
            confirmButton = {
                Button(
                    onClick = { showRenpyGuideDialog = false },
                    colors = ButtonDefaults.buttonColors(containerColor = OpenXRed)
                ) {
                    Text("Entendido")
                }
            }
        )
    }
}
