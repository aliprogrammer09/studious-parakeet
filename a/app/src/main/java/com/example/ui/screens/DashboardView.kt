package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.BookmarkAdded
import androidx.compose.material.icons.filled.CreateNewFolder
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.Photo
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.PowerSettingsNew
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.UploadFile
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.model.AppearanceConfig
import com.example.model.DirectoryConfig
import com.example.model.MediaItem
import com.example.model.MediaType
import com.example.model.PageView
import com.example.ui.components.MediaCard
import com.example.ui.components.rememberThumbModel
import com.example.ui.theme.DominaCard
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
import com.example.ui.theme.Purple950
import com.example.ui.theme.Rose700
import com.example.ui.theme.Rose950
import com.example.utils.MediaLogic

private data class HubCardData(
    val type: MediaType,
    val page: PageView,
    val title: String,
    val count: Int,
    val unit: String,
    val path: String,
    val icon: ImageVector,
    val iconTint: Color
)

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun DashboardView(
    items: List<MediaItem>,
    directories: DirectoryConfig,
    appearance: AppearanceConfig,
    columnsCount: Int,
    panicBlur: Boolean,
    uploadStatusText: String?,
    onNavigate: (PageView) -> Unit,
    onSelectItem: (MediaItem) -> Unit,
    onResumeReader: (MediaItem) -> Unit,
    onRate: (String, Int) -> Unit,
    onToggleFavorite: (String, Boolean) -> Unit,
    onQuickTagJump: (String) -> Unit,
    onOpenSearch: () -> Unit,
    onPanicClose: () -> Unit,
    onMountFolder: (MediaType) -> Unit,
    onAddFiles: (MediaType) -> Unit,
    modifier: Modifier = Modifier
) {
    val palette = LocalDominaPalette.current

    val imagesCount = remember(items) { items.count { it.type == MediaType.IMAGE } }
    val videosCount = remember(items) { items.count { it.type == MediaType.VIDEO } }
    val comicsCount = remember(items) { items.count { it.type == MediaType.COMIC } }
    val imageSetsCount = remember(items) { items.count { it.type == MediaType.IMAGESET } }
    val totalBytes = remember(items) { items.sumOf { it.sizeBytes } }

    // Highest-Rated Showcase: ONLY show rated items (rating > 0), never unrated items
    val highestRated = remember(items) {
        items.filter { it.rating > 0 }
            .sortedByDescending { it.rating }
            .take(8)
    }

    val favoritedItems = remember(items) {
        items.filter { it.isFavorite }.take(4)
    }

    // Smart Bookmarks: ONLY Comics with reading progress (Image Sets excluded)
    val inProgressComics = remember(items) {
        items.filter { item ->
            if (item.type != MediaType.COMIC) return@filter false
            val prog = MediaLogic.computeBookmarkProgress(
                item.lastReadPage,
                item.pageCount,
                item.readPages
            )
            prog.isStarted
        }
    }

    val hubCards = listOf(
        HubCardData(
            type = MediaType.IMAGE,
            page = PageView.IMAGES,
            title = "Images Gallery",
            count = imagesCount,
            unit = "Indexed Images",
            path = directories.imagesPath,
            icon = Icons.Filled.Photo,
            iconTint = Purple400
        ),
        HubCardData(
            type = MediaType.VIDEO,
            page = PageView.VIDEOS,
            title = "Videos Studio",
            count = videosCount,
            unit = "Video Streams",
            path = directories.videosPath,
            icon = Icons.Filled.Movie,
            iconTint = palette.accentBright
        ),
        HubCardData(
            type = MediaType.COMIC,
            page = PageView.COMICS,
            title = "Comics Library",
            count = comicsCount,
            unit = "Subfolder Volumes",
            path = directories.comicsPath,
            icon = Icons.Filled.MenuBook,
            iconTint = Purple400
        ),
        HubCardData(
            type = MediaType.IMAGESET,
            page = PageView.IMAGESETS,
            title = "Image Sets",
            count = imageSetsCount,
            unit = "Subfolder Sets",
            path = directories.imageSetsPath,
            icon = Icons.Filled.PhotoLibrary,
            iconTint = palette.accentBright
        )
    )

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 20.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        // Hero Banner & Workspace Toolbar
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(
                    Brush.linearGradient(
                        listOf(Color(0xFF230E40), Color(0xFF130924), Color(0xFF0A0514))
                    )
                )
                .border(1.dp, Purple800.copy(alpha = 0.6f), RoundedCornerShape(16.dp))
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.AutoAwesome,
                        contentDescription = null,
                        tint = Purple400,
                        modifier = Modifier.size(14.dp)
                    )
                    Text(
                        text = "OFFLINE LOCAL MEDIA SANCTUM",
                        style = MaterialTheme.typography.labelMedium,
                        color = Purple400
                    )
                }

                Text(
                    text = "Domina Media Archive & Immersive Reader",
                    style = MaterialTheme.typography.displayMedium,
                    color = Purple50
                )

                Text(
                    text = "Scan local directories on device or mount folders to organize, rate, filter, and view Images, Videos, multi-page Comics, and Image Sets.",
                    style = MaterialTheme.typography.bodyLarge,
                    color = Purple200.copy(alpha = 0.78f)
                )
            }

            // Workspace Quick Actions
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                ActionButtonChip(
                    icon = Icons.Filled.Tune,
                    iconTint = Purple400,
                    label = "Advanced Search",
                    onClick = { onNavigate(PageView.ADVANCED_SEARCH) },
                    testTag = "hero_advanced_search_btn"
                )
                ActionButtonChip(
                    icon = Icons.Filled.Search,
                    iconTint = palette.accentBright,
                    label = "Quick Search",
                    onClick = onOpenSearch,
                    testTag = "hero_quick_search_btn"
                )
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(Rose950.copy(alpha = 0.85f))
                        .border(1.dp, Rose700.copy(alpha = 0.65f), RoundedCornerShape(8.dp))
                        .testTag("hero_panic_close_btn")
                        .clickable { onPanicClose() }
                        .padding(horizontal = 12.dp, vertical = 9.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.PowerSettingsNew,
                        contentDescription = null,
                        tint = Color(0xFFFFE4E6),
                        modifier = Modifier.size(14.dp)
                    )
                    Text(
                        text = "Panic Close",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFFFFE4E6)
                    )
                }
                ActionButtonChip(
                    icon = Icons.Filled.Settings,
                    iconTint = Purple400,
                    label = "Settings",
                    onClick = { onNavigate(PageView.SETTINGS) },
                    testTag = "hero_settings_btn"
                )
            }

            // Vault Footprint
            Text(
                text = "Total Indexed: ${items.size} items  •  Rated: ${highestRated.size}  •  Footprint: ${
                    MediaLogic.formatFileSize(totalBytes)
                }",
                style = MaterialTheme.typography.labelMedium,
                color = Purple300.copy(alpha = 0.8f)
            )
        }

        if (!uploadStatusText.isNullOrBlank()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0xFF140A26))
                    .border(1.dp, palette.accentBright.copy(alpha = 0.6f), RoundedCornerShape(12.dp))
                    .padding(14.dp)
            ) {
                Text(
                    text = uploadStatusText,
                    style = MaterialTheme.typography.labelMedium,
                    color = Purple100
                )
            }
        }

        // Central Navigation Hub: 4 Media Category Pillars + Quick Folder Status
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            val rows = hubCards.chunked(2)
            rows.forEach { pair ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    pair.forEach { card ->
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(12.dp))
                                .background(DominaCard)
                                .border(1.dp, Purple900.copy(alpha = 0.55f), RoundedCornerShape(12.dp))
                                .padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("hub_card_${card.type.key}")
                                    .clickable { onNavigate(card.page) }
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Icon(
                                            imageVector = card.icon,
                                            contentDescription = null,
                                            tint = card.iconTint,
                                            modifier = Modifier.size(18.dp)
                                        )
                                        Text(
                                            text = card.title,
                                            style = MaterialTheme.typography.headlineMedium,
                                            color = Purple50,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                                        contentDescription = null,
                                        tint = Purple400.copy(alpha = 0.7f),
                                        modifier = Modifier.size(15.dp)
                                    )
                                }

                                Spacer(modifier = Modifier.height(10.dp))

                                Row(
                                    verticalAlignment = Alignment.Bottom,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Text(
                                        text = "${card.count}",
                                        style = MaterialTheme.typography.displaySmall,
                                        color = Purple50
                                    )
                                    Text(
                                        text = card.unit,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = Purple300.copy(alpha = 0.75f),
                                        modifier = Modifier.padding(bottom = 3.dp)
                                    )
                                }

                                Spacer(modifier = Modifier.height(8.dp))
                                HorizontalDivider(color = Purple900.copy(alpha = 0.4f))
                                Spacer(modifier = Modifier.height(6.dp))

                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(5.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.FolderOpen,
                                        contentDescription = null,
                                        tint = Purple400,
                                        modifier = Modifier.size(12.dp)
                                    )
                                    Text(
                                        text = card.path,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = Purple300.copy(alpha = 0.7f),
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }

                            // Mount Folder & Add Files buttons
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Row(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(Color(0xFF1C1033))
                                        .border(1.dp, Purple800.copy(alpha = 0.6f), RoundedCornerShape(8.dp))
                                        .clickable { onMountFolder(card.type) }
                                        .padding(vertical = 7.dp, horizontal = 8.dp),
                                    horizontalArrangement = Arrangement.Center,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.CreateNewFolder,
                                        contentDescription = null,
                                        tint = Purple400,
                                        modifier = Modifier.size(13.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = "Mount Folder",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = Purple100,
                                        maxLines = 1
                                    )
                                }

                                if (card.type == MediaType.IMAGE || card.type == MediaType.VIDEO) {
                                    Row(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(Color(0xFF0E071B))
                                            .border(1.dp, Purple900.copy(alpha = 0.6f), RoundedCornerShape(8.dp))
                                            .clickable { onAddFiles(card.type) }
                                            .padding(vertical = 7.dp, horizontal = 8.dp),
                                        horizontalArrangement = Arrangement.Center,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            imageVector = Icons.Filled.UploadFile,
                                            contentDescription = null,
                                            tint = palette.accentBright,
                                            modifier = Modifier.size(13.dp)
                                        )
                                        Spacer(modifier = Modifier.width(3.dp))
                                        Text(
                                            text = "Add",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = Purple200
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // Empty Library State vs Populated Showcases
        if (items.isEmpty()) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(DominaCard)
                    .border(1.dp, Purple900.copy(alpha = 0.55f), RoundedCornerShape(16.dp))
                    .padding(28.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "Your Local Vault is Clean & Ready",
                    style = MaterialTheme.typography.displaySmall,
                    color = Purple50
                )
                Text(
                    text = "No media files are currently indexed. Mount any folder from your device above or open Settings to rescan your local vault directories.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Purple200.copy(alpha = 0.7f)
                )
            }
        } else {
            // 1. Smart Bookmarks / Continue Reading Comics Strip (Comics ONLY)
            if (inProgressComics.isNotEmpty()) {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.BookmarkAdded,
                                contentDescription = null,
                                tint = Purple400,
                                modifier = Modifier.size(18.dp)
                            )
                            Text(
                                text = "Continue Reading Comics",
                                style = MaterialTheme.typography.displaySmall,
                                color = Purple50
                            )
                        }
                        Text(
                            text = "Pick up at last page",
                            style = MaterialTheme.typography.bodySmall,
                            color = Purple300.copy(alpha = 0.7f)
                        )
                    }
                    HorizontalDivider(color = Purple900.copy(alpha = 0.5f))

                    inProgressComics.forEach { col ->
                        val progress = MediaLogic.computeBookmarkProgress(
                            col.lastReadPage,
                            col.pageCount,
                            col.readPages
                        )
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(DominaCard)
                                .border(1.dp, Purple900.copy(alpha = 0.6f), RoundedCornerShape(12.dp))
                                .testTag("continue_comic_${col.id}")
                                .clickable { onResumeReader(col) }
                                .padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            val model = rememberThumbModel(col.thumbnailUrl)
                            Box(
                                modifier = Modifier
                                    .size(width = 60.dp, height = 78.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(Color(0xFF080410))
                                    .border(1.dp, Purple800.copy(alpha = 0.6f), RoundedCornerShape(8.dp))
                            ) {
                                if (model != null) {
                                    AsyncImage(
                                        model = model,
                                        contentDescription = col.title,
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .then(if (panicBlur) Modifier.blur(12.dp) else Modifier)
                                    )
                                }
                            }

                            Column(
                                modifier = Modifier.weight(1f),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Text(
                                    text = "PAGE ${col.lastReadPage} • ${progress.readCount}/${col.pageCount} READ (${progress.percent}%)",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Purple400
                                )
                                Text(
                                    text = col.title,
                                    style = MaterialTheme.typography.titleMedium,
                                    color = Purple50,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(6.dp)
                                        .clip(RoundedCornerShape(3.dp))
                                        .background(Purple950)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth(progress.percent / 100f)
                                            .height(6.dp)
                                            .background(
                                                Brush.horizontalGradient(
                                                    listOf(palette.accent, Purple400)
                                                )
                                            )
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // 2. Pinned Favorites Strip
            if (favoritedItems.isNotEmpty()) {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Favorite,
                                contentDescription = null,
                                tint = Purple400,
                                modifier = Modifier.size(18.dp)
                            )
                            Text(
                                text = "Pinned Favorites",
                                style = MaterialTheme.typography.displaySmall,
                                color = Purple50
                            )
                        }
                        Text(
                            text = "Filter Favorites in Advanced Search →",
                            style = MaterialTheme.typography.bodySmall,
                            color = Purple400,
                            modifier = Modifier.clickable { onNavigate(PageView.ADVANCED_SEARCH) }
                        )
                    }
                    HorizontalDivider(color = Purple900.copy(alpha = 0.5f))

                    MediaGridFlow(
                        items = favoritedItems,
                        columnsCount = columnsCount,
                        appearance = appearance,
                        panicBlur = panicBlur,
                        onSelectItem = onSelectItem,
                        onRate = onRate,
                        onToggleFavorite = onToggleFavorite,
                        onResumeReader = onResumeReader,
                        onTagClick = onQuickTagJump
                    )
                }
            }

            // 3. Highest-Rated Showcase (Strictly rated items: rating > 0)
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Highest-Rated Showcase",
                            style = MaterialTheme.typography.displaySmall,
                            color = Purple50
                        )
                        Text(
                            text = "Top rated works across your local vault — tap any star to rate inline",
                            style = MaterialTheme.typography.bodySmall,
                            color = Purple300.copy(alpha = 0.7f)
                        )
                    }
                    Text(
                        text = "Explore All Images →",
                        style = MaterialTheme.typography.bodySmall,
                        color = Purple400,
                        modifier = Modifier.clickable { onNavigate(PageView.IMAGES) }
                    )
                }
                HorizontalDivider(color = Purple900.copy(alpha = 0.5f))

                if (highestRated.isEmpty()) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(DominaCard)
                            .border(1.dp, Purple800.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = "No rated items in your showcase yet",
                            style = MaterialTheme.typography.titleSmall,
                            color = Purple100
                        )
                        Text(
                            text = "Unrated media items are hidden from this showcase. Assign 1 to 5 stars to any Image, Video, Comic, or Image Set to feature it here.",
                            style = MaterialTheme.typography.bodySmall,
                            color = Purple300.copy(alpha = 0.7f)
                        )
                    }
                } else {
                    MediaGridFlow(
                        items = highestRated,
                        columnsCount = columnsCount,
                        appearance = appearance,
                        panicBlur = panicBlur,
                        onSelectItem = onSelectItem,
                        onRate = onRate,
                        onToggleFavorite = onToggleFavorite,
                        onResumeReader = onResumeReader,
                        onTagClick = onQuickTagJump
                    )
                }
            }
        }
    }
}

