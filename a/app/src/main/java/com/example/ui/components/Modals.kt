package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CreateNewFolder
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.Photo
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.model.MediaItem
import com.example.model.MediaType
import com.example.ui.theme.DominaCard
import com.example.ui.theme.DominaCardInner
import com.example.ui.theme.DominaElevatedSurface
import com.example.ui.theme.LocalDominaPalette
import com.example.ui.theme.Purple100
import com.example.ui.theme.Purple200
import com.example.ui.theme.Purple300
import com.example.ui.theme.Purple400
import com.example.ui.theme.Purple50
import com.example.ui.theme.Purple700
import com.example.ui.theme.Purple800
import com.example.ui.theme.Purple900
import com.example.utils.MediaLogic
import java.io.File

@Composable
fun QuickSearchModal(
    isOpen: Boolean,
    onClose: () -> Unit,
    items: List<MediaItem>,
    onSelectItem: (MediaItem) -> Unit,
    onRate: (String, Int) -> Unit,
    onOpenAdvancedSearch: (() -> Unit)? = null
) {
    if (!isOpen) return

    val palette = LocalDominaPalette.current
    var query by remember { mutableStateOf("") }

    val matchingItems = remember(items, query) {
        items.filter { MediaLogic.matchesSearchQuery(it, query) }.take(12)
    }

    Dialog(
        onDismissRequest = onClose,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth(0.94f)
                .clip(RoundedCornerShape(16.dp))
                .background(DominaCard)
                .border(1.dp, Purple800.copy(alpha = 0.7f), RoundedCornerShape(16.dp))
        ) {
            // Search Input Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Icon(
                    imageVector = Icons.Filled.Search,
                    contentDescription = null,
                    tint = Purple400,
                    modifier = Modifier.size(18.dp)
                )

                Box(modifier = Modifier.weight(1f)) {
                    if (query.isEmpty()) {
                        Text(
                            text = "Search all Images, Videos, Comics, Image Sets, or #tags...",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Purple400.copy(alpha = 0.5f)
                        )
                    }
                    BasicTextField(
                        value = query,
                        onValueChange = { query = it },
                        singleLine = true,
                        textStyle = MaterialTheme.typography.bodyLarge.copy(color = Purple50),
                        cursorBrush = SolidColor(palette.accentBright),
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                if (onOpenAdvancedSearch != null) {
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color(0xFF1D0F36))
                            .border(1.dp, Purple700.copy(alpha = 0.6f), RoundedCornerShape(8.dp))
                            .clickable {
                                onClose()
                                onOpenAdvancedSearch()
                            }
                            .padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(5.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Tune,
                            contentDescription = null,
                            tint = Purple400,
                            modifier = Modifier.size(13.dp)
                        )
                        Text(
                            text = "Advanced",
                            style = MaterialTheme.typography.bodySmall,
                            color = Purple100
                        )
                    }
                }

                Icon(
                    imageVector = Icons.Filled.Close,
                    contentDescription = "Close quick search",
                    tint = Purple300,
                    modifier = Modifier
                        .size(18.dp)
                        .clickable { onClose() }
                )
            }

            HorizontalDivider(color = Purple900.copy(alpha = 0.6f))

            if (matchingItems.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 36.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "No vault media items match \"$query\".",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Purple300.copy(alpha = 0.7f)
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 380.dp)
                ) {
                    items(matchingItems, key = { it.id }) { item ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    onSelectItem(item)
                                    onClose()
                                }
                                .padding(horizontal = 16.dp, vertical = 12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                modifier = Modifier.weight(1f),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(DominaCardInner)
                                        .border(1.dp, Purple900.copy(alpha = 0.6f), RoundedCornerShape(8.dp)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    val icon = when (item.type) {
                                        MediaType.IMAGE -> Icons.Filled.Photo
                                        MediaType.VIDEO -> Icons.Filled.Movie
                                        MediaType.COMIC -> Icons.Filled.MenuBook
                                        MediaType.IMAGESET -> Icons.Filled.PhotoLibrary
                                    }
                                    Icon(
                                        imageVector = icon,
                                        contentDescription = null,
                                        tint = Purple400,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }

                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = item.title,
                                        style = MaterialTheme.typography.titleSmall,
                                        color = Purple50,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    val tagStr = item.tags.take(4).joinToString(" • ") { "#$it" }
                                    Text(
                                        text = "${item.type.key.uppercase()} • $tagStr",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = Purple300.copy(alpha = 0.75f),
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.width(8.dp))
                            StarRating(
                                rating = item.rating,
                                onChange = { r -> onRate(item.id, r) },
                                size = StarRatingSize.SM,
                                itemTitle = item.title
                            )
                        }
                        HorizontalDivider(color = Purple900.copy(alpha = 0.35f))
                    }
                }
            }
        }
    }
}

