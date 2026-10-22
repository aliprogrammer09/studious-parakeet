package com.example.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
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
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.BookmarkAdded
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.model.AppearanceConfig
import com.example.model.MediaItem
import com.example.model.MediaType
import com.example.ui.components.StarRating
import com.example.ui.components.StarRatingSize
import com.example.ui.components.rememberThumbModel
import com.example.ui.theme.DominaCard
import com.example.ui.theme.DominaCardInner
import com.example.ui.theme.DominaElevatedSurface
import com.example.ui.theme.LocalDominaPalette
import com.example.ui.theme.Purple100
import com.example.ui.theme.Purple200
import com.example.ui.theme.Purple300
import com.example.ui.theme.Purple400
import com.example.ui.theme.Purple50
import com.example.ui.theme.Purple600
import com.example.ui.theme.Purple700
import com.example.ui.theme.Purple800
import com.example.ui.theme.Purple900
import com.example.ui.theme.Purple950
import com.example.ui.theme.Rose300
import com.example.ui.theme.Rose800
import com.example.ui.theme.Rose950
import com.example.utils.MediaLogic
import java.util.Locale

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun CollectionOverviewView(
    item: MediaItem,
    appearance: AppearanceConfig = AppearanceConfig(),
    columnsCount: Int = 2,
    onBack: () -> Unit,
    onOpenReader: (Int) -> Unit,
    onSelectPage: (Int) -> Unit,
    onRate: (String, Int) -> Unit,
    onRatePage: (String, Int, Int) -> Unit,
    onToggleFavorite: ((String, Boolean) -> Unit)? = null,
    onToggleFavoritePage: ((String, Int, Boolean) -> Unit)? = null,
    onUpdateTags: (String, List<String>) -> Unit,
    onResetComicProgress: ((String) -> Unit)? = null,
    onMarkAllComicRead: ((String, Int) -> Unit)? = null,
    onDelete: (String) -> Unit,
    panicBlur: Boolean = false,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val palette = LocalDominaPalette.current

    var newTagInput by remember { mutableStateOf("") }
    var explorerNotice by remember { mutableStateOf<String?>(null) }
    var pageFilter by remember { mutableStateOf("all") } // all, favorites, rated
    var pageSort by remember { mutableStateOf("page") } // page, rating, name
    var sortMenuOpen by remember { mutableStateOf(false) }

    val pages = item.pages
    val isComic = item.type == MediaType.COMIC
    val readPagesSet = remember(item.readPages) { item.readPages.toSet() }

    // Only compute reading progress for Comics (never Image Sets)
    val bookmark = remember(isComic, item.lastReadPage, pages.size, item.readPages) {
        if (isComic) {
            MediaLogic.computeBookmarkProgress(item.lastReadPage, pages.size, item.readPages)
        } else {
            null
        }
    }

    val displayedPages = remember(pages, pageFilter, pageSort) {
        var list = pages.toList()
        if (pageFilter == "favorites") {
            list = list.filter { it.isFavorite }
        } else if (pageFilter == "rated") {
            list = list.filter { it.rating > 0 }
        }
        when (pageSort) {
            "rating" -> list.sortedByDescending { it.rating }
            "name" -> list.sortedWith { a, b -> MediaLogic.naturalCompare(a.filename, b.filename) }
            else -> list.sortedBy { it.pageNumber }
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 20.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        // Top Breadcrumb & Actions Bar
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(DominaCard)
                    .border(1.dp, Purple900.copy(alpha = 0.6f), RoundedCornerShape(8.dp))
                    .testTag("back_from_collection_btn")
                    .clickable { onBack() }
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = null,
                    tint = Purple200,
                    modifier = Modifier.size(15.dp)
                )
                Text(
                    text = "Back to ${if (isComic) "Comics" else "Image Sets"}",
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Medium,
                    color = Purple200
                )
            }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (item.isFavorite) Purple600 else DominaCard)
                        .border(
                            1.dp,
                            if (item.isFavorite) Purple400 else Purple900.copy(alpha = 0.6f),
                            RoundedCornerShape(8.dp)
                        )
                        .clickable { onToggleFavorite?.invoke(item.id, !item.isFavorite) }
                        .padding(horizontal = 10.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(5.dp)
                ) {
                    Icon(
                        imageVector = if (item.isFavorite) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
                        contentDescription = null,
                        tint = if (item.isFavorite) Color.White else Purple300,
                        modifier = Modifier.size(14.dp)
                    )
                    Text(
                        text = if (item.isFavorite) "Favorited" else "Favorite",
                        style = MaterialTheme.typography.bodySmall,
                        color = if (item.isFavorite) Color.White else Purple300
                    )
                }

                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(DominaCard)
                        .border(1.dp, Purple900.copy(alpha = 0.6f), RoundedCornerShape(8.dp))
                        .clickable {
                            val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
                            cm?.setPrimaryClip(ClipData.newPlainText("path", item.localPath))
                            explorerNotice = "Folder path copied: ${item.localPath}"
                        }
                        .padding(horizontal = 10.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(5.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.FolderOpen,
                        contentDescription = null,
                        tint = Purple200,
                        modifier = Modifier.size(14.dp)
                    )
                    Text("Folder", style = MaterialTheme.typography.bodySmall, color = Purple200)
                }

                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(Rose950.copy(alpha = 0.45f))
                        .border(1.dp, Rose800.copy(alpha = 0.6f), RoundedCornerShape(8.dp))
                        .clickable { onDelete(item.id) }
                        .padding(horizontal = 10.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(5.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.Delete,
                        contentDescription = null,
                        tint = Rose300,
                        modifier = Modifier.size(14.dp)
                    )
                    Text("Delete", style = MaterialTheme.typography.bodySmall, color = Rose300)
                }
            }
        }

        if (explorerNotice != null) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .background(Purple950.copy(alpha = 0.9f))
                    .border(1.dp, Purple700.copy(alpha = 0.7f), RoundedCornerShape(10.dp))
                    .padding(12.dp)
            ) {
                Text(explorerNotice!!, style = MaterialTheme.typography.bodySmall, color = Purple200)
            }
        }

        // Main Collection Showcase Banner
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(DominaCard)
                .border(1.dp, Purple900.copy(alpha = 0.6f), RoundedCornerShape(16.dp))
                .padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.Top
            ) {
                // Cover Thumbnail
                val firstPageUrl = pages.firstOrNull()?.url ?: item.thumbnailUrl
                val coverModel = rememberThumbModel(firstPageUrl)
                Box(
                    modifier = Modifier
                        .width(116.dp)
                        .aspectRatio(3f / 4f)
                        .clip(RoundedCornerShape(12.dp))
                        .background(DominaCardInner)
                        .border(1.dp, Purple800.copy(alpha = 0.7f), RoundedCornerShape(12.dp))
                        .clickable { onSelectPage(1) },
                    contentAlignment = Alignment.Center
                ) {
                    if (coverModel != null) {
                        AsyncImage(
                            model = coverModel,
                            contentDescription = item.title,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .fillMaxSize()
                                .then(if (panicBlur) Modifier.blur(18.dp) else Modifier)
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Filled.PhotoLibrary,
                            contentDescription = null,
                            tint = Purple400,
                            modifier = Modifier.size(32.dp)
                        )
                    }
                }

                // Metadata & Rating
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = if (isComic) "COMIC VOLUME ARCHIVE" else "PHOTO SET COLLECTION",
                        style = MaterialTheme.typography.labelSmall,
                        color = Purple400
                    )
                    Text(
                        text = item.title,
                        style = MaterialTheme.typography.displaySmall,
                        color = Purple50
                    )
                    Text(
                        text = "${pages.size} ${if (isComic) "Pages" else "Plates"} • Total Size: ${
                            MediaLogic.formatFileSize(item.sizeBytes)
                        }",
                        style = MaterialTheme.typography.labelSmall,
                        color = Purple300.copy(alpha = 0.8f)
                    )
                    Text(
                        text = item.localPath,
                        style = MaterialTheme.typography.labelSmall,
                        color = Purple300.copy(alpha = 0.65f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text("Rating:", style = MaterialTheme.typography.bodySmall, color = Purple300)
                        StarRating(
                            rating = item.rating,
                            onChange = { r -> onRate(item.id, r) },
                            size = StarRatingSize.MD,
                            itemTitle = item.title
                        )
                    }
                }
            }

            HorizontalDivider(color = Purple900.copy(alpha = 0.5f))

            // Editable Tags
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                item.tags.forEach { tag ->
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(DominaCardInner)
                            .border(1.dp, Purple900.copy(alpha = 0.6f), RoundedCornerShape(6.dp))
                            .padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(5.dp)
                    ) {
                        Text("#$tag", style = MaterialTheme.typography.labelSmall, color = Purple200)
                        Icon(
                            imageVector = Icons.Filled.Close,
                            contentDescription = "Remove tag $tag",
                            tint = Purple400,
                            modifier = Modifier
                                .size(11.dp)
                                .clickable { onUpdateTags(item.id, item.tags - tag) }
                        )
                    }
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .width(105.dp)
                            .clip(RoundedCornerShape(6.dp))
                            .background(DominaCardInner)
                            .border(1.dp, Purple900.copy(alpha = 0.6f), RoundedCornerShape(6.dp))
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        if (newTagInput.isEmpty()) {
                            Text("+ Add tag", style = MaterialTheme.typography.bodySmall, color = Purple400.copy(alpha = 0.5f))
                        }
                        BasicTextField(
                            value = newTagInput,
                            onValueChange = { newTagInput = it },
                            singleLine = true,
                            textStyle = MaterialTheme.typography.bodySmall.copy(color = Purple100),
                            cursorBrush = SolidColor(palette.accentBright),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(palette.accent)
                            .clickable {
                                val cleaned = newTagInput.trim().lowercase().removePrefix("#").replace(Regex("\\s+"), "-")
                                if (cleaned.isNotEmpty() && !item.tags.contains(cleaned)) {
                                    onUpdateTags(item.id, item.tags + cleaned)
                                }
                                newTagInput = ""
                            }
                            .padding(5.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Add,
                            contentDescription = "Confirm add tag",
                            tint = Color.White,
                            modifier = Modifier.size(13.dp)
                        )
                    }
                }
            }

            // Direct Prominent CTA: Open in Reader + Comic-Only Smart Bookmark Resume
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(palette.accent)
                        .testTag("open_in_reader_btn")
                        .clickable { onOpenReader(1) }
                        .padding(horizontal = 18.dp, vertical = 11.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.MenuBook,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(17.dp)
                    )
                    Text(
                        text = "Open in Reader",
                        style = MaterialTheme.typography.titleSmall,
                        color = Color.White
                    )
                }

                if (isComic && bookmark != null && bookmark.isStarted) {
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(DominaElevatedSurface)
                            .border(1.dp, palette.accentBright, RoundedCornerShape(12.dp))
                            .testTag("resume_reader_btn")
                            .clickable { onOpenReader(item.lastReadPage.coerceAtLeast(1)) }
                            .padding(horizontal = 14.dp, vertical = 11.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.BookmarkAdded,
                            contentDescription = null,
                            tint = Purple400,
                            modifier = Modifier.size(17.dp)
                        )
                        Text(
                            text = "Resume at Page ${item.lastReadPage} (${bookmark.percent}% • ${bookmark.readCount}/${pages.size} read)",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium,
                            color = Purple50
                        )
                    }
                }

                if (isComic && pages.isNotEmpty() && onMarkAllComicRead != null) {
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(DominaCardInner)
                            .border(1.dp, Purple900.copy(alpha = 0.6f), RoundedCornerShape(12.dp))
                            .clickable { onMarkAllComicRead(item.id, pages.size) }
                            .padding(horizontal = 12.dp, vertical = 11.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.CheckCircle,
                            contentDescription = null,
                            tint = Purple400,
                            modifier = Modifier.size(14.dp)
                        )
                        Text("Mark All Read", style = MaterialTheme.typography.bodySmall, color = Purple200)
                    }
                }

                if (isComic && bookmark != null && bookmark.isStarted && onResetComicProgress != null) {
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(DominaCardInner)
                            .border(1.dp, Purple900.copy(alpha = 0.6f), RoundedCornerShape(12.dp))
                            .clickable { onResetComicProgress(item.id) }
                            .padding(horizontal = 12.dp, vertical = 11.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Refresh,
                            contentDescription = null,
                            tint = Purple300,
                            modifier = Modifier.size(14.dp)
                        )
                        Text("Reset Progress", style = MaterialTheme.typography.bodySmall, color = Purple300)
                    }
                }
            }
        }

        // Ordered Pages / Plates Grid Header
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "${if (isComic) "Comic Pages" else "Image Set Plates"} (${displayedPages.size})",
                        style = MaterialTheme.typography.displaySmall,
                        color = Purple50
                    )
                    Text(
                        text = "Tap any plate below to inspect in Image Details, or rate individual pages with stars.",
                        style = MaterialTheme.typography.bodySmall,
                        color = Purple300.copy(alpha = 0.75f)
                    )
                }
            }

            // Page Filter & Sort Controls
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color(0xFF0D071A))
                        .border(1.dp, Purple900.copy(alpha = 0.6f), RoundedCornerShape(8.dp))
                        .padding(2.dp)
                ) {
                    listOf(
                        "all" to "All (${pages.size})",
                        "favorites" to "Favorites (${pages.count { it.isFavorite }})",
                        "rated" to "Rated (${pages.count { it.rating > 0 }})"
                    ).forEach { (key, label) ->
                        val active = pageFilter == key
                        Text(
                            text = label,
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Medium,
                            color = if (active) Color.White else Purple300,
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(if (active) palette.accent else Color.Transparent)
                                .clickable { pageFilter = key }
                                .padding(horizontal = 10.dp, vertical = 5.dp)
                        )
                    }
                }

                Box {
                    val sortLabel = when (pageSort) {
                        "rating" -> "Highest Rated Page"
                        "name" -> "Filename"
                        else -> "Page Order"
                    }
                    Text(
                        text = "$sortLabel ▾",
                        style = MaterialTheme.typography.bodySmall,
                        color = Purple200,
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color(0xFF0D071A))
                            .border(1.dp, Purple900.copy(alpha = 0.6f), RoundedCornerShape(8.dp))
                            .clickable { sortMenuOpen = true }
                            .padding(horizontal = 10.dp, vertical = 6.dp)
                    )
                    DropdownMenu(
                        expanded = sortMenuOpen,
                        onDismissRequest = { sortMenuOpen = false },
                        modifier = Modifier.background(Color(0xFF140A26))
                    ) {
                        DropdownMenuItem(
                            text = { Text("Page Order", color = Purple100) },
                            onClick = { pageSort = "page"; sortMenuOpen = false }
                        )
                        DropdownMenuItem(
                            text = { Text("Highest Rated Page", color = Purple100) },
                            onClick = { pageSort = "rating"; sortMenuOpen = false }
                        )
                        DropdownMenuItem(
                            text = { Text("Filename", color = Purple100) },
                            onClick = { pageSort = "name"; sortMenuOpen = false }
                        )
                    }
                }
            }

            HorizontalDivider(color = Purple900.copy(alpha = 0.5f))

            // Pages Grid
            val safeCols = columnsCount.coerceIn(2, 4)
            displayedPages.chunked(safeCols).forEach { rowPages ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    rowPages.forEach { page ->
                        val isPageRead = isComic && readPagesSet.contains(page.pageNumber)
                        val isLastReadPage = isComic && bookmark != null && bookmark.isStarted && item.lastReadPage == page.pageNumber

                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(12.dp))
                                .background(DominaCard)
                                .border(
                                    width = if (isLastReadPage) 1.5.dp else 1.dp,
                                    color = when {
                                        isLastReadPage -> Purple400
                                        isPageRead -> Purple700.copy(alpha = 0.8f)
                                        else -> Purple900.copy(alpha = 0.55f)
                                    },
                                    shape = RoundedCornerShape(12.dp)
                                )
                                .testTag("collection_page_${page.pageNumber}")
                                .clickable { onSelectPage(page.pageNumber) }
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .aspectRatio(3f / 4f)
                                    .background(DominaCardInner)
                            ) {
                                val pModel = rememberThumbModel(page.url)
                                if (pModel != null) {
                                    AsyncImage(
                                        model = pModel,
                                        contentDescription = "${item.title} - Page ${page.pageNumber}",
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .then(if (panicBlur) Modifier.blur(18.dp) else Modifier)
                                    )
                                }

                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(50.dp)
                                        .align(Alignment.BottomCenter)
                                        .background(
                                            Brush.verticalGradient(
                                                listOf(Color.Transparent, DominaCardInner.copy(alpha = 0.92f))
                                            )
                                        )
                                )

                                // Page index badge
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .align(Alignment.BottomCenter)
                                        .padding(horizontal = 8.dp, vertical = 6.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "P.${String.format(Locale.US, "%02d", page.pageNumber)}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = Purple100
                                    )
                                    Text(
                                        text = "${page.width}×${page.height}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = Purple300.copy(alpha = 0.75f)
                                    )
                                }

                                // Top-Left Favorite Button
                                Box(
                                    modifier = Modifier
                                        .align(Alignment.TopStart)
                                        .padding(6.dp)
                                        .size(26.dp)
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(
                                            if (page.isFavorite) Purple900.copy(alpha = 0.9f)
                                            else Color.Black.copy(alpha = 0.6f)
                                        )
                                        .clickable {
                                            onToggleFavoritePage?.invoke(
                                                item.id,
                                                page.pageNumber,
                                                !page.isFavorite
                                            )
                                        },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = if (page.isFavorite) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
                                        contentDescription = "Favorite Page ${page.pageNumber}",
                                        tint = if (page.isFavorite) Color.White else Purple300,
                                        modifier = Modifier.size(13.dp)
                                    )
                                }

                                // Status Badges
                                if (isLastReadPage) {
                                    Box(
                                        modifier = Modifier
                                            .align(Alignment.TopEnd)
                                            .padding(6.dp)
                                            .clip(RoundedCornerShape(4.dp))
                                            .background(palette.accent)
                                            .padding(horizontal = 6.dp, vertical = 2.dp)
                                    ) {
                                        Text(
                                            text = "LAST READ",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = Color.White
                                        )
                                    }
                                } else if (isPageRead) {
                                    Box(
                                        modifier = Modifier
                                            .align(Alignment.TopEnd)
                                            .padding(6.dp)
                                            .clip(RoundedCornerShape(4.dp))
                                            .background(Purple950.copy(alpha = 0.9f))
                                            .border(1.dp, Purple600.copy(alpha = 0.6f), RoundedCornerShape(4.dp))
                                            .padding(horizontal = 6.dp, vertical = 2.dp)
                                    ) {
                                        Text(
                                            text = "READ",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = Purple200
                                        )
                                    }
                                }
                            }

                            // Bottom card content with interactive page rating
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(Color(0xFF0E071C))
                                    .padding(horizontal = 10.dp, vertical = 8.dp),
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    StarRating(
                                        rating = page.rating,
                                        onChange = { r -> onRatePage(item.id, page.pageNumber, r) },
                                        size = StarRatingSize.SM,
                                        itemTitle = "Page ${page.pageNumber}"
                                    )
                                    Text(
                                        text = if (page.rating > 0) "${page.rating}★" else "Rate",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = Purple400.copy(alpha = 0.8f)
                                    )
                                }

                                HorizontalDivider(color = Purple900.copy(alpha = 0.4f))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = page.filename,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = Purple200,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        modifier = Modifier.weight(1f)
                                    )
                                    Text(
                                        text = MediaLogic.formatFileSize(page.sizeBytes),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = Purple400.copy(alpha = 0.7f)
                                    )
                                }
                            }
                        }
                    }
                    repeat(safeCols - rowPages.size) {
                        Spacer(modifier = Modifier.weight(1f))
                    }
                }
            }
        }
    }
}
