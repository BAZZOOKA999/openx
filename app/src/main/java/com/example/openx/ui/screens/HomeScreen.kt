package com.example.openx.ui.screens

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
import androidx.compose.material.icons.filled.Android
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.FileOpen
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Translate
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.openx.data.db.entities.ProjectEntity
import com.example.openx.data.db.entities.ProjectType
import com.example.openx.ui.ScreenDestination
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
import com.example.openx.ui.theme.OpenXWarning

data class ProjectTypeUiInfo(val label: String, val color: Color)

@Composable
fun HomeScreen(
    projects: List<ProjectEntity>,
    activeProject: ProjectEntity?,
    onNavigate: (ScreenDestination) -> Unit,
    onSelectProject: (Long) -> Unit,
    onCreateDemo: () -> Unit,
    onImportFileUri: (Uri, String) -> Unit,
    onInspectApkUri: (Uri, String) -> Unit
) {
    // SAF File Pickers
    val filePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri != null) {
            val fileName = uri.lastPathSegment?.substringAfterLast("/") ?: "arquivo_importado"
            onImportFileUri(uri, fileName)
        }
    }

    val renpyPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri != null) {
            val fileName = uri.lastPathSegment?.substringAfterLast("/") ?: "script.rpy"
            onImportFileUri(uri, fileName)
        }
    }

    val apkPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri != null) {
            val fileName = uri.lastPathSegment?.substringAfterLast("/") ?: "app.apk"
            onInspectApkUri(uri, fileName)
            onNavigate(ScreenDestination.Files)
        }
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
            // Hero Banner
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(20.dp))
                    .border(1.dp, OpenXRed.copy(alpha = 0.35f), RoundedCornerShape(20.dp)),
                colors = CardDefaults.cardColors(containerColor = OpenXSurface)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(OpenXRedContainer.copy(alpha = 0.5f), OpenXSurface)
                            )
                        )
                        .padding(20.dp)
                ) {
                    Column {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(OpenXRed)
                                    .border(1.dp, OpenXRedLight, RoundedCornerShape(10.dp)),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "X",
                                    color = Color.White,
                                    fontWeight = FontWeight.Black,
                                    fontSize = 24.sp
                                )
                            }
                            Column {
                                Text(
                                    text = "OpenX Studio",
                                    style = MaterialTheme.typography.titleLarge.copy(
                                        fontWeight = FontWeight.Black,
                                        color = OpenXTextPrimary
                                    )
                                )
                                Text(
                                    text = "Motor de Tradução Ren'Py & Recursos Android",
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        color = OpenXTextSecondary
                                    )
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))
                        Text(
                            text = "Preservação estrita de variáveis, formatação, tags de visual novels e pacotes APK através de acesso SAF seguro e 100% offline.",
                            style = MaterialTheme.typography.bodyMedium.copy(
                                color = OpenXTextSecondary,
                                lineHeight = 20.sp
                            )
                        )
                    }
                }
            }
        }

        // Central de IA Banner
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .border(1.dp, OpenXRed.copy(alpha = 0.4f), RoundedCornerShape(14.dp))
                    .clickable { onNavigate(ScreenDestination.AiCenter) }
                    .testTag("card_open_ai_center"),
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
                            .background(OpenXRed),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.Star, null, tint = Color.White, modifier = Modifier.size(20.dp))
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Central de IA & Tradução Híbrida", fontWeight = FontWeight.Bold, color = OpenXTextPrimary, fontSize = 13.sp)
                        Text("Google ML Kit (zero custo), Memória de Tradução e Nuvem", color = OpenXTextSecondary, fontSize = 11.sp)
                    }
                    Icon(androidx.compose.material.icons.Icons.Default.ChevronRight, null, tint = OpenXRedLight)
                }
            }
        }

        // Quick Actions Grid (4 primary buttons required by user)
        item {
            Text(
                text = "Ações Rápidas",
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Bold,
                    color = OpenXTextPrimary
                )
            )
            Spacer(modifier = Modifier.height(10.dp))

            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    ActionTile(
                        modifier = Modifier.weight(1f),
                        title = "Importar Projeto",
                        subtitle = ".rpy, .xml, .json, .zip",
                        icon = Icons.Default.FileOpen,
                        tag = "btn_import_project",
                        onClick = {
                            filePicker.launch(arrayOf("*/*"))
                        }
                    )
                    ActionTile(
                        modifier = Modifier.weight(1f),
                        title = "Traduzir Ren'Py",
                        subtitle = "Scripts .rpy & tl/ templates",
                        icon = Icons.Default.Translate,
                        tag = "btn_translate_renpy",
                        onClick = {
                            renpyPicker.launch(arrayOf("*/*"))
                        }
                    )
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    ActionTile(
                        modifier = Modifier.weight(1f),
                        title = "Analisar APK",
                        subtitle = "Inspecionar contêiner ZIP",
                        icon = Icons.Default.Android,
                        tag = "btn_analyze_apk",
                        onClick = {
                            apkPicker.launch(arrayOf("application/vnd.android.package-archive", "*/*"))
                        }
                    )
                    ActionTile(
                        modifier = Modifier.weight(1f),
                        title = "Editor",
                        subtitle = "Continuar tradução",
                        icon = Icons.Default.PlayCircle,
                        tag = "btn_open_editor",
                        onClick = {
                            onNavigate(ScreenDestination.Translation)
                        }
                    )
                }
            }
        }

        // Active Project Overview Panel
        item {
            if (activeProject != null) {
                val progress = if (activeProject.totalUnits > 0) {
                    activeProject.translatedUnits.toFloat() / activeProject.totalUnits
                } else 0f
                val percent = (progress * 100).toInt()

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .border(1.dp, OpenXSurfaceBorder, RoundedCornerShape(16.dp))
                        .testTag("active_project_panel"),
                    colors = CardDefaults.cardColors(containerColor = OpenXSurface)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "PROJETO ATIVO",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        color = OpenXRedLight,
                                        fontWeight = FontWeight.Bold
                                    )
                                )
                                Text(
                                    text = activeProject.name,
                                    style = MaterialTheme.typography.titleMedium.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = OpenXTextPrimary
                                    ),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                            ProjectTypeBadge(activeProject.type)
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        // Progress Bar & Real Metric Numbers
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Progresso Geral",
                                style = MaterialTheme.typography.bodySmall.copy(color = OpenXTextSecondary)
                            )
                            Text(
                                text = "$percent% (${activeProject.translatedUnits}/${activeProject.totalUnits})",
                                style = MaterialTheme.typography.bodySmall.copy(
                                    color = OpenXTextPrimary,
                                    fontWeight = FontWeight.Bold
                                )
                            )
                        }

                        Spacer(modifier = Modifier.height(6.dp))
                        LinearProgressIndicator(
                            progress = { progress },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(8.dp)
                                .clip(RoundedCornerShape(4.dp)),
                            color = OpenXRed,
                            trackColor = OpenXSurfaceVariant
                        )

                        Spacer(modifier = Modifier.height(14.dp))

                        // Detailed counters
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceAround
                        ) {
                            MetricItem("Pendentes", (activeProject.totalUnits - activeProject.translatedUnits).coerceAtLeast(0), OpenXWarning)
                            MetricItem("Traduzidos", activeProject.translatedUnits, OpenXRedLight)
                            MetricItem("Revisados", activeProject.reviewedUnits, OpenXSuccess)
                            MetricItem("Erros", activeProject.errorUnits, OpenXError)
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        Button(
                            onClick = { onNavigate(ScreenDestination.Translation) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("btn_resume_translation"),
                            colors = ButtonDefaults.buttonColors(containerColor = OpenXRed),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Icon(imageVector = Icons.Default.PlayCircle, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Abrir Editor de Tradução")
                        }
                    }
                }
            } else {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = OpenXSurface)
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "Nenhum projeto ativo",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = OpenXTextPrimary
                            )
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Crie um projeto de demonstração com script Ren'Py completo para testar agora mesmo.",
                            style = MaterialTheme.typography.bodySmall.copy(color = OpenXTextSecondary)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Button(
                            onClick = onCreateDemo,
                            colors = ButtonDefaults.buttonColors(containerColor = OpenXRed)
                        ) {
                            Text("Carregar Demonstração Ren'Py")
                        }
                    }
                }
            }
        }

        // Recent Projects list
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Projetos Recentes (${projects.size})",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = OpenXTextPrimary
                    )
                )
                Text(
                    text = "Ver todos",
                    style = MaterialTheme.typography.labelMedium.copy(
                        color = OpenXRedLight,
                        fontWeight = FontWeight.SemiBold
                    ),
                    modifier = Modifier.clickable { onNavigate(ScreenDestination.Projects) }
                )
            }
        }

        items(projects.take(4)) { project ->
            ProjectCardItem(
                project = project,
                isSelected = project.id == activeProject?.id,
                onClick = {
                    onSelectProject(project.id)
                    onNavigate(ScreenDestination.Translation)
                }
            )
        }

        item {
            Spacer(modifier = Modifier.height(20.dp))
        }
    }
}

