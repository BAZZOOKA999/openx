package com.example.openx.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.HourglassEmpty
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Translate
import androidx.compose.material.icons.filled.WifiOff
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.openx.data.db.entities.TranslationStatus
import com.example.openx.ui.ScreenDestination
import com.example.openx.ui.theme.OpenXBackground
import com.example.openx.ui.theme.OpenXError
import com.example.openx.ui.theme.OpenXErrorContainer
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

data class StatusUiInfo(
    val label: String,
    val bg: Color,
    val fg: Color,
    val icon: ImageVector
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OpenXTopBar(
    title: String,
    projectName: String? = null,
    isOffline: Boolean = true,
    showBackButton: Boolean = false,
    onBackClick: () -> Unit = {},
    onAiCenterClick: () -> Unit = {}
) {
    TopAppBar(
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Stylized Red 'X' Logo badge
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(OpenXRedContainer)
                        .border(1.dp, OpenXRed, RoundedCornerShape(8.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "X",
                        color = OpenXRedLight,
                        fontWeight = FontWeight.Black,
                        fontSize = 18.sp
                    )
                }

                Column {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = OpenXTextPrimary
                        )
                    )
                    if (!projectName.isNullOrBlank()) {
                        Text(
                            text = projectName,
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = OpenXTextSecondary
                            ),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        },
        navigationIcon = {
            if (showBackButton) {
                IconButton(
                    onClick = onBackClick,
                    modifier = Modifier.testTag("top_bar_back_button")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Voltar",
                        tint = OpenXTextPrimary
                    )
                }
            }
        },
        actions = {
            // Central de IA quick button
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .background(OpenXRedContainer)
                    .border(1.dp, OpenXRed.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
                    .clickable { onAiCenterClick() }
                    .padding(horizontal = 8.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Star,
                    contentDescription = "Central de IA",
                    tint = OpenXRedLight,
                    modifier = Modifier.size(13.dp)
                )
                Text(
                    text = "IA",
                    style = MaterialTheme.typography.labelSmall.copy(
                        color = OpenXRedLight,
                        fontWeight = FontWeight.Bold
                    )
                )
            }

            Spacer(modifier = Modifier.width(6.dp))

            // Offline / Security Badge
            Row(
                modifier = Modifier
                    .padding(end = 12.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(OpenXSurfaceVariant)
                    .border(1.dp, OpenXSurfaceBorder, RoundedCornerShape(12.dp))
                    .padding(horizontal = 8.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Icon(
                    imageVector = if (isOffline) Icons.Default.WifiOff else Icons.Default.CheckCircle,
                    contentDescription = if (isOffline) "Modo Offline" else "Online",
                    tint = if (isOffline) OpenXWarning else OpenXSuccess,
                    modifier = Modifier.size(14.dp)
                )
                Text(
                    text = if (isOffline) "Offline" else "Online",
                    style = MaterialTheme.typography.labelSmall.copy(
                        color = OpenXTextPrimary,
                        fontWeight = FontWeight.SemiBold
                    )
                )
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = OpenXBackground
        )
    )
}

@Composable
fun OpenXBottomNavigationBar(
    currentScreen: ScreenDestination,
    onTabSelected: (ScreenDestination) -> Unit
) {
    NavigationBar(
        containerColor = OpenXSurface,
        contentColor = OpenXTextPrimary,
        tonalElevation = 8.dp,
        modifier = Modifier
            .navigationBarsPadding()
            .testTag("bottom_nav_bar")
    ) {
        val items = listOf(
            Triple(ScreenDestination.Home, "Início", Icons.Filled.Home),
            Triple(ScreenDestination.Projects, "Projetos", Icons.Filled.Folder),
            Triple(ScreenDestination.Translation, "Tradução", Icons.Filled.Translate),
            Triple(ScreenDestination.Files, "Arquivos", Icons.Filled.Description),
            Triple(ScreenDestination.Settings, "Ajustes", Icons.Filled.Settings)
        )

        items.forEach { (destination, label, icon) ->
            val isSelected = currentScreen == destination
            NavigationBarItem(
                selected = isSelected,
                onClick = { onTabSelected(destination) },
                icon = {
                    Icon(
                        imageVector = icon,
                        contentDescription = label,
                        modifier = Modifier.size(22.dp)
                    )
                },
                label = {
                    Text(
                        text = label,
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                        )
                    )
                },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = OpenXTextPrimary,
                    selectedTextColor = OpenXRedLight,
                    indicatorColor = OpenXRed,
                    unselectedIconColor = OpenXTextSecondary,
                    unselectedTextColor = OpenXTextSecondary
                ),
                modifier = Modifier.testTag("nav_tab_${destination.route}")
            )
        }
    }
}

@Composable
fun StatusChip(status: TranslationStatus) {
    val info = when (status) {
        TranslationStatus.PENDING -> StatusUiInfo("Pendente", OpenXSurfaceVariant, OpenXTextSecondary, Icons.Filled.HourglassEmpty)
        TranslationStatus.TRANSLATED -> StatusUiInfo("Traduzido", OpenXRedContainer, OpenXRedLight, Icons.Filled.PlayArrow)
        TranslationStatus.REVIEWED -> StatusUiInfo("Revisado", OpenXSuccessContainer, OpenXSuccess, Icons.Filled.CheckCircle)
        TranslationStatus.ERROR -> StatusUiInfo("Erro", OpenXErrorContainer, OpenXError, Icons.Filled.Error)
    }

    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(info.bg)
            .border(1.dp, info.fg.copy(alpha = 0.4f), RoundedCornerShape(8.dp))
            .padding(horizontal = 8.dp, vertical = 3.dp)
    ) {
        Icon(
            imageVector = info.icon,
            contentDescription = info.label,
            tint = info.fg,
            modifier = Modifier.size(12.dp)
        )
        Text(
            text = info.label,
            style = MaterialTheme.typography.labelSmall.copy(
                color = info.fg,
                fontWeight = FontWeight.Bold,
                fontSize = 11.sp
            )
        )
    }
}
