package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CreateNewFolder
import androidx.compose.material.icons.filled.NoteAdd
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.model.MediaType
import com.example.ui.theme.DominaCard
import com.example.ui.theme.DominaCardInput
import com.example.ui.theme.DominaElevatedSurface
import com.example.ui.theme.LocalDominaPalette
import com.example.ui.theme.Purple100
import com.example.ui.theme.Purple200
import com.example.ui.theme.Purple300
import com.example.ui.theme.Purple400
import com.example.ui.theme.Purple50
import com.example.ui.theme.Purple800
import com.example.ui.theme.Purple900

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun FilterToolbar(
    mediaType: MediaType,
    totalCount: Int,
    filteredCount: Int,
    searchQuery: String,
    isUploading: Boolean = false,
    uploadStatusText: String? = null,
    onChangeSearch: (String) -> Unit,
    onOpenAdvancedSearch: (() -> Unit)? = null,
    onMountFolder: () -> Unit,
    onAddFiles: () -> Unit,
    modifier: Modifier = Modifier
) {
    val palette = LocalDominaPalette.current

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(DominaCard)
            .border(1.dp, Purple900.copy(alpha = 0.55f), RoundedCornerShape(12.dp))
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // Search Input Bar
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(8.dp))
                .background(DominaCardInput)
                .border(1.dp, Purple900.copy(alpha = 0.65f), RoundedCornerShape(8.dp))
                .padding(horizontal = 12.dp, vertical = 10.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Filled.Search,
                    contentDescription = null,
                    tint = palette.accentBright,
                    modifier = Modifier.size(16.dp)
                )
                Box(modifier = Modifier.weight(1f)) {
                    if (searchQuery.isEmpty()) {
                        Text(
                            text = "Search ${mediaType.key}s by title, filename, or #tag...",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Purple400.copy(alpha = 0.5f)
                        )
                    }
                    BasicTextField(
                        value = searchQuery,
                        onValueChange = onChangeSearch,
                        singleLine = true,
                        textStyle = MaterialTheme.typography.bodyMedium.copy(color = Purple50),
                        cursorBrush = SolidColor(palette.accentBright),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("gallery_search_input")
                    )
                }
                if (searchQuery.isNotEmpty()) {
                    Icon(
                        imageVector = Icons.Filled.Close,
                        contentDescription = "Clear search query",
                        tint = Purple300,
                        modifier = Modifier
                            .size(16.dp)
                            .clickable { onChangeSearch("") }
                    )
                }
            }
        }

        // Actions & Counter Row
        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = "Showing $filteredCount of $totalCount",
                style = MaterialTheme.typography.labelMedium,
                color = Purple300.copy(alpha = 0.8f),
                modifier = Modifier.align(Alignment.CenterVertically)
            )

            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (onOpenAdvancedSearch != null) {
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(DominaElevatedSurface)
                            .border(1.dp, Purple800.copy(alpha = 0.6f), RoundedCornerShape(8.dp))
                            .clickable { onOpenAdvancedSearch() }
                            .padding(horizontal = 10.dp, vertical = 7.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Tune,
                            contentDescription = null,
                            tint = Purple400,
                            modifier = Modifier.size(14.dp)
                        )
                        Text(
                            text = "Advanced Search",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium,
                            color = Purple100
                        )
                    }
                }

                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(palette.accent)
                        .clickable(enabled = !isUploading) { onMountFolder() }
                        .padding(horizontal = 10.dp, vertical = 7.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.CreateNewFolder,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(14.dp)
                    )
                    Text(
                        text = "Mount Folder",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = Color.White
                    )
                }

                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color(0xFF180C2E))
                        .border(1.dp, Purple800.copy(alpha = 0.6f), RoundedCornerShape(8.dp))
                        .clickable(enabled = !isUploading) { onAddFiles() }
                        .padding(horizontal = 10.dp, vertical = 7.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.NoteAdd,
                        contentDescription = null,
                        tint = Purple400,
                        modifier = Modifier.size(14.dp)
                    )
                    Text(
                        text = "Add Files",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium,
                        color = Purple100
                    )
                }
            }
        }

        if (!uploadStatusText.isNullOrBlank()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(DominaCardInput)
                    .border(1.dp, palette.accentBright.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
                    .padding(horizontal = 12.dp, vertical = 8.dp)
            ) {
                Text(
                    text = uploadStatusText,
                    style = MaterialTheme.typography.labelMedium,
                    color = Purple100
                )
            }
        }
    }
}
