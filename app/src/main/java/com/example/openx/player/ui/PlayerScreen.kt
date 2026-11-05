package com.example.openx.player.ui

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FastForward
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Keyboard
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.Translate
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.openx.player.engine.RenpyChoice
import com.example.openx.player.engine.RenpyRuntimeBridge
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
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun PlayerScreen(
    runtimeBridge: RenpyRuntimeBridge,
    onExitPlayer: () -> Unit
) {
    val gameState by runtimeBridge.gameState.collectAsState()
    val scope = rememberCoroutineScope()

    var showHistoryDialog by remember { mutableStateOf(false) }
    var showKeyboardDialog by remember { mutableStateOf(false) }
    var showControlsCustomizer by remember { mutableStateOf(false) }
    var showPauseMenu by remember { mutableStateOf(false) }

    // On-screen overlay customization state
    var overlayOpacity by remember { mutableFloatStateOf(0.85f) }
    var isOverlayVisible by remember { mutableStateOf(true) }

    // Intercept back button to show pause menu
    BackHandler {
        showPauseMenu = true
    }

    // Auto dismiss quick save message after 3s
    LaunchedEffect(gameState.saveMessage) {
        if (gameState.saveMessage != null) {
            delay(3000)
            runtimeBridge.dismissSaveMessage()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .pointerInput(Unit) {
                detectTapGestures(
                    onTap = {
                        if (gameState.activeChoices.isEmpty()) {
                            runtimeBridge.nextStep()
                        }
                    }
                )
            }
    ) {
        // 1. SCENE BACKGROUND (Stylized ambient gradient representing game scene)
        val bgColors = when (gameState.sceneBackground.lowercase()) {
            "bg_mansion", "bg_entrance" -> listOf(Color(0xFF1E1014), Color(0xFF090608), Color(0xFF0F0A0E))
            "bg_secret_library", "bg_library" -> listOf(Color(0xFF0B1724), Color(0xFF070D14), Color(0xFF0A0F1A))
            "bg_salon" -> listOf(Color(0xFF1B1A0E), Color(0xFF0D0C07), Color(0xFF14130B))
            else -> listOf(Color(0xFF17171F), Color(0xFF09090D), Color(0xFF12121A))
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Brush.radialGradient(bgColors))
        )

        // 2. CHARACTER AVATAR / SPRITE SILHOUETTE (If speaking)
        if (gameState.characterSpeaker != null) {
            Box(
                modifier = Modifier
                    .align(Alignment.Center)
                    .size(260.dp)
                    .clip(CircleShape)
                    .background(Color(0x22E50914)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = gameState.characterSpeaker!!.take(1).uppercase(),
                    color = Color.White.copy(alpha = 0.2f),
                    fontSize = 120.sp,
                    fontWeight = FontWeight.Black
                )
            }
        }

        // 3. QUICK SAVE / MESSAGE TOAST
        AnimatedVisibility(
            visible = gameState.saveMessage != null,
            modifier = Modifier
                .align(Alignment.TopCenter)
                .statusBarsPadding()
                .padding(top = 16.dp)
        ) {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .background(OpenXSurface.copy(alpha = 0.95f))
                    .border(1.dp, OpenXRed, RoundedCornerShape(12.dp))
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                Text(
                    text = gameState.saveMessage ?: "",
                    color = OpenXTextPrimary,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp
                )
            }
        }

        // 4. TOP STATUS & TOGGLE BAR (Discreet overlay controls)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Live language indicator pill
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(if (gameState.isPortugueseActive) OpenXRedContainer else OpenXSurfaceVariant)
                    .border(1.dp, if (gameState.isPortugueseActive) OpenXRed else OpenXSurfaceBorder, RoundedCornerShape(8.dp))
                    .clickable { runtimeBridge.toggleLanguage() }
                    .padding(horizontal = 10.dp, vertical = 4.dp)
                    .testTag("btn_toggle_lang_live")
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    Icon(Icons.Default.Translate, null, tint = OpenXRedLight, modifier = Modifier.size(14.dp))
                    Text(
                        text = if (gameState.isPortugueseActive) "PT-BR (TL)" else "EN (Original)",
                        color = OpenXTextPrimary,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                // Controls Customizer (Transparency & Size)
                IconButton(
                    onClick = { showControlsCustomizer = true },
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(OpenXSurface.copy(alpha = 0.7f))
                ) {
                    Icon(Icons.Default.Tune, "Ajustar Controles", tint = OpenXTextPrimary, modifier = Modifier.size(18.dp))
                }

                // Overlay visibility toggle
                IconButton(
                    onClick = { isOverlayVisible = !isOverlayVisible },
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(OpenXSurface.copy(alpha = 0.7f))
                ) {
                    Icon(
                        imageVector = if (isOverlayVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                        contentDescription = "Ocultar Controles",
                        tint = OpenXTextPrimary,
                        modifier = Modifier.size(18.dp)
                    )
                }

                // ESC / Menu button
                IconButton(
                    onClick = { showPauseMenu = true },
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(OpenXRed)
                        .testTag("btn_esc_menu")
                ) {
                    Icon(Icons.Default.Close, "Pausa / Menu", tint = Color.White, modifier = Modifier.size(18.dp))
                }
            }
        }

        // 5. DIALOGUE BOX & CHOICES (Only when UI is not hidden)
        if (!gameState.isUiHidden) {
            Column(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Choices Menu (if active)
                if (gameState.activeChoices.isNotEmpty()) {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        gameState.activeChoices.forEach { choice ->
                            Button(
                                onClick = { runtimeBridge.selectChoice(choice) },
                                colors = ButtonDefaults.buttonColors(containerColor = OpenXSurface),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .border(1.dp, OpenXRed.copy(alpha = 0.8f), RoundedCornerShape(12.dp))
                                    .testTag("choice_btn_${choice.targetLabel}")
                            ) {
                                Text(
                                    text = choice.text,
                                    color = OpenXTextPrimary,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp,
                                    modifier = Modifier.padding(vertical = 4.dp)
                                )
                            }
                        }
                    }
                }

                // Dialogue Container Card
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(18.dp))
                        .border(1.dp, OpenXRed.copy(alpha = 0.4f), RoundedCornerShape(18.dp))
                        .testTag("dialogue_card"),
                    colors = CardDefaults.cardColors(containerColor = OpenXSurface.copy(alpha = 0.94f))
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(18.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        // Character Name Badge
                        if (gameState.characterSpeaker != null) {
                            val color = try {
                                Color(android.graphics.Color.parseColor(gameState.characterNameColor))
                            } catch (_: Exception) {
                                OpenXRedLight
                            }

                            Text(
                                text = gameState.characterSpeaker ?: "",
                                color = color,
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Black,
                                    fontSize = 16.sp
                                )
                            )
                        }

                        // Dialogue Text
                        Text(
                            text = gameState.dialogueText,
                            style = MaterialTheme.typography.bodyLarge.copy(
                                color = OpenXTextPrimary,
                                fontSize = 16.sp,
                                lineHeight = 24.sp
                            )
                        )
                    }
                }

                // 6. FLOATING ON-SCREEN CONTROLS OVERLAY (Menu flutuante opcional)
                if (isOverlayVisible) {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .border(1.dp, OpenXSurfaceBorder, RoundedCornerShape(14.dp))
                            .testTag("floating_controls_bar"),
                        colors = CardDefaults.cardColors(
                            containerColor = OpenXSurfaceVariant.copy(alpha = overlayOpacity)
                        )
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 6.dp, vertical = 4.dp),
                            horizontalArrangement = Arrangement.SpaceAround,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            OverlayButton(
                                label = "ESC",
                                isActive = false,
                                onClick = { showPauseMenu = true }
                            )
                            OverlayButton(
                                label = "AUTO",
                                isActive = gameState.isAutoMode,
                                onClick = { runtimeBridge.toggleAutoMode() }
                            )
                            OverlayButton(
                                label = "SKIP",
                                isActive = gameState.isSkipMode,
                                onClick = { runtimeBridge.toggleSkipMode() }
                            )
                            OverlayButton(
                                label = "HIST.",
                                isActive = false,
                                onClick = { showHistoryDialog = true }
                            )
                            OverlayButton(
                                label = "OCULT.",
                                isActive = false,
                                onClick = { runtimeBridge.toggleUiVisibility() }
                            )
                            OverlayButton(
                                label = "TECL.",
                                isActive = false,
                                onClick = { showKeyboardDialog = true }
                            )
                            OverlayButton(
                                label = "QS",
                                isActive = false,
                                onClick = { scope.launch { runtimeBridge.quickSave() } }
                            )
                            OverlayButton(
                                label = "QL",
                                isActive = false,
                                onClick = { scope.launch { runtimeBridge.quickLoad() } }
                            )
                        }
                    }
                }
            }
        } else {
            // UI is hidden - discreet tap notice
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .navigationBarsPadding()
                    .padding(16.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color.Black.copy(alpha = 0.6f))
                    .clickable { runtimeBridge.toggleUiVisibility() }
                    .padding(horizontal = 14.dp, vertical = 6.dp)
            ) {
                Text(
                    text = "Toque para reexibir a interface",
                    color = OpenXTextSecondary,
                    fontSize = 12.sp
                )
            }
        }
    }

    // Modal: Pause / Main Menu
    if (showPauseMenu) {
        AlertDialog(
            onDismissRequest = { showPauseMenu = false },
            title = {
                Text("Menu do Jogo", color = OpenXTextPrimary, fontWeight = FontWeight.Black)
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Jogo: ${gameState.gameTitle}", color = OpenXRedLight, fontSize = 12.sp)
                    Button(
                        onClick = {
                            scope.launch { runtimeBridge.quickSave() }
                            showPauseMenu = false
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = OpenXRed),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Salvar Rápido (QuickSave)")
                    }
                    Button(
                        onClick = {
                            scope.launch { runtimeBridge.quickLoad() }
                            showPauseMenu = false
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = OpenXSurfaceVariant),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Carregar Rápido (QuickLoad)", color = OpenXTextPrimary)
                    }
                    Button(
                        onClick = {
                            runtimeBridge.toggleLanguage()
                            showPauseMenu = false
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = OpenXSurfaceVariant),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Alternar Idioma (PT-BR / Original)", color = OpenXTextPrimary)
                    }
                    OutlinedButton(
                        onClick = {
                            showPauseMenu = false
                            runtimeBridge.stopGame()
                            onExitPlayer()
                        },
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = OpenXError),
                        border = androidx.compose.foundation.BorderStroke(1.dp, OpenXError),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Sair para a Biblioteca")
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = { showPauseMenu = false },
                    colors = ButtonDefaults.buttonColors(containerColor = OpenXRed)
                ) {
                    Text("Continuar")
                }
            },
            containerColor = OpenXSurface
        )
    }

    // Modal: Dialogue History
    if (showHistoryDialog) {
        AlertDialog(
            onDismissRequest = { showHistoryDialog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(Icons.Default.History, null, tint = OpenXRedLight)
                    Text("Histórico de Diálogos", color = OpenXTextPrimary, fontWeight = FontWeight.Bold)
                }
            },
            text = {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(300.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(gameState.historyLog) { log ->
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(OpenXSurfaceVariant.copy(alpha = 0.5f))
                                .padding(8.dp)
                        ) {
                            if (log.character != null) {
                                Text(
                                    text = log.character,
                                    color = OpenXRedLight,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp
                                )
                            }
                            Text(
                                text = log.text,
                                color = OpenXTextPrimary,
                                fontSize = 13.sp
                            )
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = { showHistoryDialog = false },
                    colors = ButtonDefaults.buttonColors(containerColor = OpenXRed)
                ) {
                    Text("Fechar")
                }
            },
            containerColor = OpenXSurface
        )
    }

    // Modal: Virtual Keyboard
    if (showKeyboardDialog) {
        var keyboardInput by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showKeyboardDialog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(Icons.Default.Keyboard, null, tint = OpenXRedLight)
                    Text("Entrada de Texto Ren'Py", color = OpenXTextPrimary, fontWeight = FontWeight.Bold)
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Digite um valor ou nome para enviar ao script:", color = OpenXTextSecondary, fontSize = 12.sp)
                    OutlinedTextField(
                        value = keyboardInput,
                        onValueChange = { keyboardInput = it },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = { showKeyboardDialog = false },
                    colors = ButtonDefaults.buttonColors(containerColor = OpenXRed)
                ) {
                    Text("Enviar")
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { showKeyboardDialog = false }) {
                    Text("Cancelar")
                }
            },
            containerColor = OpenXSurface
        )
    }

    // Modal: Controls Customizer (Transparência / Tamanho)
    if (showControlsCustomizer) {
        AlertDialog(
            onDismissRequest = { showControlsCustomizer = false },
            title = {
                Text("Personalizar Controles Flutuantes", color = OpenXTextPrimary, fontWeight = FontWeight.Bold)
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        text = "Transparência do Menu: ${(overlayOpacity * 100).toInt()}%",
                        color = OpenXTextSecondary,
                        fontSize = 13.sp
                    )
                    Slider(
                        value = overlayOpacity,
                        onValueChange = { overlayOpacity = it },
                        valueRange = 0.2f..1.0f,
                        colors = SliderDefaults.colors(
                            thumbColor = OpenXRed,
                            activeTrackColor = OpenXRed,
                            inactiveTrackColor = OpenXSurfaceVariant
                        )
                    )
                    Text(
                        text = "Os botões flutuantes dão acesso direto a ESC, Auto, Skip, Histórico, Ocultar UI, Teclado e QuickSaves.",
                        color = OpenXTextSecondary,
                        fontSize = 11.sp
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = { showControlsCustomizer = false },
                    colors = ButtonDefaults.buttonColors(containerColor = OpenXRed)
                ) {
                    Text("Concluir")
                }
            },
            containerColor = OpenXSurface
        )
    }
}

@Composable
private fun OverlayButton(
    label: String,
    isActive: Boolean,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(6.dp))
            .background(if (isActive) OpenXRed else Color.Transparent)
            .clickable(onClick = onClick)
            .padding(horizontal = 6.dp, vertical = 6.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            color = if (isActive) Color.White else OpenXTextPrimary,
            fontWeight = FontWeight.Black,
            fontSize = 10.sp
        )
    }
}
