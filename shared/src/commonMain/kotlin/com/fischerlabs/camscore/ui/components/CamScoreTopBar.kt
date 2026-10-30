package com.fischerlabs.camscore.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.fischerlabs.camscore.ui.theme.CamScoreTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CamScoreTopBar(
    title: String = "CamScore",
    showSubtitle: Boolean = true,
    isDarkMode: Boolean = false,
    onThemeToggle: (() -> Unit)? = null,
    onSettingsClick: (() -> Unit)? = null,
    navigationIcon: @Composable (() -> Unit)? = null
) {
    val colors = CamScoreTheme.colors

    CenterAlignedTopAppBar(
        title = {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.Bold
                    ),
                    color = colors.textPrimary
                )
                if (showSubtitle) {
                    Text(
                        text = "by Fischer Labs",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontSize = 10.sp,
                            letterSpacing = 1.sp
                        ),
                        color = colors.textSecondary
                    )
                }
            }
        },
        navigationIcon = {
            if (navigationIcon != null) {
                navigationIcon()
            }
        },
        actions = {
            if (onThemeToggle != null) {
                IconButton(onClick = onThemeToggle) {
                    Icon(
                        imageVector = if (isDarkMode) Icons.Filled.LightMode else Icons.Filled.DarkMode,
                        contentDescription = if (isDarkMode) "Switch to light mode" else "Switch to dark mode",
                        tint = colors.textSecondary
                    )
                }
            }
            if (onSettingsClick != null) {
                IconButton(onClick = onSettingsClick) {
                    Icon(
                        imageVector = Icons.Filled.Settings,
                        contentDescription = "Settings",
                        tint = colors.textSecondary
                    )
                }
            }
        },
        colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
            containerColor = MaterialTheme.colorScheme.background,
            titleContentColor = colors.textPrimary
        )
    )
}