@Composable
fun ActionTile(
    modifier: Modifier = Modifier,
    title: String,
    subtitle: String,
    icon: ImageVector,
    tag: String,
    onClick: () -> Unit
) {
    Card(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .border(1.dp, OpenXSurfaceBorder, RoundedCornerShape(14.dp))
            .clickable { onClick() }
            .testTag(tag),
        colors = CardDefaults.cardColors(containerColor = OpenXSurface)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(OpenXRedContainer),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = title,
                    tint = OpenXRedLight,
                    modifier = Modifier.size(20.dp)
                )
            }
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontWeight = FontWeight.Bold,
                    color = OpenXTextPrimary
                )
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.labelSmall.copy(
                    color = OpenXTextSecondary
                ),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
fun ProjectCardItem(
    project: ProjectEntity,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val progress = if (project.totalUnits > 0) {
        (project.translatedUnits * 100) / project.totalUnits
    } else 0

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .border(
                1.dp,
                if (isSelected) OpenXRed else OpenXSurfaceBorder,
                RoundedCornerShape(12.dp)
            )
            .clickable { onClick() }
            .testTag("project_item_${project.id}"),
        colors = CardDefaults.cardColors(containerColor = OpenXSurface)
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(if (isSelected) OpenXRedContainer else OpenXSurfaceVariant),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = when (project.type) {
                        ProjectType.RENPY -> Icons.Default.Translate
                        ProjectType.ANDROID_RES -> Icons.Default.Description
                        ProjectType.APK_INSPECTION -> Icons.Default.Android
                        else -> Icons.Default.Folder
                    },
                    contentDescription = null,
                    tint = if (isSelected) OpenXRedLight else OpenXTextSecondary,
                    modifier = Modifier.size(22.dp)
                )
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = project.name,
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = OpenXTextPrimary
                    ),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "${project.totalUnits} textos • $progress% concluído • ${project.sourceLang} → ${project.targetLang}",
                    style = MaterialTheme.typography.labelSmall.copy(color = OpenXTextSecondary)
                )
            }

            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = "Abrir",
                tint = OpenXTextSecondary,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