@Composable
fun DirectoryPickerModal(
    isOpen: Boolean,
    title: String,
    initialPath: String,
    onClose: () -> Unit,
    onSelectPath: (String) -> Unit
) {
    if (!isOpen) return

    val palette = LocalDominaPalette.current
    var currentPath by remember(initialPath) { mutableStateOf(initialPath) }
    var newFolderName by remember { mutableStateOf("") }
    var refreshTrigger by remember { mutableStateOf(0) }

    val dirFile = remember(currentPath, refreshTrigger) {
        val f = File(currentPath)
        if (!f.exists()) f.mkdirs()
        f
    }

    val subdirectories = remember(dirFile, refreshTrigger) {
        dirFile.listFiles()
            ?.filter { it.isDirectory && !it.name.startsWith(".") }
            ?.sortedBy { it.name.lowercase() }
            ?: emptyList()
    }

    Dialog(
        onDismissRequest = onClose,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth(0.94f)
                .clip(RoundedCornerShape(16.dp))
                .background(DominaCard)
                .border(1.dp, Purple800.copy(alpha = 0.7f), RoundedCornerShape(16.dp))
        ) {
            // Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 18.dp, vertical = 14.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.Storage,
                        contentDescription = null,
                        tint = Purple400,
                        modifier = Modifier.size(18.dp)
                    )
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleMedium,
                        color = Purple50
                    )
                }
                Icon(
                    imageVector = Icons.Filled.Close,
                    contentDescription = "Close directory browser",
                    tint = Purple300,
                    modifier = Modifier
                        .size(18.dp)
                        .clickable { onClose() }
                )
            }

            HorizontalDivider(color = Purple900.copy(alpha = 0.6f))

            // Current Path Bar
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(DominaCardInner)
                    .padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(DominaElevatedSurface)
                            .border(1.dp, Purple800.copy(alpha = 0.6f), RoundedCornerShape(8.dp))
                            .clickable {
                                dirFile.parentFile?.let { parent ->
                                    currentPath = parent.absolutePath
                                }
                            }
                            .padding(horizontal = 10.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.ArrowUpward,
                            contentDescription = "Up",
                            tint = Purple100,
                            modifier = Modifier.size(14.dp)
                        )
                        Text("Up", style = MaterialTheme.typography.bodySmall, color = Purple100)
                    }

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(8.dp))
                            .background(DominaCard)
                            .border(1.dp, Purple900.copy(alpha = 0.6f), RoundedCornerShape(8.dp))
                            .padding(horizontal = 10.dp, vertical = 8.dp)
                    ) {
                        BasicTextField(
                            value = currentPath,
                            onValueChange = { currentPath = it },
                            singleLine = true,
                            textStyle = MaterialTheme.typography.labelMedium.copy(color = Purple50),
                            cursorBrush = SolidColor(palette.accentBright),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }

                Text(
                    text = "Subfolders: ${subdirectories.size}",
                    style = MaterialTheme.typography.labelSmall,
                    color = Purple300.copy(alpha = 0.75f)
                )
            }

            HorizontalDivider(color = Purple900.copy(alpha = 0.6f))

            // Subdirectory List
            if (subdirectories.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 32.dp, horizontal = 16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "No subdirectories inside this folder. You can select this directory or create a new subfolder below.",
                        style = MaterialTheme.typography.bodySmall,
                        color = Purple300.copy(alpha = 0.65f)
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 240.dp)
                        .padding(8.dp)
                ) {
                    items(subdirectories, key = { it.absolutePath }) { sub ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .clickable { currentPath = sub.absolutePath }
                                .padding(horizontal = 12.dp, vertical = 10.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Folder,
                                    contentDescription = null,
                                    tint = Purple400,
                                    modifier = Modifier.size(16.dp)
                                )
                                Text(
                                    text = sub.name,
                                    style = MaterialTheme.typography.labelMedium,
                                    color = Purple100
                                )
                            }
                            Text(
                                text = "Open →",
                                style = MaterialTheme.typography.labelSmall,
                                color = Purple400
                            )
                        }
                    }
                }
            }

            HorizontalDivider(color = Purple900.copy(alpha = 0.6f))

            // Footer: Create Subfolder + Use This Directory
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(DominaCardInner)
                    .padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(8.dp))
                            .background(DominaCard)
                            .border(1.dp, Purple900.copy(alpha = 0.6f), RoundedCornerShape(8.dp))
                            .padding(horizontal = 10.dp, vertical = 8.dp)
                    ) {
                        if (newFolderName.isEmpty()) {
                            Text(
                                text = "New folder name...",
                                style = MaterialTheme.typography.bodySmall,
                                color = Purple400.copy(alpha = 0.5f)
                            )
                        }
                        BasicTextField(
                            value = newFolderName,
                            onValueChange = { newFolderName = it },
                            singleLine = true,
                            textStyle = MaterialTheme.typography.bodySmall.copy(color = Purple100),
                            cursorBrush = SolidColor(palette.accentBright),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(DominaElevatedSurface)
                            .border(1.dp, Purple800.copy(alpha = 0.6f), RoundedCornerShape(8.dp))
                            .clickable {
                                val cleaned = newFolderName.trim()
                                if (cleaned.isNotEmpty()) {
                                    val created = File(dirFile, cleaned)
                                    created.mkdirs()
                                    newFolderName = ""
                                    currentPath = created.absolutePath
                                    refreshTrigger++
                                }
                            }
                            .padding(horizontal = 10.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(5.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.CreateNewFolder,
                            contentDescription = null,
                            tint = Purple400,
                            modifier = Modifier.size(14.dp)
                        )
                        Text(
                            text = "Create Folder",
                            style = MaterialTheme.typography.bodySmall,
                            color = Purple100
                        )
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Cancel",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Purple300,
                        modifier = Modifier
                            .clickable { onClose() }
                            .padding(horizontal = 12.dp, vertical = 8.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(palette.accent)
                            .clickable {
                                onSelectPath(currentPath)
                                onClose()
                            }
                            .padding(horizontal = 14.dp, vertical = 9.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Check,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(14.dp)
                        )
                        Text(
                            text = "Use This Directory",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = Color.White
                        )
                    }
                }
            }
        }
    }
}
