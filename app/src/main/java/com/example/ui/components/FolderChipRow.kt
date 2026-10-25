package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.FolderSpecial
import androidx.compose.material.icons.outlined.VideoLibrary
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.data.model.Folder
import com.example.ui.viewmodel.FolderFilter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FolderChipRow(
    folders: List<Folder>,
    currentFilter: FolderFilter,
    totalVideosCount: Int,
    favoritesCount: Int,
    onSelectFilter: (FolderFilter) -> Unit,
    onNewFolderClick: () -> Unit,
    onEditFolderClick: (Folder) -> Unit,
    modifier: Modifier = Modifier
) {
    val scrollState = rememberScrollState()

    Row(
        modifier = modifier
            .fillMaxWidth()
            .horizontalScroll(scrollState)
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // "All" Chip
        val isAllSelected = currentFilter is FolderFilter.All
        FilterChip(
            selected = isAllSelected,
            onClick = { onSelectFilter(FolderFilter.All) },
            label = {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("الكل")
                    BadgeCount(count = totalVideosCount, isSelected = isAllSelected)
                }
            },
            leadingIcon = {
                Icon(
                    imageVector = Icons.Outlined.VideoLibrary,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
            },
            modifier = Modifier.testTag("filter_all_chip")
        )

        // "Favorites" Chip
        val isFavSelected = currentFilter is FolderFilter.Favorites
        FilterChip(
            selected = isFavSelected,
            onClick = { onSelectFilter(FolderFilter.Favorites) },
            label = {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("المفضلة")
                    if (favoritesCount > 0) {
                        BadgeCount(count = favoritesCount, isSelected = isFavSelected)
                    }
                }
            },
            leadingIcon = {
                Icon(
                    imageVector = Icons.Filled.Star,
                    contentDescription = null,
                    tint = if (isFavSelected) MaterialTheme.colorScheme.onPrimaryContainer else Color(0xFFFFB300),
                    modifier = Modifier.size(18.dp)
                )
            },
            modifier = Modifier.testTag("filter_favorites_chip")
        )

        // User Folders
        folders.forEach { folder ->
            val isSelected = currentFilter is FolderFilter.ByFolder && currentFilter.folderId == folder.id
            val folderColor = try {
                Color(android.graphics.Color.parseColor(folder.colorHex))
            } catch (e: Exception) {
                MaterialTheme.colorScheme.primary
            }

            FilterChip(
                selected = isSelected,
                onClick = { onSelectFilter(FolderFilter.ByFolder(folder.id)) },
                label = {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(folder.name)
                    }
                },
                leadingIcon = {
                    Box(
                        modifier = Modifier
                            .size(12.dp)
                            .clip(CircleShape)
                            .background(folderColor)
                    )
                },
                trailingIcon = {
                    Box(
                        modifier = Modifier
                            .clip(CircleShape)
                            .clickable { onEditFolderClick(folder) }
                            .padding(2.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Folder,
                            contentDescription = "خيارات المجلد",
                            modifier = Modifier.size(16.dp),
                            tint = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.outline
                        )
                    }
                },
                modifier = Modifier.testTag("filter_folder_${folder.id}")
            )
        }

        // "Uncategorized" Chip
        val isUncategorizedSelected = currentFilter is FolderFilter.Uncategorized
        FilterChip(
            selected = isUncategorizedSelected,
            onClick = { onSelectFilter(FolderFilter.Uncategorized) },
            label = { Text("بدون مجلد") },
            leadingIcon = {
                Icon(
                    imageVector = Icons.Outlined.FolderSpecial,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
            },
            modifier = Modifier.testTag("filter_uncategorized_chip")
        )

        // "+ New Folder" Action Chip
        AssistChip(
            onClick = onNewFolderClick,
            label = { Text("مجلد جديد") },
            leadingIcon = {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
            },
            colors = AssistChipDefaults.assistChipColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
            ),
            modifier = Modifier.testTag("add_folder_chip")
        )
    }
}

@Composable
private fun BadgeCount(count: Int, isSelected: Boolean) {
    Surface(
        shape = RoundedCornerShape(10.dp),
        color = if (isSelected) {
            MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.2f)
        } else {
            MaterialTheme.colorScheme.surfaceVariant
        }
    ) {
        Text(
            text = "$count",
            style = MaterialTheme.typography.labelSmall,
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
            color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
