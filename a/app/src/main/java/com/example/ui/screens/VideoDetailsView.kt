package com.example.ui.screens

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.VolumeOff
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.FastForward
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Forward10
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.material.icons.filled.FullscreenExit
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.Replay10
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material3.HorizontalDivider
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.model.MediaItem
import com.example.ui.components.StarRating
import com.example.ui.components.StarRatingSize
import com.example.ui.components.rememberThumbModel
import com.example.ui.theme.DominaCard
import com.example.ui.theme.DominaCardInner
import com.example.ui.theme.DominaCardInput
import com.example.ui.theme.DominaSidebarBg
import com.example.ui.theme.Emerald300
import com.example.ui.theme.LocalDominaPalette
import com.example.ui.theme.Purple100
import com.example.ui.theme.Purple200
import com.example.ui.theme.Purple300
import com.example.ui.theme.Purple400
import com.example.ui.theme.Purple50
import com.example.ui.theme.Purple800
import com.example.ui.theme.Purple900
import com.example.ui.theme.Rose300
import com.example.ui.theme.Rose400
import com.example.ui.theme.Rose800
import com.example.ui.theme.Rose950
import com.example.utils.MediaLogic
import kotlinx.coroutines.delay
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun VideoDetailsView(
    item: MediaItem,
    nextVideoItem: MediaItem? = null,
    defaultLoop: Boolean = false,
    defaultAutoPlayNext: Boolean = true,
    onClose: () -> Unit,
    onSelectVideo: (MediaItem) -> Unit,
    onRate: (String, Int) -> Unit,
    onToggleFavorite: ((String, Boolean) -> Unit)? = null,
    onUpdateTags: (String, List<String>) -> Unit,
    onDelete: (String) -> Unit,
    panicBlur: Boolean = false,
    modifier: Modifier = Modifier
) {
    val palette = LocalDominaPalette.current

    var isPlaying by remember(item.id) { mutableStateOf(true) }
    var currentTime by remember(item.id) { mutableFloatStateOf(0f) }
    val duration = remember(item.id, item.durationSeconds) {
        item.durationSeconds.coerceAtLeast(30).toFloat()
    }
    var isLooping by remember { mutableStateOf(defaultLoop) }
    var autoPlayNext by remember { mutableStateOf(defaultAutoPlayNext) }
    var playbackSpeed by remember { mutableFloatStateOf(1f) }
    var volumePercent by remember { mutableIntStateOf(100) }
    var isMuted by remember { mutableStateOf(false) }
    var isFullscreen by remember { mutableStateOf(false) }
    var showFsControls by remember { mutableStateOf(true) }

    // A-B Segment Loop state
    var pointA by remember(item.id) { mutableStateOf<Float?>(null) }
    var pointB by remember(item.id) { mutableStateOf<Float?>(null) }

    var newTagInput by remember { mutableStateOf("") }
    var snapshotNotice by remember { mutableStateOf<String?>(null) }

    // Playback clock with A-B loop & Auto-Play Next support
    LaunchedEffect(isPlaying, playbackSpeed, isLooping, autoPlayNext, pointA, pointB, item.id) {
        while (isPlaying) {
            delay(100L)
            val step = 0.1f * playbackSpeed
            val nextT = currentTime + step
            val a = pointA
            val b = pointB
            if (a != null && b != null && nextT >= b) {
                currentTime = a
            } else if (nextT >= duration) {
                if (isLooping) {
                    currentTime = 0f
                } else if (autoPlayNext && nextVideoItem != null && nextVideoItem.id != item.id) {
                    onSelectVideo(nextVideoItem)
                    break
                } else {
                    currentTime = duration
                    isPlaying = false
                }
            } else {
                currentTime = nextT
            }
        }
    }

    val infiniteTransition = rememberInfiniteTransition(label = "cinema_pulse")
    val pulsePhase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 6.28318f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 4000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "phase"
    )

    @Composable
    fun ControlDeck( modifierDeck: Modifier = Modifier) {
        Column(
            modifier = modifierDeck
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(DominaCard)
                .border(1.dp, Purple900.copy(alpha = 0.6f), RoundedCornerShape(12.dp))
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Timeline Scrubber
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = MediaLogic.formatDuration(currentTime.roundToInt()),
                    style = MaterialTheme.typography.labelMedium,
                    color = Purple200
                )
                Slider(
                    value = currentTime.coerceIn(0f, duration),
                    onValueChange = { currentTime = it },
                    valueRange = 0f..duration,
                    colors = SliderDefaults.colors(
                        thumbColor = palette.accentBright,
                        activeTrackColor = palette.accent,
                        inactiveTrackColor = Purple900.copy(alpha = 0.5f)
                    ),
                    modifier = Modifier
                        .weight(1f)
                        .height(24.dp)
                )
                Text(
                    text = MediaLogic.formatDuration(duration.roundToInt()),
                    style = MaterialTheme.typography.labelMedium,
                    color = Purple300.copy(alpha = 0.7f)
                )
            }

            // Transport, -10s / +10s, A-B Segment Loop, Next
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(palette.accent)
                        .clickable { isPlaying = !isPlaying }
                        .padding(horizontal = 14.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(5.dp)
                ) {
                    Icon(
                        imageVector = if (isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(15.dp)
                    )
                    Text(
                        text = if (isPlaying) "Pause" else "Play",
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.SemiBold,
                        color = Color.White
                    )
                }

                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(DominaCardInput)
                        .border(1.dp, Purple900.copy(alpha = 0.6f), RoundedCornerShape(8.dp))
                        .clickable { currentTime = (currentTime - 10f).coerceAtLeast(0f) }
                        .padding(horizontal = 10.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.Replay10,
                        contentDescription = "Rewind 10s",
                        tint = Purple200,
                        modifier = Modifier.size(14.dp)
                    )
                    Text("-10s", style = MaterialTheme.typography.labelSmall, color = Purple200)
                }

                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(DominaCardInput)
                        .border(1.dp, Purple900.copy(alpha = 0.6f), RoundedCornerShape(8.dp))
                        .clickable { currentTime = (currentTime + 10f).coerceAtMost(duration) }
                        .padding(horizontal = 10.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.Forward10,
                        contentDescription = "Forward 10s",
                        tint = Purple200,
                        modifier = Modifier.size(14.dp)
                    )
                    Text("+10s", style = MaterialTheme.typography.labelSmall, color = Purple200)
                }

                // A-B Segment Loop
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(DominaCardInput)
                        .border(1.dp, Purple900.copy(alpha = 0.6f), RoundedCornerShape(8.dp))
                        .padding(3.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = pointA?.let { "A:${MediaLogic.formatDuration(it.roundToInt())}" } ?: "Set A",
                        style = MaterialTheme.typography.labelSmall,
                        color = if (pointA != null) Color.White else Purple300,
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(if (pointA != null) palette.accent else Color.Transparent)
                            .clickable { pointA = currentTime }
                            .padding(horizontal = 8.dp, vertical = 5.dp)
                    )
                    Text(
                        text = pointB?.let { "B:${MediaLogic.formatDuration(it.roundToInt())}" } ?: "Set B",
                        style = MaterialTheme.typography.labelSmall,
                        color = if (pointB != null) Color.White else Purple300,
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(if (pointB != null) Color(0xFF9333EA) else Color.Transparent)
                            .clickable {
                                val a = pointA
                                if (a != null && currentTime > a) {
                                    pointB = currentTime
                                }
                            }
                            .padding(horizontal = 8.dp, vertical = 5.dp)
                    )
                    if (pointA != null || pointB != null) {
                        Icon(
                            imageVector = Icons.Filled.Close,
                            contentDescription = "Clear A-B loop",
                            tint = Rose400,
                            modifier = Modifier
                                .size(15.dp)
                                .clickable {
                                    pointA = null
                                    pointB = null
                                }
                        )
                    }
                }

                if (nextVideoItem != null && nextVideoItem.id != item.id) {
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(DominaCardInput)
                            .border(1.dp, Purple900.copy(alpha = 0.6f), RoundedCornerShape(8.dp))
                            .clickable { onSelectVideo(nextVideoItem) }
                            .padding(horizontal = 10.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.SkipNext,
                            contentDescription = "Next video",
                            tint = Purple100,
                            modifier = Modifier.size(15.dp)
                        )
                        Text("Next", style = MaterialTheme.typography.bodySmall, color = Purple100)
                    }
                }
            }

            // Volume Booster (0% - 200% BOOST) & Playback Speed Selector & Snapshot & Fullscreen
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(DominaCardInput)
                        .border(1.dp, Purple900.copy(alpha = 0.6f), RoundedCornerShape(8.dp))
                        .padding(horizontal = 10.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = if (isMuted || volumePercent == 0) Icons.AutoMirrored.Filled.VolumeOff else Icons.AutoMirrored.Filled.VolumeUp,
                        contentDescription = "Mute toggle",
                        tint = if (isMuted || volumePercent == 0) Rose400 else if (volumePercent > 100) Purple400 else Purple200,
                        modifier = Modifier
                            .size(16.dp)
                            .clickable { isMuted = !isMuted }
                    )
                    Slider(
                        value = if (isMuted) 0f else volumePercent.toFloat(),
                        onValueChange = {
                            isMuted = false
                            volumePercent = it.roundToInt()
                        },
                        valueRange = 0f..200f,
                        colors = SliderDefaults.colors(
                            thumbColor = palette.accentBright,
                            activeTrackColor = palette.accent
                        ),
                        modifier = Modifier
                            .width(96.dp)
                            .height(22.dp)
                    )
                    Text(
                        text = if (isMuted) "MUTED" else "$volumePercent%${if (volumePercent > 100) " BOOST" else ""}",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = if (volumePercent > 100) FontWeight.Bold else FontWeight.Normal,
                        color = if (volumePercent > 100) Purple400 else Purple200
                    )
                }

                // Speed & Snapshot & Fullscreen
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(DominaCardInput)
                            .border(1.dp, Purple900.copy(alpha = 0.6f), RoundedCornerShape(8.dp))
                            .padding(3.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Filled.FastForward,
                            contentDescription = null,
                            tint = Purple400,
                            modifier = Modifier
                                .padding(horizontal = 4.dp)
                                .size(13.dp)
                        )
                        listOf(0.5f, 1f, 1.25f, 1.5f, 2f).forEach { rate ->
                            val active = playbackSpeed == rate
                            Text(
                                text = "${if (rate == 1f) "1" else if (rate == 2f) "2" else rate}x",
                                style = MaterialTheme.typography.labelSmall,
                                color = if (active) Color.White else Purple300,
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(if (active) palette.accent else Color.Transparent)
                                    .clickable { playbackSpeed = rate }
                                    .padding(horizontal = 6.dp, vertical = 4.dp)
                            )
                        }
                    }

                    Icon(
                        imageVector = Icons.Filled.CameraAlt,
                        contentDescription = "Save frame snapshot",
                        tint = Purple200,
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(DominaCardInput)
                            .border(1.dp, Purple900.copy(alpha = 0.6f), RoundedCornerShape(8.dp))
                            .clickable {
                                snapshotNotice = "Frame snapshot saved at ${MediaLogic.formatDuration(currentTime.roundToInt())}"
                            }
                            .padding(8.dp)
                            .size(16.dp)
                    )

                    Icon(
                        imageVector = if (isFullscreen) Icons.Filled.FullscreenExit else Icons.Filled.Fullscreen,
                        contentDescription = "Toggle fullscreen",
                        tint = Purple200,
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(DominaCardInput)
                            .border(1.dp, Purple900.copy(alpha = 0.6f), RoundedCornerShape(8.dp))
                            .clickable { isFullscreen = !isFullscreen }
                            .padding(8.dp)
                            .size(16.dp)
                    )
                }
            }

            if (snapshotNotice != null) {
                Text(
                    text = snapshotNotice!!,
                    style = MaterialTheme.typography.labelSmall,
                    color = Emerald300
                )
            }
        }
    }

    if (isFullscreen) {
        Box(
            modifier = modifier
                .fillMaxSize()
                .background(Color.Black)
                .clickable { showFsControls = !showFsControls }
        ) {
            CinemaVideoViewport(
                item = item,
                isPlaying = isPlaying,
                currentTime = currentTime,
                pulsePhase = pulsePhase,
                panicBlur = panicBlur,
                onTogglePlay = { isPlaying = !isPlaying },
                modifier = Modifier.fillMaxSize()
            )
            if (showFsControls) {
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(16.dp)
                ) {
                    ControlDeck()
                }
            }
        }
        return
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF07040E))
            .verticalScroll(rememberScrollState())
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Top Stage Bar
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
                        .testTag("back_to_videos_btn")
                        .clickable { onClose() }
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = null,
                        tint = Purple100,
                        modifier = Modifier.size(15.dp)
                    )
                    Text("Back to Videos", style = MaterialTheme.typography.bodySmall, color = Purple100)
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (isLooping) palette.accent else DominaCard)
                            .border(
                                1.dp,
                                if (isLooping) Purple400 else Purple900.copy(alpha = 0.6f),
                                RoundedCornerShape(8.dp)
                            )
                            .clickable { isLooping = !isLooping }
                            .padding(horizontal = 10.dp, vertical = 7.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(5.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Repeat,
                            contentDescription = null,
                            tint = if (isLooping) Color.White else Purple200,
                            modifier = Modifier.size(13.dp)
                        )
                        Text(
                            text = "Loop: ${if (isLooping) "On" else "Off"}",
                            style = MaterialTheme.typography.bodySmall,
                            color = if (isLooping) Color.White else Purple200
                        )
                    }

                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (autoPlayNext) Color(0xFF9333EA) else DominaCard)
                            .border(
                                1.dp,
                                if (autoPlayNext) Purple400 else Purple900.copy(alpha = 0.6f),
                                RoundedCornerShape(8.dp)
                            )
                            .clickable { autoPlayNext = !autoPlayNext }
                            .padding(horizontal = 10.dp, vertical = 7.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(5.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.SkipNext,
                            contentDescription = null,
                            tint = if (autoPlayNext) Color.White else Purple200,
                            modifier = Modifier.size(13.dp)
                        )
                        Text(
                            text = "Auto-Next: ${if (autoPlayNext) "On" else "Off"}",
                            style = MaterialTheme.typography.bodySmall,
                            color = if (autoPlayNext) Color.White else Purple200
                        )
                    }
                }
            }

            // Sleek Video Viewport
            CinemaVideoViewport(
                item = item,
                isPlaying = isPlaying,
                currentTime = currentTime,
                pulsePhase = pulsePhase,
                panicBlur = panicBlur,
                onTogglePlay = { isPlaying = !isPlaying },
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(16f / 9f)
                    .clip(RoundedCornerShape(12.dp))
                    .border(1.dp, Purple900.copy(alpha = 0.6f), RoundedCornerShape(12.dp))
            )

            // Control Deck
            ControlDeck()
        }

        // Video Metadata Sidebar
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(DominaSidebarBg)
                .padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "CINEMA STUDIO INSPECTOR",
                    style = MaterialTheme.typography.labelMedium,
                    color = Purple400
                )
                if (onToggleFavorite != null) {
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(20.dp))
                            .background(if (item.isFavorite) palette.accent else DominaCardInner)
                            .border(
                                1.dp,
                                if (item.isFavorite) Purple400 else Purple800.copy(alpha = 0.6f),
                                RoundedCornerShape(20.dp)
                            )
                            .clickable { onToggleFavorite(item.id, !item.isFavorite) }
                            .padding(horizontal = 10.dp, vertical = 5.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = if (item.isFavorite) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
                            contentDescription = null,
                            tint = if (item.isFavorite) Color.White else Purple300,
                            modifier = Modifier.size(13.dp)
                        )
                        Text(
                            text = if (item.isFavorite) "Favorited" else "Favorite",
                            style = MaterialTheme.typography.bodySmall,
                            color = if (item.isFavorite) Color.White else Purple300
                        )
                    }
                }
            }

            Text(
                text = item.title,
                style = MaterialTheme.typography.displaySmall,
                color = Purple50
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Vault Rating", style = MaterialTheme.typography.bodyMedium, color = Purple300)
                StarRating(
                    rating = item.rating,
                    onChange = { r -> onRate(item.id, r) },
                    size = StarRatingSize.LG,
                    itemTitle = item.title
                )
            }

            HorizontalDivider(color = Purple900.copy(alpha = 0.5f))

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Stream & Container Telemetry", style = MaterialTheme.typography.titleSmall, color = Purple200)
                SpecRow("Filename", item.filename)
                SpecRow("Resolution", "${item.width} × ${item.height}")
                SpecRow("Container", item.format)
                SpecRow("Duration", MediaLogic.formatDuration(duration.roundToInt()))
                SpecRow("File Size", MediaLogic.formatFileSize(item.sizeBytes))

                Text("Local Storage Path", style = MaterialTheme.typography.bodySmall, color = Purple300)
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(6.dp))
                        .background(DominaCardInner)
                        .border(1.dp, Purple900.copy(alpha = 0.6f), RoundedCornerShape(6.dp))
                        .padding(10.dp)
                ) {
                    Text(
                        text = item.localPath,
                        style = MaterialTheme.typography.labelSmall,
                        color = Purple200
                    )
                }
            }

            HorizontalDivider(color = Purple900.copy(alpha = 0.5f))

            // Editable Tags
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = "Assigned Tags (${item.tags.size})",
                    style = MaterialTheme.typography.titleSmall,
                    color = Purple200
                )
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
                                .padding(horizontal = 9.dp, vertical = 5.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text("#$tag", style = MaterialTheme.typography.labelSmall, color = Purple100)
                            Icon(
                                imageVector = Icons.Filled.Close,
                                contentDescription = "Remove tag $tag",
                                tint = Purple400,
                                modifier = Modifier
                                    .size(12.dp)
                                    .clickable { onUpdateTags(item.id, item.tags - tag) }
                            )
                        }
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(8.dp))
                            .background(DominaCardInner)
                            .border(1.dp, Purple900.copy(alpha = 0.6f), RoundedCornerShape(8.dp))
                            .padding(horizontal = 10.dp, vertical = 8.dp)
                    ) {
                        if (newTagInput.isEmpty()) {
                            Text(
                                text = "Add video tag...",
                                style = MaterialTheme.typography.bodySmall,
                                color = Purple400.copy(alpha = 0.5f)
                            )
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

                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(palette.accent)
                            .clickable {
                                val cleaned = newTagInput.trim().lowercase().removePrefix("#").replace(Regex("\\s+"), "-")
                                if (cleaned.isNotEmpty() && !item.tags.contains(cleaned)) {
                                    onUpdateTags(item.id, item.tags + cleaned)
                                }
                                newTagInput = ""
                            }
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Add,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(14.dp)
                        )
                        Text(
                            text = "Add",
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.SemiBold,
                            color = Color.White
                        )
                    }
                }
            }

            // Up Next in Queue Preview & Delete
            if (nextVideoItem != null && nextVideoItem.id != item.id) {
                HorizontalDivider(color = Purple900.copy(alpha = 0.5f))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(DominaCardInner)
                        .border(1.dp, Purple900.copy(alpha = 0.6f), RoundedCornerShape(8.dp))
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("UP NEXT IN QUEUE", style = MaterialTheme.typography.labelSmall, color = Purple400)
                        Text(
                            text = nextVideoItem.title,
                            style = MaterialTheme.typography.bodyMedium,
                            color = Purple100,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    Text(
                        text = "Play Now",
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.SemiBold,
                        color = Color.White,
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(palette.accent)
                            .clickable { onSelectVideo(nextVideoItem) }
                            .padding(horizontal = 10.dp, vertical = 6.dp)
                    )
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(Rose950.copy(alpha = 0.4f))
                    .border(1.dp, Rose800.copy(alpha = 0.6f), RoundedCornerShape(8.dp))
                    .clickable { onDelete(item.id) }
                    .padding(vertical = 10.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Filled.Delete,
                    contentDescription = null,
                    tint = Rose300,
                    modifier = Modifier.size(15.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "Remove from Vault",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                    color = Rose300
                )
            }
        }
    }
}

