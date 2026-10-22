package com.example.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTransformGestures
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.filled.RotateLeft
import androidx.compose.material.icons.automirrored.filled.RotateRight
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Flip
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.material.icons.filled.FullscreenExit
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.ZoomIn
import androidx.compose.material.icons.filled.ZoomOut
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
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
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
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
import com.example.ui.theme.DominaElevatedSurface
import com.example.ui.theme.DominaSidebarBg
import com.example.ui.theme.Emerald300
import com.example.ui.theme.Emerald950
import com.example.ui.theme.LocalDominaPalette
import com.example.ui.theme.Purple100
import com.example.ui.theme.Purple200
import com.example.ui.theme.Purple300
import com.example.ui.theme.Purple400
import com.example.ui.theme.Purple50
import com.example.ui.theme.Purple800
import com.example.ui.theme.Purple900
import com.example.ui.theme.Rose300
import com.example.ui.theme.Rose800
import com.example.ui.theme.Rose950
import com.example.utils.MediaLogic
import kotlinx.coroutines.delay
import kotlin.math.roundToInt

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ImageDetailsView(
    item: MediaItem,
    onClose: () -> Unit,
    onPrev: (() -> Unit)? = null,
    onNext: (() -> Unit)? = null,
    hasPrev: Boolean = false,
    hasNext: Boolean = false,
    onRate: (String, Int) -> Unit,
    onToggleFavorite: ((String, Boolean) -> Unit)? = null,
    onUpdateTags: (String, List<String>) -> Unit,
    onUpdateNotes: ((String, String) -> Unit)? = null,
    onDelete: (String) -> Unit,
    onOpenReaderAtPage: (() -> Unit)? = null,
    isCollectionPage: Boolean = false,
    slideshowIntervalSeconds: Int = 4,
    panicBlur: Boolean = false,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val palette = LocalDominaPalette.current

    var zoom by remember(item.id) { mutableFloatStateOf(1f) }
    var pan by remember(item.id) { mutableStateOf(Offset.Zero) }
    var rotation by remember(item.id) { mutableIntStateOf(0) }
    var flipH by remember(item.id) { mutableStateOf(false) }
    var flipV by remember(item.id) { mutableStateOf(false) }

    // Optical Tone Adjustments
    var brightness by remember { mutableFloatStateOf(100f) }
    var contrast by remember { mutableFloatStateOf(100f) }
    var saturation by remember { mutableFloatStateOf(100f) }
    var sepia by remember { mutableFloatStateOf(0f) }
    var invert by remember { mutableFloatStateOf(0f) }
    var softBlur by remember { mutableFloatStateOf(0f) }
    var vignette by remember { mutableFloatStateOf(0f) }
    var violetTint by remember { mutableFloatStateOf(0f) }
    var activeTonePreset by remember { mutableStateOf("normal") }

    var newTagInput by remember { mutableStateOf("") }
    var notesDraft by remember(item.id, item.notes) { mutableStateOf(item.notes) }
    var isFullscreen by remember { mutableStateOf(false) }
    var slideshowPlaying by remember { mutableStateOf(false) }
    var slideshowSpeed by remember { mutableIntStateOf(slideshowIntervalSeconds.coerceIn(1, 10)) }
    var speedMenuOpen by remember { mutableStateOf(false) }
    var copyNotice by remember { mutableStateOf<String?>(null) }

    fun applyTonePreset(preset: String) {
        activeTonePreset = preset
        when (preset) {
            "normal" -> {
                brightness = 100f; contrast = 100f; saturation = 100f
                sepia = 0f; invert = 0f; softBlur = 0f; vignette = 0f; violetTint = 0f
            }
            "noir" -> {
                brightness = 95f; contrast = 145f; saturation = 0f
                sepia = 0f; invert = 0f; softBlur = 0f; vignette = 60f; violetTint = 0f
            }
            "amethyst" -> {
                brightness = 100f; contrast = 125f; saturation = 135f
                sepia = 0f; invert = 0f; softBlur = 0f; vignette = 35f; violetTint = 50f
            }
            "blood" -> {
                brightness = 95f; contrast = 135f; saturation = 150f
                sepia = 15f; invert = 0f; softBlur = 0f; vignette = 45f; violetTint = 25f
            }
            "sepia" -> {
                brightness = 105f; contrast = 110f; saturation = 90f
                sepia = 75f; invert = 0f; softBlur = 0f; vignette = 25f; violetTint = 0f
            }
            "vivid" -> {
                brightness = 105f; contrast = 130f; saturation = 170f
                sepia = 0f; invert = 0f; softBlur = 0f; vignette = 0f; violetTint = 0f
            }
            "negative" -> {
                brightness = 100f; contrast = 120f; saturation = 100f
                sepia = 0f; invert = 100f; softBlur = 0f; vignette = 0f; violetTint = 0f
            }
        }
    }

    // Automatic Slideshow Timer
    LaunchedEffect(slideshowPlaying, slideshowSpeed, item.id) {
        if (slideshowPlaying && onNext != null) {
            delay(slideshowSpeed.coerceAtLeast(1) * 1000L)
            onNext()
        }
    }

    val colorMatrix = remember(brightness, contrast, saturation, sepia, invert) {
        val cm = ColorMatrix()
        cm.setToSaturation(saturation / 100f)

        val c = contrast / 100f
        val b = (brightness - 100f) * 2.55f
        val scaleTranslate = ColorMatrix(
            floatArrayOf(
                c, 0f, 0f, 0f, b,
                0f, c, 0f, 0f, b,
                0f, 0f, c, 0f, b,
                0f, 0f, 0f, 1f, 0f
            )
        )
        cm.timesAssign(scaleTranslate)

        if (sepia > 0f) {
            val s = sepia / 100f
            val sepiaMatrix = ColorMatrix(
                floatArrayOf(
                    1f - 0.607f * s, 0.769f * s, 0.189f * s, 0f, 0f,
                    0.349f * s, 1f - 0.314f * s, 0.168f * s, 0f, 0f,
                    0.272f * s, 0.534f * s, 1f - 0.869f * s, 0f, 0f,
                    0f, 0f, 0f, 1f, 0f
                )
            )
            cm.timesAssign(sepiaMatrix)
        }

        if (invert >= 50f) {
            val invMatrix = ColorMatrix(
                floatArrayOf(
                    -1f, 0f, 0f, 0f, 255f,
                    0f, -1f, 0f, 0f, 255f,
                    0f, 0f, -1f, 0f, 255f,
                    0f, 0f, 0f, 1f, 0f
                )
            )
            cm.timesAssign(invMatrix)
        }
        cm
    }

    if (isFullscreen) {
        // Pure Fullscreen Viewport (hides all UI, tap or Exit button to return)
        Box(
            modifier = modifier
                .fillMaxSize()
                .background(Color.Black)
                .pointerInput(Unit) {
                    detectTransformGestures { _, panChange, zoomChange, _ ->
                        zoom = (zoom * zoomChange).coerceIn(0.4f, 6f)
                        pan += panChange
                    }
                }
        ) {
            val model = rememberThumbModel(item.mediaUrl.ifBlank { item.thumbnailUrl })
            if (model != null) {
                AsyncImage(
                    model = model,
                    contentDescription = item.title,
                    contentScale = ContentScale.Fit,
                    colorFilter = ColorFilter.colorMatrix(colorMatrix),
                    modifier = Modifier
                        .fillMaxSize()
                        .graphicsLayer {
                            scaleX = (if (flipH) -1f else 1f) * zoom
                            scaleY = (if (flipV) -1f else 1f) * zoom
                            translationX = pan.x
                            translationY = pan.y
                            rotationZ = rotation.toFloat()
                        }
                        .then(
                            if (panicBlur) Modifier.blur(24.dp)
                            else if (softBlur > 0f) Modifier.blur(softBlur.dp)
                            else Modifier
                        )
                )
            }

            Row(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(16.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(DominaCard.copy(alpha = 0.85f))
                    .border(1.dp, Purple800, RoundedCornerShape(8.dp))
                    .clickable { isFullscreen = false }
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Icon(
                    imageVector = Icons.Filled.FullscreenExit,
                    contentDescription = "Exit fullscreen",
                    tint = Color.White,
                    modifier = Modifier.size(16.dp)
                )
                Text("Exit Fullscreen", style = MaterialTheme.typography.bodySmall, color = Color.White)
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
        // Top Floating Viewport Bar
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(DominaCard)
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(DominaElevatedSurface)
                            .border(1.dp, Purple800.copy(alpha = 0.7f), RoundedCornerShape(8.dp))
                            .testTag("back_to_gallery_btn")
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
                        Text(
                            text = if (isCollectionPage) "Back to Collection" else "Back to Gallery",
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Medium,
                            color = Purple100
                        )
                    }

                    if (onOpenReaderAtPage != null) {
                        Row(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(palette.accent)
                                .border(1.dp, Purple400.copy(alpha = 0.7f), RoundedCornerShape(8.dp))
                                .clickable { onOpenReaderAtPage() }
                                .padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.MenuBook,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(14.dp)
                            )
                            Text(
                                text = "Open in Reader",
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.SemiBold,
                                color = Color.White
                            )
                        }
                    }
                }

                Icon(
                    imageVector = Icons.Filled.Fullscreen,
                    contentDescription = "Toggle fullscreen",
                    tint = Purple200,
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(DominaElevatedSurface)
                        .border(1.dp, Purple800.copy(alpha = 0.7f), RoundedCornerShape(8.dp))
                        .clickable { isFullscreen = true }
                        .padding(8.dp)
                        .size(18.dp)
                )
            }

            // Transform & Slideshow Toolbar
            FlowRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(DominaCardInner)
                    .border(1.dp, Purple900.copy(alpha = 0.6f), RoundedCornerShape(8.dp))
                    .padding(6.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Icon(
                    imageVector = Icons.Filled.ZoomOut,
                    contentDescription = "Zoom out",
                    tint = Purple200,
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .clickable { zoom = (zoom - 0.25f).coerceAtLeast(0.4f) }
                        .padding(6.dp)
                        .size(16.dp)
                )
                Text(
                    text = "${(zoom * 100).roundToInt()}%",
                    style = MaterialTheme.typography.labelMedium,
                    color = Purple100,
                    modifier = Modifier.align(Alignment.CenterVertically)
                )
                Icon(
                    imageVector = Icons.Filled.ZoomIn,
                    contentDescription = "Zoom in",
                    tint = Purple200,
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .clickable { zoom = (zoom + 0.25f).coerceAtMost(6f) }
                        .padding(6.dp)
                        .size(16.dp)
                )
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.RotateLeft,
                    contentDescription = "Rotate counterclockwise",
                    tint = Purple200,
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .clickable { rotation = (rotation - 90 + 360) % 360 }
                        .padding(6.dp)
                        .size(16.dp)
                )
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.RotateRight,
                    contentDescription = "Rotate clockwise",
                    tint = Purple200,
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .clickable { rotation = (rotation + 90) % 360 }
                        .padding(6.dp)
                        .size(16.dp)
                )
                Text(
                    text = "Flip H",
                    style = MaterialTheme.typography.labelSmall,
                    color = if (flipH) Color.White else Purple200,
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(if (flipH) palette.accent else Color.Transparent)
                        .clickable { flipH = !flipH }
                        .padding(horizontal = 8.dp, vertical = 5.dp)
                )
                Text(
                    text = "Flip V",
                    style = MaterialTheme.typography.labelSmall,
                    color = if (flipV) Color.White else Purple200,
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(if (flipV) palette.accent else Color.Transparent)
                        .clickable { flipV = !flipV }
                        .padding(horizontal = 8.dp, vertical = 5.dp)
                )

                if (onNext != null) {
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(DominaElevatedSurface)
                            .border(1.dp, Purple800.copy(alpha = 0.6f), RoundedCornerShape(6.dp))
                            .padding(horizontal = 6.dp, vertical = 3.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
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
                            Text("Slideshow", style = MaterialTheme.typography.labelSmall, color = Color.White)
                        }

                        Box {
                            Text(
                                text = "${slideshowSpeed}s ▾",
                                style = MaterialTheme.typography.labelSmall,
                                color = Purple200,
                                modifier = Modifier
                                    .clickable { speedMenuOpen = true }
                                    .padding(horizontal = 4.dp, vertical = 2.dp)
                            )
                            DropdownMenu(
                                expanded = speedMenuOpen,
                                onDismissRequest = { speedMenuOpen = false },
                                modifier = Modifier.background(Color(0xFF140A26))
                            ) {
                                listOf(1, 2, 3, 4, 5, 7, 10).forEach { s ->
                                    DropdownMenuItem(
                                        text = { Text("${s}s", color = Purple100) },
                                        onClick = {
                                            slideshowSpeed = s
                                            speedMenuOpen = false
                                        }
                                    )
                                }
                            }
                        }
                    }
                }

                Text(
                    text = "Reset",
                    style = MaterialTheme.typography.labelSmall,
                    color = Purple200,
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .clickable {
                            zoom = 1f
                            pan = Offset.Zero
                            rotation = 0
                            flipH = false
                            flipV = false
                            applyTonePreset("normal")
                        }
                        .padding(horizontal = 8.dp, vertical = 5.dp)
                )
            }
        }

        // Interactive Pan & Zoom Image Canvas
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(380.dp)
                .background(Color(0xFF05020A))
                .pointerInput(Unit) {
                    detectTransformGestures { _, panChange, zoomChange, _ ->
                        zoom = (zoom * zoomChange).coerceIn(0.4f, 6f)
                        pan += panChange
                    }
                },
            contentAlignment = Alignment.Center
        ) {
            val model = rememberThumbModel(item.mediaUrl.ifBlank { item.thumbnailUrl })
            if (model != null) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    AsyncImage(
                        model = model,
                        contentDescription = item.title,
                        contentScale = ContentScale.Fit,
                        colorFilter = ColorFilter.colorMatrix(colorMatrix),
                        modifier = Modifier
                            .fillMaxSize()
                            .graphicsLayer {
                                scaleX = (if (flipH) -1f else 1f) * zoom
                                scaleY = (if (flipV) -1f else 1f) * zoom
                                translationX = pan.x
                                translationY = pan.y
                                rotationZ = rotation.toFloat()
                            }
                            .then(
                                if (panicBlur) Modifier.blur(22.dp)
                                else if (softBlur > 0f) Modifier.blur(softBlur.dp)
                                else Modifier
                            )
                    )

                    if (vignette > 0f) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(
                                    Brush.radialGradient(
                                        listOf(
                                            Color.Transparent,
                                            Color.Black.copy(alpha = (vignette / 100f).coerceIn(0f, 0.9f))
                                        )
                                    )
                                )
                        )
                    }

                    if (violetTint > 0f) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(
                                    Color(0xFF8B5CF6).copy(alpha = (violetTint * 0.45f / 100f).coerceIn(0f, 0.6f))
                                )
                        )
                    }
                }
            }

            if ((hasPrev || isCollectionPage) && onPrev != null) {
                Box(
                    modifier = Modifier
                        .align(Alignment.CenterStart)
                        .padding(start = 10.dp)
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(DominaCard.copy(alpha = 0.85f))
                        .border(1.dp, Purple800, CircleShape)
                        .clickable { onPrev() },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                        contentDescription = "Previous image",
                        tint = Color.White
                    )
                }
            }

            if ((hasNext || isCollectionPage) && onNext != null) {
                Box(
                    modifier = Modifier
                        .align(Alignment.CenterEnd)
                        .padding(end = 10.dp)
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(DominaCard.copy(alpha = 0.85f))
                        .border(1.dp, Purple800, CircleShape)
                        .clickable { onNext() },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                        contentDescription = "Next image",
                        tint = Color.White
                    )
                }
            }
        }

        // Metadata Inspector Panel
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(DominaSidebarBg)
                .padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            // Title, Favorite & Vault Rating
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "IMAGE INSPECTOR",
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

                HorizontalDivider(color = Purple900.copy(alpha = 0.5f))

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
            }

            HorizontalDivider(color = Purple900.copy(alpha = 0.5f))

            // Viewport Tone Adjustments
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Tune,
                            contentDescription = null,
                            tint = Purple400,
                            modifier = Modifier.size(15.dp)
                        )
                        Text(
                            text = "Viewport Tone Adjustments",
                            style = MaterialTheme.typography.titleSmall,
                            color = Purple200
                        )
                    }
                    Text(
                        text = "Reset All",
                        style = MaterialTheme.typography.bodySmall,
                        color = Purple400,
                        modifier = Modifier.clickable { applyTonePreset("normal") }
                    )
                }

                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    listOf(
                        "normal" to "Default",
                        "noir" to "Gothic Noir",
                        "amethyst" to "Amethyst",
                        "blood" to "Crimson",
                        "sepia" to "Sepia",
                        "vivid" to "Vivid",
                        "negative" to "Invert"
                    ).forEach { (id, label) ->
                        val active = activeTonePreset == id
                        Text(
                            text = label,
                            style = MaterialTheme.typography.bodySmall,
                            color = if (active) Color.White else Purple300,
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(if (active) palette.accent else DominaCardInner)
                                .border(
                                    1.dp,
                                    if (active) Purple400 else Purple900.copy(alpha = 0.6f),
                                    RoundedCornerShape(6.dp)
                                )
                                .clickable { applyTonePreset(id) }
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }

                ToneSliderRow("Brightness", "${brightness.roundToInt()}%", brightness, 25f..200f) {
                    brightness = it; activeTonePreset = "custom"
                }
                ToneSliderRow("Contrast", "${contrast.roundToInt()}%", contrast, 25f..250f) {
                    contrast = it; activeTonePreset = "custom"
                }
                ToneSliderRow("Saturation", "${saturation.roundToInt()}%", saturation, 0f..250f) {
                    saturation = it; activeTonePreset = "custom"
                }
                ToneSliderRow("Sepia Warmth", "${sepia.roundToInt()}%", sepia, 0f..100f) {
                    sepia = it; activeTonePreset = "custom"
                }
                ToneSliderRow("Vignette", "${vignette.roundToInt()}%", vignette, 0f..100f) {
                    vignette = it; activeTonePreset = "custom"
                }
                ToneSliderRow("Velvet Violet Tint", "${violetTint.roundToInt()}%", violetTint, 0f..100f) {
                    violetTint = it; activeTonePreset = "custom"
                }
            }

            HorizontalDivider(color = Purple900.copy(alpha = 0.5f))

            // File Specifications
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("File Specifications", style = MaterialTheme.typography.titleSmall, color = Purple200)
                SpecRow("Filename", item.filename)
                SpecRow("Resolution", "${item.width} × ${item.height} px")
                SpecRow("Format", item.format)
                SpecRow("File Size", MediaLogic.formatFileSize(item.sizeBytes))
                SpecRow("Date Indexed", item.dateAdded.take(10))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Local Storage Path", style = MaterialTheme.typography.bodySmall, color = Purple300)
                    Row(
                        modifier = Modifier.clickable {
                            val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
                            cm?.setPrimaryClip(ClipData.newPlainText("path", item.localPath))
                            copyNotice = "Copied local path!"
                        },
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.ContentCopy,
                            contentDescription = null,
                            tint = Purple400,
                            modifier = Modifier.size(12.dp)
                        )
                        Text(
                            text = copyNotice ?: "Copy Path",
                            style = MaterialTheme.typography.bodySmall,
                            color = Purple400
                        )
                    }
                }

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

            // Editable Tags Section
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
                                text = "Add new tag (e.g. violet)...",
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
                            text = "Add Tag",
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.SemiBold,
                            color = Color.White
                        )
                    }
                }
            }

            // Curator Notes
            if (onUpdateNotes != null) {
                HorizontalDivider(color = Purple900.copy(alpha = 0.5f))
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Curator Notes", style = MaterialTheme.typography.titleSmall, color = Purple200)
                        if (notesDraft != item.notes) {
                            Text(
                                text = "Save Notes",
                                style = MaterialTheme.typography.labelSmall,
                                color = Purple400,
                                modifier = Modifier.clickable { onUpdateNotes(item.id, notesDraft) }
                            )
                        }
                    }
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(DominaCardInner)
                            .border(1.dp, Purple900.copy(alpha = 0.6f), RoundedCornerShape(8.dp))
                            .padding(10.dp)
                    ) {
                        if (notesDraft.isEmpty()) {
                            Text(
                                text = "Add private notes about this work...",
                                style = MaterialTheme.typography.bodySmall,
                                color = Purple400.copy(alpha = 0.45f)
                            )
                        }
                        BasicTextField(
                            value = notesDraft,
                            onValueChange = {
                                notesDraft = it
                                onUpdateNotes(item.id, it)
                            },
                            textStyle = MaterialTheme.typography.bodySmall.copy(color = Purple100),
                            cursorBrush = SolidColor(palette.accentBright),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(56.dp)
                        )
                    }
                }
            }

            HorizontalDivider(color = Purple900.copy(alpha = 0.5f))

            // Remove from Vault Button
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
private fun ToneSliderRow(
    label: String,
    valueText: String,
    value: Float,
    range: ClosedFloatingPointRange<Float>,
    onValueChange: (Float) -> Unit
) {
    val palette = LocalDominaPalette.current
    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(label, style = MaterialTheme.typography.bodySmall, color = Purple300.copy(alpha = 0.85f))
            Text(valueText, style = MaterialTheme.typography.labelSmall, color = Purple200)
        }
        Slider(
            value = value,
            onValueChange = onValueChange,
            valueRange = range,
            colors = SliderDefaults.colors(
                thumbColor = palette.accentBright,
                activeTrackColor = palette.accent,
                inactiveTrackColor = Purple900.copy(alpha = 0.5f)
            ),
            modifier = Modifier.height(26.dp)
        )
    }
}

@Composable
fun SpecRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = Purple300.copy(alpha = 0.75f)
        )
        Text(
            text = value,
            style = MaterialTheme.typography.labelSmall,
            color = Purple100,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}
