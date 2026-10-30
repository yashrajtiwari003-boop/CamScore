package com.fischerlabs.camscore.ui.components

import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import com.fischerlabs.camscore.ui.navigation.BottomNavItem
import com.fischerlabs.camscore.ui.theme.CamScoreTheme

@Composable
fun BottomNavBar(
    selectedItem: BottomNavItem,
    onItemSelected: (BottomNavItem) -> Unit,
    hasNewResults: Boolean = false
) {
    val colors = CamScoreTheme.colors

    NavigationBar(
        containerColor = MaterialTheme.colorScheme.surface,
        contentColor = colors.textPrimary,
        tonalElevation = 2.dp
    ) {
        BottomNavItem.entries.forEach { item ->
            val isSelected = item == selectedItem
            val showBadge = item == BottomNavItem.HISTORY && hasNewResults

            NavigationBarItem(
                selected = isSelected,
                onClick = { onItemSelected(item) },
                icon = {
                    if (showBadge) {
                        BadgedBox(
                            badge = {
                                Badge(
                                    containerColor = colors.accentBlue
                                )
                            }
                        ) {
                            Icon(
                                imageVector = if (isSelected) item.selectedIcon else item.unselectedIcon,
                                contentDescription = item.label
                            )
                        }
                    } else {
                        Icon(
                            imageVector = if (isSelected) item.selectedIcon else item.unselectedIcon,
                            contentDescription = item.label
                        )
                    }
                },
                label = {
                    Text(
                        text = item.label,
                        style = MaterialTheme.typography.labelSmall
                    )
                },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = MaterialTheme.colorScheme.primary,
                    selectedTextColor = MaterialTheme.colorScheme.primary,
                    unselectedIconColor = colors.textSecondary,
                    unselectedTextColor = colors.textSecondary,
                    indicatorColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
                )
            )
        }
    }
}
