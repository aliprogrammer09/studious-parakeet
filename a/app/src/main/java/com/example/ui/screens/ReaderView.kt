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
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Brightness2
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.material.icons.filled.FullscreenExit
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.ViewSidebar
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material.icons.filled.ZoomIn
import androidx.compose.material.icons.filled.ZoomOut
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.model.MediaItem
import com.example.model.MediaType
import com.example.model.ReaderDirection
import com.example.model.ReaderFitMode
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
import com.example.ui.theme.Purple800
import com.example.ui.theme.Purple900
import com.example.utils.MediaLogic
import kotlinx.coroutines.delay
import java.util.Locale
import kotlin.math.roundToInt

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ReaderView(
    item: MediaItem,
    initialPage: Int = 1,
    defaultFitMode: ReaderFitMode = ReaderFitMode.HEIGHT,
    defaultDirection: ReaderDirection = ReaderDirection.LTR,
    slideshowIntervalSeconds: Int = 4,
    onCloseToOverview: () -> Unit,
    onRate: (String, Int) -> Unit,
    onRatePage: ((String, Int, Int) -> Unit)? = null,
    onUpdateBookmark: (String, Int, List<Int>) -> Unit,
    panicBlur: Boolean = false,
    modifier: Modifier = Modifier
) {
    val palette = LocalDominaPalette.current
    val pages = item.pages
    val totalPages = pages.size.coerceAtLeast(1)
    val isComic = item.type == MediaType.COMIC

    var currentPage by remember(item.id, initialPage) {
        mutableIntStateOf(initialPage.coerceIn(1, totalPages))
    }
    var fitMode by remember { mutableStateOf(defaultFitMode) }
    var direction by remember { mutableStateOf(defaultDirection) }
    var readerZoom by remember { mutableFloatStateOf(1f) }
    var drawerOpen by remember { mutableStateOf(false) }
    var isFullscreen by remember { mutableStateOf(false) }
    var fullscreenControlsVisible by remember { mutableStateOf(false) }
    var slideshowPlaying by remember { mutableStateOf(false) }
    var slideshowSpeed by remember { mutableIntStateOf(slideshowIntervalSeconds.coerceIn(1, 10)) }
    var speedMenuOpen by remember { mutableStateOf(false) }
    var readerInvert by remember { mutableStateOf(false) }

    val stepAmount = if (fitMode == ReaderFitMode.DOUBLE && direction != ReaderDirection.VERTICAL) 2 else 1

    fun goNextPage() {
        currentPage = (currentPage + stepAmount).coerceAtMost(totalPages)
    }

    fun goPrevPage() {
        currentPage = (currentPage - stepAmount).coerceAtLeast(1)
    }

    // Persist smart bookmark & actual readPages for Comics ONLY
    LaunchedEffect(isComic, item.id, currentPage, fitMode, direction, totalPages) {
        if (isComic && pages.isNotEmpty()) {
            val visiblePages = if (fitMode == ReaderFitMode.DOUBLE &&
                direction != ReaderDirection.VERTICAL &&
                currentPage < totalPages
            ) {
                listOf(currentPage, currentPage + 1)
            } else {
                listOf(currentPage)
            }
            onUpdateBookmark(item.id, currentPage, visiblePages)
        }
    }

    // Automatic Slideshow Timer (infinite loop back to page 1)
    LaunchedEffect(slideshowPlaying, totalPages, slideshowSpeed, currentPage) {
        if (slideshowPlaying && totalPages > 1) {
            delay(slideshowSpeed.coerceAtLeast(1) * 1000L)
            currentPage = if (currentPage >= totalPages) 1 else currentPage + 1
        }
    }

    val activePageObj = pages.getOrNull(currentPage - 1) ?: pages.firstOrNull()
    val secondSpreadPageObj = if (fitMode == ReaderFitMode.DOUBLE && currentPage < totalPages) {
        pages.getOrNull(currentPage)
    } else null

    val comicProgress = remember(isComic, item.lastReadPage, totalPages, item.readPages) {
        if (isComic) {
            MediaLogic.computeBookmarkProgress(item.lastReadPage, totalPages, item.readPages)
        } else null
    }
    val readSet = remember(item.readPages) { item.readPages.toSet() }

    val invertColorFilter = remember(readerInvert) {
        if (readerInvert) {
            ColorFilter.colorMatrix(
                ColorMatrix(
                    floatArrayOf(
                        -1f, 0f, 0f, 0f, 255f,
                        0f, -1f, 0f, 0f, 255f,
                        0f, 0f, -1f, 0f, 255f,
                        0f, 0f, 0f, 1f, 0f
                    )
                )
            )
        } else null
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF06030C))
    ) {
        // Top Navigation Bar (Visible in non-fullscreen mode)
        if (!isFullscreen) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFF10081E))
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Row 1: Overview Back + Pages Drawer + Current Page Star Rating + Fullscreen
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(DominaElevatedSurface)
                                .border(1.dp, Purple800.copy(alpha = 0.6f), RoundedCornerShape(8.dp))
                                .testTag("reader_overview_btn")
                                .clickable { onCloseToOverview() }
                                .padding(horizontal = 10.dp, vertical = 7.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(5.dp)
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = null,
                                tint = Purple100,
                                modifier = Modifier.size(14.dp)
                            )
                            Text("Overview", style = MaterialTheme.typography.bodySmall, color = Purple100)
                        }

                        Row(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (drawerOpen) palette.accent else DominaElevatedSurface)
                                .border(
                                    1.dp,
                                    if (drawerOpen) Purple400 else Purple800.copy(alpha = 0.6f),
                                    RoundedCornerShape(8.dp)
                                )
                                .testTag("reader_drawer_toggle")
                                .clickable { drawerOpen = !drawerOpen }
                                .padding(horizontal = 10.dp, vertical = 7.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(5.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.ViewSidebar,
                                contentDescription = "Toggle thumbnail sidebar drawer",
                                tint = if (drawerOpen) Color.White else Purple200,
                                modifier = Modifier.size(14.dp)
                            )
                            Text(
                                text = "Pages ($currentPage/$totalPages)",
                                style = MaterialTheme.typography.bodySmall,
                                color = if (drawerOpen) Color.White else Purple200
                            )
                        }
                    }

                    // Current Page Star Rating
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(DominaCardInner)
                            .border(1.dp, Purple900.copy(alpha = 0.6f), RoundedCornerShape(8.dp))
                            .padding(horizontal = 8.dp, vertical = 5.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(5.dp)
                    ) {
                        Text(
                            text = "P.$currentPage:",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = Purple400
                        )
                        StarRating(
                            rating = activePageObj?.rating ?: 0,
                            onChange = { r -> onRatePage?.invoke(item.id, currentPage, r) },
                            size = StarRatingSize.SM,
                            itemTitle = "Page $currentPage rating"
                        )
                    }
                }

                // Row 2: Display Fit Modes + Reading Direction + Slideshow + Invert + Fullscreen
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    // Fit Modes
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(DominaCardInner)
                            .border(1.dp, Purple900.copy(alpha = 0.6f), RoundedCornerShape(8.dp))
                            .padding(3.dp),
                        horizontalArrangement = Arrangement.spacedBy(3.dp)
                    ) {
                        ReaderFitMode.entries.forEach { mode ->
                            val active = fitMode == mode
                            Text(
                                text = mode.label,
                                style = MaterialTheme.typography.labelSmall,
                                color = if (active) Color.White else Purple300,
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(if (active) palette.accent else Color.Transparent)
                                    .testTag("reader_fit_${mode.key}")
                                    .clickable { fitMode = mode }
                                    .padding(horizontal = 8.dp, vertical = 5.dp)
                            )
                        }
                    }

                    // Direction Modes
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(DominaCardInner)
                            .border(1.dp, Purple900.copy(alpha = 0.6f), RoundedCornerShape(8.dp))
                            .padding(3.dp),
                        horizontalArrangement = Arrangement.spacedBy(3.dp)
                    ) {
                        ReaderDirection.entries.forEach { dir ->
                            val active = direction == dir
                            Text(
                                text = dir.label,
                                style = MaterialTheme.typography.labelSmall,
                                color = if (active) Color.White else Purple300,
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(if (active) Purple600 else Color.Transparent)
                                    .testTag("reader_dir_${dir.key}")
                                    .clickable { direction = dir }
                                    .padding(horizontal = 8.dp, vertical = 5.dp)
                            )
                        }
                    }

                    // Zoom Controls
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(DominaCardInner)
                            .border(1.dp, Purple900.copy(alpha = 0.6f), RoundedCornerShape(8.dp))
                            .padding(3.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Filled.ZoomOut,
                            contentDescription = "Zoom out reader",
                            tint = Purple200,
                            modifier = Modifier
                                .clickable { readerZoom = (readerZoom - 0.2f).coerceAtLeast(0.5f) }
                                .padding(4.dp)
                                .size(14.dp)
                        )
                        Text(
                            text = "${(readerZoom * 100).roundToInt()}%",
                            style = MaterialTheme.typography.labelSmall,
                            color = Purple100,
                            modifier = Modifier
                                .clickable { readerZoom = 1f }
                                .padding(horizontal = 4.dp)
                        )
                        Icon(
                            imageVector = Icons.Filled.ZoomIn,
                            contentDescription = "Zoom in reader",
                            tint = Purple200,
                            modifier = Modifier
                                .clickable { readerZoom = (readerZoom + 0.2f).coerceAtMost(3f) }
                                .padding(4.dp)
                                .size(14.dp)
                        )
                    }

                    // Slideshow + Speed
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(DominaElevatedSurface)
                            .border(1.dp, Purple800.copy(alpha = 0.6f), RoundedCornerShape(8.dp))
                            .padding(horizontal = 6.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .clip(RoundedCornerShape(5.dp))
                                .background(if (slideshowPlaying) palette.accent else Color.Transparent)
                                .clickable { slideshowPlaying = !slideshowPlaying }
                                .padding(horizontal = 6.dp, vertical = 2.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(3.dp)
                        ) {
                            Icon(
                                imageVector = if (slideshowPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(12.dp)
                            )
                            Text("Slide", style = MaterialTheme.typography.labelSmall, color = Color.White)
                        }
                        Box {
                            Text(
                                text = "${slideshowSpeed}s ▾",
                                style = MaterialTheme.typography.labelSmall,
                                color = Purple200,
                                modifier = Modifier.clickable { speedMenuOpen = true }
                            )
                            DropdownMenu(
                                expanded = speedMenuOpen,
                                onDismissRequest = { speedMenuOpen = false },
                                modifier = Modifier.background(Color(0xFF140A26))
                            ) {
                                listOf(1, 2, 3, 4, 5, 7, 10).forEach { s ->
                                    DropdownMenuItem(
                                        text = { Text("${s}s", color = Purple100) },
                                        onClick = { slideshowSpeed = s; speedMenuOpen = false }
                                    )
                                }
                            }
                        }
                    }

                    // Night Invert Toggle
                    Icon(
                        imageVector = if (readerInvert) Icons.Filled.Brightness2 else Icons.Filled.WbSunny,
                        contentDescription = "Night Reading Invert Mode",
                        tint = if (readerInvert) Color.White else Purple200,
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (readerInvert) palette.accent else DominaElevatedSurface)
                            .border(
                                1.dp,
                                if (readerInvert) Purple400 else Purple800.copy(alpha = 0.6f),
                                RoundedCornerShape(8.dp)
                            )
                            .clickable { readerInvert = !readerInvert }
                            .padding(7.dp)
                            .size(14.dp)
                    )

                    // Fullscreen Button
                    Icon(
                        imageVector = Icons.Filled.Fullscreen,
                        contentDescription = "Toggle reader fullscreen",
                        tint = Purple200,
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(DominaElevatedSurface)
                            .border(1.dp, Purple800.copy(alpha = 0.6f), RoundedCornerShape(8.dp))
                            .clickable {
                                isFullscreen = true
                                fullscreenControlsVisible = true
                            }
                            .padding(7.dp)
                            .size(14.dp)
                    )
                }
            }
        }

        // Main Reader Stage: Thumbnail Drawer + Viewport
        Box(modifier = Modifier.weight(1f)) {
            Row(modifier = Modifier.fillMaxSize()) {
                // Quick Thumbnail Sidebar Drawer
                if (drawerOpen && !isFullscreen) {
                    LazyColumn(
                        modifier = Modifier
                            .width(146.dp)
                            .fillMaxHeight()
                            .background(Color(0xFF0D0618))
                            .padding(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        item {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 4.dp, vertical = 2.dp),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("INDEX", style = MaterialTheme.typography.labelSmall, color = Purple300)
                                Text("$totalPages PLATES", style = MaterialTheme.typography.labelSmall, color = Purple300)
                            }
                        }

                        items(pages, key = { it.id }) { p ->
                            val isCurrent = p.pageNumber == currentPage ||
                                    (fitMode == ReaderFitMode.DOUBLE && p.pageNumber == currentPage + 1)
                            val isRead = isComic && readSet.contains(p.pageNumber)

                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (isCurrent) Color(0xFF1F0F3A) else DominaCard)
                                    .border(
                                        1.dp,
                                        if (isCurrent) Purple400 else Purple900.copy(alpha = 0.5f),
                                        RoundedCornerShape(8.dp)
                                    )
                                    .clickable { currentPage = p.pageNumber }
                            ) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .aspectRatio(3f / 4f)
                                        .background(DominaCardInner)
                                ) {
                                    val model = rememberThumbModel(p.url)
                                    if (model != null) {
                                        AsyncImage(
                                            model = model,
                                            contentDescription = "Page ${p.pageNumber}",
                                            contentScale = ContentScale.Crop,
                                            modifier = Modifier
                                                .fillMaxSize()
                                                .then(if (panicBlur) Modifier.blur(12.dp) else Modifier)
                                        )
                                    }
                                }

                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 6.dp, vertical = 4.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "P.${String.format(Locale.US, "%02d", p.pageNumber)}",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Normal,
                                        color = if (isCurrent) Purple400 else Purple200
                                    )
                                    Text(
                                        text = if (p.rating > 0) "★ ${p.rating}" else if (isRead) "READ" else p.format,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = Purple400.copy(alpha = 0.75f)
                                    )
                                }
                            }
                        }
                    }
                }

                // Primary Reading Canvas
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .background(Color(0xFF05020A))
                        .clickable {
                            if (isFullscreen) {
                                fullscreenControlsVisible = !fullscreenControlsVisible
                            }
                        },
                    contentAlignment = Alignment.Center
                ) {
                    if (direction == ReaderDirection.VERTICAL) {
                        // Vertical Webtoon Continuous Scroll Mode
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(vertical = 12.dp, horizontal = 8.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            items(pages, key = { it.id }) { p ->
                                val model = rememberThumbModel(p.url)
                                if (model != null) {
                                    AsyncImage(
                                        model = model,
                                        contentDescription = "Page ${p.pageNumber}",
                                        contentScale = if (fitMode == ReaderFitMode.WIDTH) ContentScale.FillWidth else ContentScale.Fit,
                                        colorFilter = invertColorFilter,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .aspectRatio(3f / 4f)
                                            .clip(RoundedCornerShape(6.dp))
                                            .border(1.dp, Purple900.copy(alpha = 0.6f), RoundedCornerShape(6.dp))
                                            .then(if (panicBlur) Modifier.blur(22.dp) else Modifier)
                                    )
                                }
                            }
                        }
                    } else {
                        // Horizontal Paginated Flip Mode (LTR / RTL & Single / Double Spread)
                        Row(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(12.dp)
                                .graphicsLayer {
                                    scaleX = readerZoom
                                    scaleY = readerZoom
                                },
                            horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            val pagesToRender = if (secondSpreadPageObj != null) {
                                if (direction == ReaderDirection.RTL) {
                                    listOf(secondSpreadPageObj, activePageObj)
                                } else {
                                    listOf(activePageObj, secondSpreadPageObj)
                                }
                            } else {
                                listOf(activePageObj)
                            }

                            pagesToRender.filterNotNull().forEach { pageItem ->
                                val model = rememberThumbModel(pageItem.url)
                                if (model != null) {
                                    AsyncImage(
                                        model = model,
                                        contentDescription = "Page ${pageItem.pageNumber}",
                                        contentScale = when (fitMode) {
                                            ReaderFitMode.WIDTH -> ContentScale.FillWidth
                                            ReaderFitMode.ORIGINAL -> ContentScale.Inside
                                            else -> ContentScale.Fit
                                        },
                                        colorFilter = invertColorFilter,
                                        modifier = Modifier
                                            .weight(1f)
                                            .fillMaxHeight()
                                            .clip(RoundedCornerShape(6.dp))
                                            .border(
                                                1.dp,
                                                if (isFullscreen) Color.Transparent else Purple900.copy(alpha = 0.6f),
                                                RoundedCornerShape(6.dp)
                                            )
                                            .then(if (panicBlur) Modifier.blur(22.dp) else Modifier)
                                    )
                                }
                            }
                        }

                        // Left Flip Button
                        if (!isFullscreen) {
                            val leftEnabled = if (direction == ReaderDirection.RTL) currentPage < totalPages else currentPage > 1
                            Box(
                                modifier = Modifier
                                    .align(Alignment.CenterStart)
                                    .padding(start = 10.dp)
                                    .size(40.dp)
                                    .clip(CircleShape)
                                    .background(DominaCard.copy(alpha = if (leftEnabled) 0.9f else 0.35f))
                                    .border(1.dp, Purple800.copy(alpha = 0.7f), CircleShape)
                                    .testTag("reader_left_flip_btn")
                                    .clickable(enabled = leftEnabled) {
                                        if (direction == ReaderDirection.RTL) goNextPage() else goPrevPage()
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                                    contentDescription = if (direction == ReaderDirection.RTL) "Next page" else "Previous page",
                                    tint = Color.White
                                )
                            }

                            // Right Flip Button
                            val rightEnabled = if (direction == ReaderDirection.RTL) currentPage > 1 else currentPage < totalPages
                            Box(
                                modifier = Modifier
                                    .align(Alignment.CenterEnd)
                                    .padding(end = 10.dp)
                                    .size(40.dp)
                                    .clip(CircleShape)
                                    .background(DominaCard.copy(alpha = if (rightEnabled) 0.9f else 0.35f))
                                    .border(1.dp, Purple800.copy(alpha = 0.7f), CircleShape)
                                    .testTag("reader_right_flip_btn")
                                    .clickable(enabled = rightEnabled) {
                                        if (direction == ReaderDirection.RTL) goPrevPage() else goNextPage()
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                                    contentDescription = if (direction == ReaderDirection.RTL) "Previous page" else "Next page",
                                    tint = Color.White
                                )
                            }
                        }
                    }

                    // Floating Fullscreen Controls Overlay
                    if (isFullscreen && fullscreenControlsVisible) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .align(Alignment.TopCenter)
                                .background(Color(0xE60B0516))
                                .padding(horizontal = 16.dp, vertical = 10.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(palette.accent)
                                    .clickable { isFullscreen = false }
                                    .padding(horizontal = 10.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(5.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.FullscreenExit,
                                    contentDescription = "Exit fullscreen",
                                    tint = Color.White,
                                    modifier = Modifier.size(14.dp)
                                )
                                Text("Exit Fullscreen", style = MaterialTheme.typography.bodySmall, color = Color.White)
                            }

                            Text(
                                text = "Page $currentPage of $totalPages",
                                style = MaterialTheme.typography.labelMedium,
                                color = Purple200
                            )

                            StarRating(
                                rating = activePageObj?.rating ?: 0,
                                onChange = { r -> onRatePage?.invoke(item.id, currentPage, r) },
                                size = StarRatingSize.SM,
                                itemTitle = "Page $currentPage"
                            )
                        }
                    }
                }
            }
        }

        // Bottom Scrubber & Reading Status Bar (Hidden in buttonless fullscreen mode)
        if (!isFullscreen) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFF10081E))
                    .padding(horizontal = 14.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(5.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.MenuBook,
                        contentDescription = null,
                        tint = Purple400,
                        modifier = Modifier.size(14.dp)
                    )
                    val spreadText = if (secondSpreadPageObj != null) "–${secondSpreadPageObj.pageNumber}" else ""
                    Text(
                        text = "Page $currentPage$spreadText of $totalPages",
                        style = MaterialTheme.typography.labelSmall,
                        color = Purple100
                    )
                }

                Slider(
                    value = currentPage.toFloat(),
                    onValueChange = { currentPage = it.roundToInt().coerceIn(1, totalPages) },
                    valueRange = 1f..totalPages.coerceAtLeast(1).toFloat(),
                    colors = SliderDefaults.colors(
                        thumbColor = palette.accentBright,
                        activeTrackColor = palette.accent,
                        inactiveTrackColor = Purple900.copy(alpha = 0.5f)
                    ),
                    modifier = Modifier
                        .weight(1f)
                        .height(22.dp)
                )

                if (isComic && comicProgress != null) {
                    Text(
                        text = "Read: ${comicProgress.readCount}/$totalPages (${comicProgress.percent}%)",
                        style = MaterialTheme.typography.labelSmall,
                        color = Purple400
                    )
                }
            }
        }
    }
}
