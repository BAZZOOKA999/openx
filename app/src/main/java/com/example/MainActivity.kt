package com.example

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.openx.ui.OpenXViewModel
import com.example.openx.ui.ScreenDestination
import com.example.openx.ui.components.OpenXBottomNavigationBar
import com.example.openx.ui.components.OpenXTopBar
import com.example.openx.ui.screens.AiCenterScreen
import com.example.openx.ui.screens.FilesScreen
import com.example.openx.ui.screens.HomeScreen
import com.example.openx.ui.screens.ProjectsScreen
import com.example.openx.ui.screens.SettingsScreen
import com.example.openx.ui.screens.TranslationScreen
import com.example.openx.ui.theme.OpenXBackground
import com.example.openx.ui.theme.OpenXError
import com.example.openx.ui.theme.OpenXRed
import com.example.openx.ui.theme.OpenXRedLight
import com.example.openx.ui.theme.OpenXSuccess
import com.example.openx.ui.theme.OpenXSurface
import com.example.openx.ui.theme.OpenXSurfaceBorder
import com.example.openx.ui.theme.OpenXSurfaceVariant
import com.example.openx.ui.theme.OpenXTextPrimary
import com.example.openx.ui.theme.OpenXTextSecondary
import com.example.openx.ui.theme.OpenXTheme
import com.example.openx.ui.theme.OpenXWarning
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            OpenXTheme {
                OpenXApp()
            }
        }
    }
}