@Composable
private fun CinemaVideoViewport(
    item: MediaItem,
    isPlaying: Boolean,
    currentTime: Float,
    pulsePhase: Float,
    panicBlur: Boolean,
    onTogglePlay: () -> Unit,
    modifier: Modifier = Modifier
) {
    val palette = LocalDominaPalette.current
    val thumbModel = rememberThumbModel(item.thumbnailUrl)

    Box(
        modifier = modifier
            .background(Color.Black)
            .clickable { onTogglePlay() },
        contentAlignment = Alignment.Center
    ) {
        if (thumbModel != null) {
            AsyncImage(
                model = thumbModel,
                contentDescription = item.title,
                contentScale = ContentScale.Crop,
                alpha = 0.65f,
                modifier = Modifier
                    .fillMaxSize()
                    .then(if (panicBlur) Modifier.blur(22.dp) else Modifier)
            )
        }

        // Dynamic Cinema Studio Visualizer Canvas
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .then(if (panicBlur) Modifier.blur(22.dp) else Modifier)
        ) {
            val w = size.width
            val h = size.height
            val cx = w * 0.5f
            val cy = h * 0.5f
            val baseR = minOf(w, h) * 0.25f
            val dynamicOffset = if (isPlaying) sin(pulsePhase + currentTime) * 18f else 0f

            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        palette.accentBright.copy(alpha = 0.35f),
                        palette.accent.copy(alpha = 0.12f),
                        Color.Transparent
                    ),
                    center = Offset(cx, cy),
                    radius = (baseR * 1.8f + dynamicOffset).coerceAtLeast(10f)
                ),
                radius = (baseR * 1.8f + dynamicOffset).coerceAtLeast(10f),
                center = Offset(cx, cy)
            )

            drawCircle(
                color = palette.accentBright.copy(alpha = 0.65f),
                radius = (baseR + dynamicOffset * 0.5f).coerceAtLeast(10f),
                center = Offset(cx, cy),
                style = Stroke(width = 3f)
            )

            // Waveform bars at bottom
            val barCount = 28
            val spacing = w / (barCount + 4)
            for (i in 0 until barCount) {
                val x = spacing * (i + 2)
                val wave = if (isPlaying) {
                    (sin(pulsePhase * 2f + i * 0.45f + currentTime * 2f) * 0.5f + 0.5f)
                } else 0.2f
                val barH = h * (0.04f + wave * 0.18f)
                drawLine(
                    color = palette.accentBright.copy(alpha = 0.6f),
                    start = Offset(x, h - 18f),
                    end = Offset(x, h - 18f - barH),
                    strokeWidth = 5f
                )
            }
        }

        if (!isPlaying) {
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .clip(CircleShape)
                    .background(palette.accent.copy(alpha = 0.92f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Filled.PlayArrow,
                    contentDescription = "Play video",
                    tint = Color.White,
                    modifier = Modifier.size(34.dp)
                )
            }
        }
    }
}
