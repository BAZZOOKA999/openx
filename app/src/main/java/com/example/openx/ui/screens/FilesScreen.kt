package com.example.openx.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Android
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.FolderZip
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Translate
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.openx.data.db.entities.FileItemEntity
import com.example.openx.data.db.entities.FileType
import com.example.openx.data.db.entities.ProjectEntity
import com.example.openx.engine.apk.ApkInspectionReport
import com.example.openx.ui.theme.OpenXBackground
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
fun FilesScreen(
    activeProject: ProjectEntity?,
    files: List<FileItemEntity>,
    apkReport: ApkInspectionReport?,
    isApkInspecting: Boolean,
    onInspectApk: (Uri, String) -> Unit,
    onImportInspectedApk: (Uri, String) -> Unit,
    onExportSingleFile: (Long, Uri) -> Unit
) {
    var selectedTab by remember { mutableStateOf(if (apkReport != null) 1 else 0) }
    var selectedApkUri by remember { mutableStateOf<Uri?>(null) }
    var selectedApkName by remember { mutableStateOf("aplicativo.apk") }

    var fileToExportId by remember { mutableStateOf<Long?>(null) }
    var fileToExportName by remember { mutableStateOf("") }

    // SAF Single file exporter
    val singleFileExportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("*/*")
    ) { uri: Uri? ->
        if (uri != null && fileToExportId != null) {
            onExportSingleFile(fileToExportId!!, uri)
            fileToExportId = null
        }
    }

    // SAF APK Picker
    val apkPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri != null) {
            val name = uri.lastPathSegment?.substringAfterLast("/") ?: "app.apk"
            selectedApkUri = uri
            selectedApkName = name
            selectedTab = 1
            onInspectApk(uri, name)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(OpenXBackground)
    ) {
        TabRow(
            selectedTabIndex = selectedTab,
            containerColor = OpenXSurface,
            contentColor = OpenXTextPrimary,
            indicator = { tabPositions ->
                TabRowDefaults.SecondaryIndicator(
                    modifier = Modifier.tabIndicatorOffset(tabPositions[selectedTab]),
                    color = OpenXRed
                )
            }
        ) {
            Tab(
                selected = selectedTab == 0,
                onClick = { selectedTab = 0 },
                text = { Text("Arquivos do Projeto (${files.size})", fontWeight = FontWeight.Bold) }
            )
            Tab(
                selected = selectedTab == 1,
                onClick = { selectedTab = 1 },
                text = { Text("Inspetor de APK", fontWeight = FontWeight.Bold) }
            )
        }

        if (selectedTab == 0) {
            // Project Files Tab
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                item {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Arquivos em '${activeProject?.name ?: "Projeto"}'",
                        style = MaterialTheme.typography.titleSmall.copy(
                            fontWeight = FontWeight.Bold,
                            color = OpenXTextPrimary
                        )
                    )
                    Text(
                        text = "Exporte cada arquivo individualmente ou o projeto completo no menu de projetos.",
                        style = MaterialTheme.typography.bodySmall.copy(color = OpenXTextSecondary)
                    )
                }

                if (files.isEmpty()) {
                    item {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 20.dp),
                            colors = CardDefaults.cardColors(containerColor = OpenXSurface)
                        ) {
                            Column(
                                modifier = Modifier.padding(24.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text("Nenhum arquivo no projeto ativo", color = OpenXTextPrimary)
                            }
                        }
                    }
                }

                items(files) { file ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .border(1.dp, OpenXSurfaceBorder, RoundedCornerShape(12.dp)),
                        colors = CardDefaults.cardColors(containerColor = OpenXSurface)
                    ) {
                        Row(
                            modifier = Modifier.padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(38.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (file.isBinary) OpenXSurfaceVariant else OpenXRedContainer),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = when (file.fileType) {
                                        FileType.RPY -> Icons.Default.Translate
                                        FileType.XML -> Icons.Default.Code
                                        FileType.JSON -> Icons.Default.Description
                                        FileType.APK, FileType.ZIP -> Icons.Default.FolderZip
                                        FileType.RPA, FileType.RPYC -> Icons.Default.Lock
                                        else -> Icons.Default.Description
                                    },
                                    contentDescription = null,
                                    tint = if (file.isBinary) OpenXTextSecondary else OpenXRedLight,
                                    modifier = Modifier.size(20.dp)
                                )
                            }

                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = file.fileName,
                                    style = MaterialTheme.typography.bodyMedium.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = OpenXTextPrimary
                                    )
                                )
                                Text(
                                    text = file.relativePath,
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        color = OpenXTextSecondary,
                                        fontSize = 11.sp
                                    ),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                if (file.notes.isNotBlank()) {
                                    Text(
                                        text = file.notes,
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            color = OpenXWarning,
                                            fontSize = 10.sp
                                        ),
                                        maxLines = 1
                                    )
                                }
                            }

                            if (!file.isBinary) {
                                IconButton(
                                    onClick = {
                                        fileToExportId = file.id
                                        fileToExportName = "traduzido_${file.fileName}"
                                        singleFileExportLauncher.launch(fileToExportName)
                                    },
                                    modifier = Modifier.testTag("btn_export_file_${file.id}")
                                ) {
                                    Icon(Icons.Default.Download, contentDescription = "Exportar", tint = OpenXTextPrimary)
                                }
                            }
                        }
                    }
                }

                item {
                    Spacer(modifier = Modifier.height(20.dp))
                }
            }
        } else {
            // APK Inspector Tab
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                item {
                    Spacer(modifier = Modifier.height(8.dp))
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
                                    Icon(Icons.Default.Android, null, tint = OpenXRedLight)
                                }
                                Column {
                                    Text(
                                        text = "Inspetor de Contêineres APK",
                                        fontWeight = FontWeight.Bold,
                                        color = OpenXTextPrimary
                                    )
                                    Text(
                                        text = "Auditoria e extração segura via Storage Access Framework",
                                        fontSize = 11.sp,
                                        color = OpenXTextSecondary
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(14.dp))

                            Button(
                                onClick = {
                                    apkPickerLauncher.launch(arrayOf("application/vnd.android.package-archive", "*/*"))
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = OpenXRed),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("btn_select_apk_for_inspection"),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Icon(Icons.Default.Android, null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Selecionar APK para Análise")
                            }
                        }
                    }
                }

                if (isApkInspecting) {
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = OpenXSurface)
                        ) {
                            Row(
                                modifier = Modifier.padding(20.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(14.dp)
                            ) {
                                CircularProgressIndicator(color = OpenXRed, modifier = Modifier.size(24.dp))
                                Column {
                                    Text("Analisando estrutura do APK...", fontWeight = FontWeight.Bold, color = OpenXTextPrimary)
                                    Text("Mapeando arquivos DEX, resources.arsc e ativos de engine...", color = OpenXTextSecondary, fontSize = 12.sp)
                                }
                            }
                        }
                    }
                }

                if (apkReport != null) {
                    item {
                        Text(
                            text = "Resultado da Auditoria: $selectedApkName",
                            fontWeight = FontWeight.Bold,
                            color = OpenXTextPrimary
                        )
                    }

                    // Security & Signature Advisory Box
                    item {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .border(1.dp, OpenXWarning.copy(alpha = 0.5f), RoundedCornerShape(12.dp)),
                            colors = CardDefaults.cardColors(containerColor = OpenXWarningContainer.copy(alpha = 0.4f))
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Icon(Icons.Default.Security, null, tint = OpenXWarning, modifier = Modifier.size(18.dp))
                                    Text("Aviso Crítico de Assinatura e Reconstrução", fontWeight = FontWeight.Bold, color = OpenXWarning, fontSize = 13.sp)
                                }
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = "1. Reconstruir um APK requer nova assinatura com apksigner. Um APK reassinado NUNCA poderá ser instalado como atualização sobre o APK original oficial sem desinstalar antes.\n2. Arquivos 'resources.arsc' e XMLs de layout são compilados pelo aapt2 em formato binário fechado. A edição destes formatos necessita da cadeia completa de ferramentas aapt2/apktool.\n3. O OpenX extrai e traduz todos os recursos textuais puros (scripts Ren'Py em assets/, arquivos JSON, configurações e textos UTF-8).",
                                    color = OpenXTextPrimary,
                                    fontSize = 11.sp,
                                    lineHeight = 16.sp
                                )
                            }
                        }
                    }

                    // Summary Stats
                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Card(
                                modifier = Modifier.weight(1f),
                                colors = CardDefaults.cardColors(containerColor = OpenXSurface)
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Text("Recursos Legíveis", fontSize = 11.sp, color = OpenXTextSecondary)
                                    Text("${apkReport.readableEntries.size}", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = OpenXSuccess)
                                }
                            }
                            Card(
                                modifier = Modifier.weight(1f),
                                colors = CardDefaults.cardColors(containerColor = OpenXSurface)
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Text("Ativos Ren'Py", fontSize = 11.sp, color = OpenXTextSecondary)
                                    Text("${apkReport.renpyEntries.size}", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = OpenXRedLight)
                                }
                            }
                            Card(
                                modifier = Modifier.weight(1f),
                                colors = CardDefaults.cardColors(containerColor = OpenXSurface)
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Text("Binários / DEX", fontSize = 11.sp, color = OpenXTextSecondary)
                                    Text("${apkReport.dexFilesCount} DEX", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = OpenXTextPrimary)
                                }
                            }
                        }
                    }

                    // Action to import readable content into project
                    if (apkReport.readableEntries.isNotEmpty() && selectedApkUri != null) {
                        item {
                            Button(
                                onClick = {
                                    onImportInspectedApk(selectedApkUri!!, selectedApkName)
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = OpenXSuccess),
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Icon(Icons.Default.Download, null)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Criar Projeto com os Arquivos Legíveis (${apkReport.readableEntries.size})")
                            }
                        }
                    }

                    // Detected RPA / RPYC Advisory if present
                    if (apkReport.renpyEntries.isNotEmpty()) {
                        item {
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .border(1.dp, OpenXRed.copy(alpha = 0.5f), RoundedCornerShape(12.dp)),
                                colors = CardDefaults.cardColors(containerColor = OpenXRedContainer.copy(alpha = 0.3f))
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                        Icon(Icons.Default.Info, null, tint = OpenXRedLight, modifier = Modifier.size(16.dp))
                                        Text("Arquivos Ren'Py Compilados (.rpa / .rpyc)", fontWeight = FontWeight.Bold, color = OpenXRedLight, fontSize = 12.sp)
                                    }
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        "Detectados arquivos .rpa e .rpyc. O OpenX identifica honestamente que estes formatos exigem descompactação e descompilação de bytecode Python (ex: Unrpyc). O OpenX não simula descompilações falsas.",
                                        color = OpenXTextSecondary,
                                        fontSize = 11.sp
                                    )
                                }
                            }
                        }
                    }

                    item {
                        Text("Lista de Entradas Mapeadas:", fontWeight = FontWeight.Bold, color = OpenXTextPrimary, fontSize = 13.sp)
                    }

                    items(apkReport.readableEntries + apkReport.binaryEntries.take(15)) { entry ->
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = OpenXSurface)
                        ) {
                            Row(
                                modifier = Modifier.padding(10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(4.dp))
                                        .background(if (entry.isReadableText) OpenXSuccessContainer else OpenXSurfaceVariant)
                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                ) {
                                    Text(
                                        text = entry.category,
                                        color = if (entry.isReadableText) OpenXSuccess else OpenXTextSecondary,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(entry.path, color = OpenXTextPrimary, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                    Text(entry.description, color = OpenXTextSecondary, fontSize = 10.sp, maxLines = 1)
                                }
                            }
                        }
                    }
                }

                item {
                    Spacer(modifier = Modifier.height(40.dp))
                }
            }
        }
    }
}