@Composable
fun OpenXApp(viewModel: OpenXViewModel = viewModel()) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    val currentScreen by viewModel.currentScreen.collectAsState()
    val projects by viewModel.projects.collectAsState()
    val selectedProjectId by viewModel.selectedProjectId.collectAsState()
    val filterState by viewModel.filterState.collectAsState()
    val translationProgress by viewModel.translationProgress.collectAsState()
    val userSession by viewModel.userSession.collectAsState()
    val apkReport by viewModel.apkReport.collectAsState()
    val isApkInspecting by viewModel.isApkInspecting.collectAsState()
    val exportReport by viewModel.exportReport.collectAsState()
    val mlKitStatuses by viewModel.mlKitStatuses.collectAsState()
    val tmCount by viewModel.tmCount.collectAsState()
    val isTestingConnection by viewModel.isTestingConnection.collectAsState()
    val connectionTestResult by viewModel.connectionTestResult.collectAsState()

    val activeProject = remember(projects, selectedProjectId) {
        projects.find { it.id == selectedProjectId } ?: projects.firstOrNull()
    }

    // Units flow for active project
    val unitsFlow = remember(activeProject?.id, viewModel) {
        if (activeProject != null) {
            viewModel.repository.getUnitsForProject(activeProject.id)
        } else {
            kotlinx.coroutines.flow.flowOf(emptyList())
        }
    }
    val units by unitsFlow.collectAsState(initial = emptyList())

    // Files flow for active project
    val filesFlow = remember(activeProject?.id, viewModel) {
        if (activeProject != null) {
            viewModel.repository.getFilesForProject(activeProject.id)
        } else {
            kotlinx.coroutines.flow.flowOf(emptyList())
        }
    }
    val files by filesFlow.collectAsState(initial = emptyList())

    // Glossary flow for active project
    val glossaryFlow = remember(activeProject?.id, viewModel) {
        if (activeProject != null) {
            viewModel.repository.getGlossaryForProject(activeProject.id)
        } else {
            kotlinx.coroutines.flow.flowOf(emptyList())
        }
    }
    val glossary by glossaryFlow.collectAsState(initial = emptyList())

    // Toast and snackbar listener
    LaunchedEffect(viewModel) {
        viewModel.toastMessage.collectLatest { msg ->
            snackbarHostState.showSnackbar(msg)
        }
    }

    // Authentication & PIN lock handling
    var isUnlocked by remember { mutableStateOf(false) }
    var enteredPin by remember { mutableStateOf("") }
    var pinError by remember { mutableStateOf(false) }

    val requiresPin = userSession?.requirePinOnLaunch == true && userSession?.pinCodeHash?.isNotBlank() == true

    if (requiresPin && !isUnlocked) {
        // Secure Lock Overlay
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(OpenXBackground)
                .padding(24.dp),
            contentAlignment = Alignment.Center
        ) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .border(1.dp, OpenXRed.copy(alpha = 0.5f), RoundedCornerShape(16.dp)),
                colors = CardDefaults.cardColors(containerColor = OpenXSurface)
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(54.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(OpenXRed),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Lock,
                            contentDescription = null,
                            tint = OpenXTextPrimary,
                            modifier = Modifier.size(30.dp)
                        )
                    }

                    Text(
                        text = "OpenX Seguro",
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.Black,
                            color = OpenXTextPrimary
                        )
                    )

                    Text(
                        text = "Digite seu PIN de 4 dígitos para acessar o ambiente de tradução local.",
                        style = MaterialTheme.typography.bodySmall.copy(color = OpenXTextSecondary)
                    )

                    OutlinedTextField(
                        value = enteredPin,
                        onValueChange = {
                            if (it.length <= 6 && it.all { c -> c.isDigit() }) {
                                enteredPin = it
                                pinError = false
                            }
                        },
                        label = { Text("Código PIN") },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = OpenXRed,
                            unfocusedBorderColor = if (pinError) OpenXError else OpenXSurfaceBorder,
                            focusedTextColor = OpenXTextPrimary,
                            unfocusedTextColor = OpenXTextPrimary
                        ),
                        modifier = Modifier.fillMaxWidth().testTag("pin_unlock_input")
                    )

                    if (pinError) {
                        Text("PIN incorreto. Tente novamente.", color = OpenXError, fontSize = 12.sp)
                    }

                    Button(
                        onClick = {
                            if (enteredPin == userSession?.pinCodeHash) {
                                isUnlocked = true
                            } else {
                                pinError = true
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = OpenXRed),
                        modifier = Modifier.fillMaxWidth().testTag("btn_pin_unlock")
                    ) {
                        Text("Desbloquear")
                    }
                }
            }
        }
        return
    }

    // Back handler: pop sub-screens back to Home
    BackHandler(enabled = currentScreen != ScreenDestination.Home) {
        viewModel.navigateTo(ScreenDestination.Home)
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = OpenXBackground,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            val title = when (currentScreen) {
                ScreenDestination.Home -> "OpenX"
                ScreenDestination.Projects -> "Projetos"
                ScreenDestination.Translation -> "Editor de Tradução"
                ScreenDestination.Files -> "Arquivos & APK"
                ScreenDestination.Settings -> "Configurações"
                ScreenDestination.AiCenter -> "Central de IA"
            }
            OpenXTopBar(
                title = title,
                projectName = activeProject?.name,
                isOffline = userSession?.isOfflineMode != false,
                showBackButton = currentScreen != ScreenDestination.Home,
                onBackClick = { viewModel.navigateTo(ScreenDestination.Home) },
                onAiCenterClick = { viewModel.navigateTo(ScreenDestination.AiCenter) }
            )
        },
        bottomBar = {
            OpenXBottomNavigationBar(
                currentScreen = currentScreen,
                onTabSelected = { destination ->
                    viewModel.navigateTo(destination)
                }
            )
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            AnimatedContent(
                targetState = currentScreen,
                transitionSpec = { fadeIn() togetherWith fadeOut() },
                label = "ScreenTransition"
            ) { screen ->
                when (screen) {
                    ScreenDestination.Home -> {
                        HomeScreen(
                            projects = projects,
                            activeProject = activeProject,
                            onNavigate = { viewModel.navigateTo(it) },
                            onSelectProject = { viewModel.selectProject(it) },
                            onCreateDemo = { viewModel.createDemoProject() },
                            onImportFileUri = { uri, name -> viewModel.importTextFile(uri, name) },
                            onInspectApkUri = { uri, name -> viewModel.inspectApk(uri, name) }
                        )
                    }
                    ScreenDestination.Projects -> {
                        ProjectsScreen(
                            projects = projects,
                            activeProjectId = activeProject?.id,
                            onSelectProject = { viewModel.selectProject(it) },
                            onNavigate = { viewModel.navigateTo(it) },
                            onCreateDemo = { viewModel.createDemoProject() },
                            onDeleteProject = { viewModel.deleteProject(it) },
                            onExportZip = { viewModel.exportProjectZip(it) },
                            onImportFileUri = { uri, name -> viewModel.importTextFile(uri, name) }
                        )
                    }
                    ScreenDestination.Translation -> {
                        TranslationScreen(
                            activeProject = activeProject,
                            units = units,
                            glossary = glossary,
                            filterState = filterState,
                            progressState = translationProgress,
                            exportReport = exportReport,
                            onQueryChange = { viewModel.setFilterQuery(it) },
                            onStatusFilterChange = { viewModel.setStatusFilter(it) },
                            onUpdateTranslation = { id, text, st -> viewModel.updateTranslation(id, text, st) },
                            onSetStatus = { id, st -> viewModel.updateUnitStatus(id, st) },
                            onStartBatchTranslate = { style -> viewModel.startBatchTranslation(style) },
                            onPauseBatchTranslate = { viewModel.pauseBatchTranslation() },
                            onResumeBatchTranslate = { viewModel.resumeBatchTranslation() },
                            onCancelBatchTranslate = { viewModel.cancelBatchTranslation() },
                            onReplaceAll = { find, repl, case -> viewModel.replaceAll(find, repl, case) },
                            onPrepareExportReport = { viewModel.prepareExportReport() },
                            onDismissExportReport = { viewModel.clearExportReport() },
                            onAddGlossaryTerm = { src, tgt, dnt, cat, role, gen, rel, pers, speech, notes ->
                                viewModel.addGlossaryItem(src, tgt, dnt, cat, role, gen, rel, pers, speech, notes)
                            },
                            onDeleteGlossaryTerm = { id -> viewModel.deleteGlossaryItem(id) }
                        )
                    }
                    ScreenDestination.Files -> {
                        FilesScreen(
                            activeProject = activeProject,
                            files = files,
                            apkReport = apkReport,
                            isApkInspecting = isApkInspecting,
                            onInspectApk = { uri, name -> viewModel.inspectApk(uri, name) },
                            onImportInspectedApk = { uri, name -> viewModel.importInspectedApk(uri, name) },
                            onExportSingleFile = { id, uri -> viewModel.exportSingleFile(id, uri) }
                        )
                    }
                    ScreenDestination.Settings -> {
                        SettingsScreen(
                            userSession = userSession,
                            onToggleOffline = { viewModel.toggleOfflineMode(it) },
                            onUpdatePin = { pin, en -> viewModel.updatePinSecurity(pin, en) },
                            onNavigateToAiCenter = { viewModel.navigateTo(ScreenDestination.AiCenter) }
                        )
                    }
                    ScreenDestination.AiCenter -> {
                        AiCenterScreen(
                            userSession = userSession,
                            mlKitStatuses = mlKitStatuses,
                            tmCount = tmCount,
                            isTestingConnection = isTestingConnection,
                            connectionTestResult = connectionTestResult,
                            onSelectEngine = { id, name -> viewModel.selectEngine(id, name) },
                            onTestConnection = { id, key, model -> viewModel.testEngineConnection(id, key, model) },
                            onDownloadMlKitModel = { lang -> viewModel.downloadMlKitModel(lang) },
                            onDeleteMlKitModel = { lang -> viewModel.deleteMlKitModel(lang) },
                            onSaveApiKeys = { gemini, groq, openRouter -> viewModel.saveApiKeys(gemini, groq, openRouter) },
                            onClearTm = { viewModel.clearTranslationMemory() }
                        )
                    }
                }
            }
        }
    }
}
