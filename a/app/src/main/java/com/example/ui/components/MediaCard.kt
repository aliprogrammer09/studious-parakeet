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
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.Photo
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.model.AppearanceConfig
import com.example.model.MediaItem
import com.example.model.MediaType
import com.example.ui.theme.DominaCard
import com.example.ui.theme.DominaCardInner
import com.example.ui.theme.DominaCardInput
import com.example.ui.theme.LocalDominaPalette
import com.example.ui.theme.Purple100
import com.example.ui.theme.Purple200
import com.example.ui.theme.Purple300
import com.example.ui.theme.Purple400
import com.example.ui.theme.Purple50
import com.example.ui.theme.Purple700
import com.example.ui.theme.Purple900
import com.example.ui.theme.Purple950
import com.example.utils.MediaLogic
import java.io.File

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun MediaCard(
    item: MediaItem,
    onSelect: (MediaItem) -> Unit,
    onRate: (String, Int) -> Unit,
    onToggleFavorite: ((String, Boolean) -> Unit)? = null,
    onTagClick: ((String) -> Unit)? = null,
    onResumeReader: ((MediaItem) -> Unit)? = null,
    panicBlur: Boolean = false,
    appearance: AppearanceConfig = AppearanceConfig(),
    aspectMode: String? = null,
    selectable: Boolean = false,
    isSelected: Boolean = false,
    onToggleSelect: ((String) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val palette = LocalDominaPalette.current

    val effectiveAspect = aspectMode ?: appearance.thumbnailAspect.ifBlank {
        if (item.type == MediaType.VIDEO) "landscape" else "portrait"
    }
    val ratio = when (effectiveAspect) {
        "landscape" -> 16f / 9f
        "square" -> 1f
        "comic" -> 2f / 3f
        else -> 3f / 4f
    }

    val cornerRadius = when (appearance.cardRadius) {
        "sharp" -> 6.dp
        "lux" -> 16.dp
        else -> 12.dp
    }
    val cardShape = RoundedCornerShape(cornerRadius)
    val contentScale = if (appearance.thumbnailFit == "contain") {
        ContentScale.Fit
    } else {
        ContentScale.Crop
    }

    val cardBgColor = when (appearance.borderStyle) {
        "minimal" -> Color(0xE60E071B)
        "glass" -> Color(0xB3160B28)
        else -> DominaCard
    }
    val borderColor = if (isSelected) {
        palette.accentBright
    } else {
        palette.border
    }

    val isComic = item.type == MediaType.COMIC
    val bookmark = if (isComic) {
        MediaLogic.computeBookmarkProgress(item.lastReadPage, item.pageCount, item.readPages)
    } else {
        null
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(cardShape)
            .background(cardBgColor)
            .border(if (isSelected) 2.dp else 1.dp, borderColor, cardShape)
            .testTag("media_card_${item.id}")
            .clickable {
                if (selectable && onToggleSelect != null) {
                    onToggleSelect(item.id)
                } else {
                    onSelect(item)
                }
            }
    ) {
        // Thumbnail Viewport
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(ratio)
                .background(DominaCardInner)
        ) {
            val thumbModel: Any? = rememberThumbModel(item.thumbnailUrl.ifBlank { item.mediaUrl })
            if (thumbModel != null) {
                AsyncImage(
                    model = thumbModel,
                    contentDescription = item.title,
                    contentScale = contentScale,
                    modifier = Modifier
                        .fillMaxSize()
                        .then(if (panicBlur) Modifier.blur(20.dp) else Modifier)
                )
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.linearGradient(
                                listOf(Color(0xFF210D3B), Color(0xFF110722), DominaCardInner)
                            )
                        )
                        .padding(16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        val icon = when (item.type) {
                            MediaType.VIDEO -> Icons.Filled.Movie
                            MediaType.COMIC -> Icons.Filled.MenuBook
                            MediaType.IMAGESET -> Icons.Filled.PhotoLibrary
                            MediaType.IMAGE -> Icons.Filled.Photo
                        }
                        Icon(
                            imageVector = icon,
                            contentDescription = item.type.key,
                            tint = palette.accentBright,
                            modifier = Modifier.size(32.dp)
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = item.title,
                            style = MaterialTheme.typography.bodySmall,
                            color = Purple200,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }

            // Bottom Gradient Scrim
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(68.dp)
                    .align(Alignment.BottomCenter)
                    .background(
                        Brush.verticalGradient(
                            listOf(
                                Color.Transparent,
                                DominaCardInput.copy(alpha = 0.5f),
                                DominaCardInput.copy(alpha = 0.95f)
                            )
                        )
                    )
            )

            // Top-Left Batch Selection Checkbox
            if (selectable) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(8.dp)
                        .size(24.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .background(if (isSelected) palette.accent else Color.Black.copy(alpha = 0.7f))
                        .border(1.dp, if (isSelected) palette.accentBright else Purple700, RoundedCornerShape(6.dp))
                        .clickable { onToggleSelect?.invoke(item.id) },
                    contentAlignment = Alignment.Center
                ) {
                    if (isSelected) {
                        Icon(
                            imageVector = Icons.Filled.Check,
                            contentDescription = "Selected",
                            tint = Color.White,
                            modifier = Modifier.size(14.dp)
                        )
                    }
                }
            }

            // Top-Right Favorite Heart Toggle
            if (onToggleFavorite != null) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(8.dp)
                        .size(30.dp)
                        .clip(CircleShape)
                        .background(
                            if (item.isFavorite) palette.accent.copy(alpha = 0.9f)
                            else Color.Black.copy(alpha = 0.6f)
                        )
                        .border(
                            1.dp,
                            if (item.isFavorite) palette.accentBright else Purple700.copy(alpha = 0.5f),
                            CircleShape
                        )
                        .testTag("fav_btn_${item.id}")
                        .clickable { onToggleFavorite(item.id, !item.isFavorite) },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (item.isFavorite) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
                        contentDescription = if (item.isFavorite) "Remove from favorites" else "Add to favorites",
                        tint = if (item.isFavorite) Color.White else Purple200,
                        modifier = Modifier.size(15.dp)
                    )
                }
            }

            // Video Duration Overlay
            if (item.type == MediaType.VIDEO) {
                Row(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(horizontal = 10.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.PlayArrow,
                        contentDescription = null,
                        tint = palette.accentBright,
                        modifier = Modifier.size(14.dp)
                    )
                    Text(
                        text = MediaLogic.formatDuration(item.durationSeconds),
                        style = MaterialTheme.typography.labelMedium,
                        color = Purple100
                    )
                }
            }

            // Comic / Image Set Page Count & Comic-Only Resume Indicator
            if (item.type == MediaType.COMIC || item.type == MediaType.IMAGESET) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.BottomCenter)
                        .padding(horizontal = 10.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val count = item.pageCount.coerceAtLeast(item.pages.size)
                    val unit = if (item.type == MediaType.COMIC) "PAGES" else "PLATES"
                    Text(
                        text = "$count $unit",
                        style = MaterialTheme.typography.labelSmall,
                        color = Purple100
                    )

                    if (isComic && bookmark != null && bookmark.isStarted && onResumeReader != null) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(palette.accent)
                                .border(1.dp, Purple400.copy(alpha = 0.4f), RoundedCornerShape(4.dp))
                                .clickable { onResumeReader(item) }
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = "Resume P.${item.lastReadPage} (${bookmark.percent}%)",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color.White,
                                fontSize = 10.sp
                            )
                        }
                    }
                }
            }

            // Smart Bookmark Progress Line on Comics ONLY (never Image Sets)
            if (isComic && bookmark != null && bookmark.isStarted) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(4.dp)
                        .align(Alignment.BottomCenter)
                        .background(Purple950)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(bookmark.percent / 100f)
                            .height(4.dp)
                            .background(
                                Brush.horizontalGradient(
                                    listOf(palette.accent, palette.accentBright)
                                )
                            )
                    )
                }
            }
        }

        // Card Body: Title, Interactive 5-Star Rating, and Unboxed Metadata/Tags
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(
                text = item.title,
                style = MaterialTheme.typography.titleSmall,
                color = Purple50,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            if (appearance.showCardRating || appearance.showCardSize || appearance.showCardDimensions) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (appearance.showCardRating) {
                        StarRating(
                            rating = item.rating,
                            onChange = { r -> onRate(item.id, r) },
                            size = StarRatingSize.SM,
                            itemTitle = item.title
                        )
                    } else {
                        Spacer(modifier = Modifier.size(1.dp))
                    }

                    if (appearance.showCardSize || appearance.showCardDimensions) {
                        val dimText = if (appearance.showCardDimensions && item.width > 0) {
                            "${item.width}×${item.height}"
                        } else {
                            item.format
                        }
                        val sizeText = if (appearance.showCardSize) {
                            " • ${MediaLogic.formatFileSize(item.sizeBytes)}"
                        } else ""
                        Text(
                            text = dimText + sizeText,
                            style = MaterialTheme.typography.labelSmall,
                            color = Purple300.copy(alpha = 0.65f),
                            maxLines = 1
                        )
                    }
                }
            }

            if (appearance.showCardTags && item.tags.isNotEmpty()) {
                HorizontalDivider(color = Purple900.copy(alpha = 0.4f), thickness = 1.dp)
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    val displayedTags = item.tags.take(4)
                    displayedTags.forEachIndexed { idx, tag ->
                        if (idx > 0) {
                            Text(
                                text = "•",
                                style = MaterialTheme.typography.bodySmall,
                                color = Purple700
                            )
                        }
                        Text(
                            text = "#$tag",
                            style = MaterialTheme.typography.bodySmall,
                            color = Purple300.copy(alpha = 0.8f),
                            modifier = if (onTagClick != null) {
                                Modifier.clickable { onTagClick(tag) }
                            } else Modifier
                        )
                    }
                    if (item.tags.size > 4) {
                        Text(
                            text = "• +${item.tags.size - 4}",
                            style = MaterialTheme.typography.labelSmall,
                            color = Purple400.copy(alpha = 0.7f)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun rememberThumbModel(pathOrUrl: String): Any? {
    if (pathOrUrl.isBlank()) return null
    val f = File(pathOrUrl)
    return if (f.exists()) f else pathOrUrl
}