@Composable
fun ProjectTypeBadge(type: ProjectType) {
    val info = when (type) {
        ProjectType.RENPY -> ProjectTypeUiInfo("Ren'Py", OpenXRedLight)
        ProjectType.ANDROID_RES -> ProjectTypeUiInfo("Android Res", OpenXSuccess)
        ProjectType.APK_INSPECTION -> ProjectTypeUiInfo("APK", OpenXWarning)
        ProjectType.GENERIC_TEXT -> ProjectTypeUiInfo("Texto", OpenXTextSecondary)
    }

    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(6.dp))
            .background(info.color.copy(alpha = 0.15f))
            .border(1.dp, info.color.copy(alpha = 0.4f), RoundedCornerShape(6.dp))
            .padding(horizontal = 6.dp, vertical = 2.dp)
    ) {
        Text(
            text = info.label,
            style = MaterialTheme.typography.labelSmall.copy(
                color = info.color,
                fontWeight = FontWeight.Bold,
                fontSize = 10.sp
            )
        )
    }
}

@Composable
fun MetricItem(label: String, count: Int, color: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = "$count",
            style = MaterialTheme.typography.titleMedium.copy(
                fontWeight = FontWeight.Bold,
                color = color
            )
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall.copy(color = OpenXTextSecondary)
        )
    }
}