@Composable
private fun ActionButtonChip(
    icon: ImageVector,
    iconTint: Color,
    label: String,
    onClick: () -> Unit,
    testTag: String
) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(Color(0xFF160B2B))
            .border(1.dp, Purple800.copy(alpha = 0.6f), RoundedCornerShape(8.dp))
            .testTag(testTag)
            .clickable { onClick() }
            .padding(horizontal = 12.dp, vertical = 9.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = iconTint,
            modifier = Modifier.size(14.dp)
        )
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium,
            color = Purple100
        )
    }
}

@Composable
fun MediaGridFlow(
    items: List<MediaItem>,
    columnsCount: Int,
    appearance: AppearanceConfig,
    panicBlur: Boolean,
    onSelectItem: (MediaItem) -> Unit,
    onRate: (String, Int) -> Unit,
    onToggleFavorite: (String, Boolean) -> Unit,
    onResumeReader: (MediaItem) -> Unit,
    onTagClick: (String) -> Unit,
    selectable: Boolean = false,
    selectedIds: List<String> = emptyList(),
    onToggleSelect: ((String) -> Unit)? = null
) {
    val gapDp = when (appearance.cardGap) {
        "tight" -> 8.dp
        "relaxed" -> 18.dp
        else -> 12.dp
    }
    val safeCols = columnsCount.coerceIn(1, 4)

    Column(verticalArrangement = Arrangement.spacedBy(gapDp)) {
        items.chunked(safeCols).forEach { rowItems ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(gapDp)
            ) {
                rowItems.forEach { item ->
                    Box(modifier = Modifier.weight(1f)) {
                        MediaCard(
                            item = item,
                            onSelect = onSelectItem,
                            onRate = onRate,
                            onToggleFavorite = onToggleFavorite,
                            onTagClick = onTagClick,
                            onResumeReader = onResumeReader,
                            panicBlur = panicBlur,
                            appearance = appearance,
                            selectable = selectable,
                            isSelected = selectedIds.contains(item.id),
                            onToggleSelect = onToggleSelect
                        )
                    }
                }
                val remaining = safeCols - rowItems.size
                repeat(remaining) {
                    Spacer(modifier = Modifier.weight(1f))
                }
            }
        }
    }
}
