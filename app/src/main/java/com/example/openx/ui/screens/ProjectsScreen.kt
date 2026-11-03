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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Archive
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.openx.data.db.entities.ProjectEntity
import com.example.openx.ui.ScreenDestination
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
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun ProjectsScreen(
    projects: List<ProjectEntity>,
    activeProjectId: Long?,
    onSelectProject: (Long) -> Unit,
    onNavigate: (ScreenDestination) -> Unit,
    onCreateDemo: () -> Unit,
    onDeleteProject: (Long) -> Unit,
    onExportZip: (Uri) -> Unit,
    onImportFileUri: (Uri, String) -> Unit
) {
    var projectToDelete by remember { mutableStateOf<ProjectEntity?>(null) }

    // SAF Zip Exporter
    val zipExportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/zip")
    ) { uri: Uri? ->
        if (uri != null) {
            onExportZip(uri)
        }
    }

    // SAF File Import
    val fileImportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri != null) {
            val fileName = uri.lastPathSegment?.substringAfterLast("/") ?: "importado.rpy"
            onImportFileUri(uri, fileName)
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(OpenXBackground)
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Gerenciar Projetos",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = OpenXTextPrimary
                        )
                    )
                    OutlinedButton(
                        onClick = onCreateDemo,
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = OpenXRedLight),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text("+ Exemplo Ren'Py", fontSize = 12.sp)
                    }
                }
            }

            if (projects.isEmpty()) {
                item {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 40.dp),
                        colors = CardDefaults.cardColors(containerColor = OpenXSurface)
                    ) {
                        Column(
                            modifier = Modifier.padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                imageVector = Icons.Default.Folder,
                                contentDescription = null,
                                tint = OpenXTextSecondary,
                                modifier = Modifier.size(48.dp)
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = "Nenhum projeto cadastrado",
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = OpenXTextPrimary
                                )
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "Importe um script Ren'Py, arquivo XML de recursos ou crie um projeto demo.",
                                style = MaterialTheme.typography.bodySmall.copy(color = OpenXTextSecondary)
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Button(
                                onClick = onCreateDemo,
                                colors = ButtonDefaults.buttonColors(containerColor = OpenXRed)
                            ) {
                                Text("Carregar Demonstração")
                            }
                        }
                    }
                }
            }

            items(projects) { project ->
                val isSelected = project.id == activeProjectId
                val progress = if (project.totalUnits > 0) {
                    project.translatedUnits.toFloat() / project.totalUnits
                } else 0f
                val percent = (progress * 100).toInt()
                val dateFormat = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
                val formattedDate = dateFormat.format(Date(project.lastModified))

                var menuExpanded by remember { mutableStateOf(false) }

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .border(
                            1.dp,
                            if (isSelected) OpenXRed else OpenXSurfaceBorder,
                            RoundedCornerShape(14.dp)
                        )
                        .clickable { onSelectProject(project.id) }
                        .testTag("project_manage_card_${project.id}"),
                    colors = CardDefaults.cardColors(
                        containerColor = if (isSelected) OpenXSurfaceVariant else OpenXSurface
                    )
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                ProjectTypeBadge(project.type)
                                Text(
                                    text = project.name,
                                    style = MaterialTheme.typography.titleMedium.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = OpenXTextPrimary
                                    ),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }

                            Box {
                                IconButton(onClick = { menuExpanded = true }) {
                                    Icon(
                                        imageVector = Icons.Default.MoreVert,
                                        contentDescription = "Mais opções",
                                        tint = OpenXTextSecondary
                                    )
                                }
                                DropdownMenu(
                                    expanded = menuExpanded,
                                    onDismissRequest = { menuExpanded = false },
                                    modifier = Modifier.background(OpenXSurface)
                                ) {
                                    DropdownMenuItem(
                                        text = { Text("Abrir no Editor", color = OpenXTextPrimary) },
                                        leadingIcon = { Icon(Icons.Default.PlayCircle, null, tint = OpenXRedLight) },
                                        onClick = {
                                            menuExpanded = false
                                            onSelectProject(project.id)
                                            onNavigate(ScreenDestination.Translation)
                                        }
                                    )
                                    DropdownMenuItem(
                                        text = { Text("Exportar ZIP (SAF)", color = OpenXTextPrimary) },
                                        leadingIcon = { Icon(Icons.Default.Archive, null, tint = OpenXSuccess) },
                                        onClick = {
                                            menuExpanded = false
                                            onSelectProject(project.id)
                                            val sanitized = project.name.replace(Regex("[^a-zA-Z0-9_]"), "_")
                                            zipExportLauncher.launch("openx_${sanitized}.zip")
                                        }
                                    )
                                    DropdownMenuItem(
                                        text = { Text("Excluir Projeto", color = OpenXError) },
                                        leadingIcon = { Icon(Icons.Default.Delete, null, tint = OpenXError) },
                                        onClick = {
                                            menuExpanded = false
                                            projectToDelete = project
                                        }
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "${project.sourceLang} → ${project.targetLang} • $formattedDate",
                                style = MaterialTheme.typography.bodySmall.copy(color = OpenXTextSecondary)
                            )
                            Text(
                                text = "$percent% (${project.translatedUnits} / ${project.totalUnits})",
                                style = MaterialTheme.typography.bodySmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = if (percent == 100) OpenXSuccess else OpenXTextPrimary
                                )
                            )
                        }

                        Spacer(modifier = Modifier.height(6.dp))
                        LinearProgressIndicator(
                            progress = { progress },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(6.dp)
                                .clip(RoundedCornerShape(3.dp)),
                            color = if (percent == 100) OpenXSuccess else OpenXRed,
                            trackColor = OpenXSurface
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.End,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            if (isSelected) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                                    modifier = Modifier.padding(end = 8.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.CheckCircle,
                                        contentDescription = null,
                                        tint = OpenXRedLight,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Text(
                                        text = "Projeto Ativo",
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            color = OpenXRedLight,
                                            fontWeight = FontWeight.Bold
                                        )
                                    )
                                }
                            }

                            Button(
                                onClick = {
                                    onSelectProject(project.id)
                                    onNavigate(ScreenDestination.Translation)
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = OpenXRed),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.testTag("btn_open_project_${project.id}")
                            ) {
                                Text("Abrir Editor", fontSize = 12.sp)
                            }
                        }
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(80.dp))
            }
        }

        // FAB to import file
        FloatingActionButton(
            onClick = { fileImportLauncher.launch(arrayOf("*/*")) },
            containerColor = OpenXRed,
            contentColor = OpenXTextPrimary,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(bottom = 20.dp, end = 20.dp)
                .testTag("fab_import_project")
        ) {
            Icon(imageVector = Icons.Default.Add, contentDescription = "Importar Arquivo")
        }
    }

    // Delete confirmation dialog
    if (projectToDelete != null) {
        val proj = projectToDelete!!
        AlertDialog(
            onDismissRequest = { projectToDelete = null },
            containerColor = OpenXSurface,
            title = {
                Text("Excluir Projeto?", color = OpenXTextPrimary, fontWeight = FontWeight.Bold)
            },
            text = {
                Text(
                    "Tem certeza que deseja excluir o projeto '${proj.name}'? Todos os textos e arquivos locais deste projeto serão apagados permanentemente.",
                    color = OpenXTextSecondary
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        onDeleteProject(proj.id)
                        projectToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = OpenXError)
                ) {
                    Text("Excluir")
                }
            },
            dismissButton = {
                TextButton(onClick = { projectToDelete = null }) {
                    Text("Cancelar", color = OpenXTextSecondary)
                }
            }
        )
    }
}
