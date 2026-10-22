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
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.CreateNewFolder
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.Keyboard
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.PowerSettingsNew
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.Upload
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
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
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.model.AppearanceConfig
import com.example.model.DirectoryConfig
import com.example.model.MediaItem
import com.example.model.MediaType
import com.example.model.ReaderDirection
import com.example.model.ReaderFitMode
import com.example.ui.components.DirectoryPickerModal
import com.example.ui.theme.AmethystAccent
import com.example.ui.theme.CrimsonAccent
import com.example.ui.theme.DominaCard
import com.example.ui.theme.DominaCardInner
import com.example.ui.theme.DominaElevatedSurface
import com.example.ui.theme.Emerald400
import com.example.ui.theme.IndigoAccent
import com.example.ui.theme.LocalDominaPalette
import com.example.ui.theme.Purple100
import com.example.ui.theme.Purple200
import com.example.ui.theme.Purple300
import com.example.ui.theme.Purple400
import com.example.ui.theme.Purple50
import com.example.ui.theme.Purple800
import com.example.ui.theme.Purple900
import com.example.ui.theme.Purple950
import com.example.ui.theme.Rose300
import com.example.ui.theme.Rose400
import com.example.ui.theme.Rose700
import com.example.ui.theme.Rose800
import com.example.ui.theme.Rose900
import com.example.ui.theme.RoyalVioletAccent
import kotlin.math.roundToInt

private data class DirectoryFieldSpec(
    val key: String,
    val label: String,
    val type: MediaType
)

private data class ShortcutGroup(
    val title: String,
    val items: List<Pair<String, String>>
)

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SettingsView(
    directories: DirectoryConfig,
    appearance: AppearanceConfig,
    mediaItems: List<MediaItem>,
    onUpdateDirectories: (DirectoryConfig) -> Unit,
    onUpdateAppearance: (AppearanceConfig) -> Unit,
    onTriggerServerScan: (DirectoryConfig, (Int) -> Unit) -> Unit,
    onMountFolder: (MediaType) -> Unit,
    onExportDatabaseJson: ((String) -> Unit) -> Unit,
    onImportDatabaseJson: (String, (Boolean) -> Unit) -> Unit,
    onClearLibrary: (() -> Unit) -> Unit,
    onPanicClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val palette = LocalDominaPalette.current

    var activeTab by remember { mutableStateOf("general") } // general, shortcuts, architecture
    var dirDrafts by remember(directories) { mutableStateOf(directories) }
    var isScanning by remember { mutableStateOf(false) }
    var scanProgress by remember { mutableIntStateOf(0) }
    var scanStatusText by remember { mutableStateOf<String?>(null) }
    var dbFeedback by remember { mutableStateOf<String?>(null) }
    var showImportJsonBox by remember { mutableStateOf(false) }
    var importJsonDraft by remember { mutableStateOf("") }

    var pickerTarget by remember { mutableStateOf<Pair<String, String>?>(null) }

    val directoryFields = listOf(
        DirectoryFieldSpec("imagesPath", "Images Root Directory (Recursive)", MediaType.IMAGE),
        DirectoryFieldSpec("videosPath", "Videos Root Directory (Recursive)", MediaType.VIDEO),
        DirectoryFieldSpec("comicsPath", "Comics Library Directory (Subfolder per Comic)", MediaType.COMIC),
        DirectoryFieldSpec("imageSetsPath", "Image Sets Directory (Subfolder per Set)", MediaType.IMAGESET)
    )

    val shortcutGroups = listOf(
        ShortcutGroup(
            title = "Global & Privacy Shortcuts",
            items = listOf(
                "Ctrl + D / Tap" to "Panic Close (immediately conceals and closes vault)",
                "Ctrl + K / Tap" to "Open Global Quick Search modal",
                "Shift + L / Tap" to "Engage / disengage full-screen Privacy Lock",
                "1 – 5" to "Assign 1 to 5 star rating to active item"
            )
        ),
        ShortcutGroup(
            title = "Image Details Inspector",
            items = listOf(
                "Pinch / Wheel" to "Smooth zoom in and zoom out (40% – 600%)",
                "Touch + Drag" to "Pan around zoomed image canvas",
                "← / → Buttons" to "Navigate to previous or next image",
                "F / Fullscreen" to "Toggle pure fullscreen image view (hides all UI)",
                "R / Rotate" to "Rotate image 90° clockwise",
                "0 / Reset" to "Reset zoom, pan, and rotation to default",
                "Back / Esc" to "Exit fullscreen or return to gallery"
            )
        ),
        ShortcutGroup(
            title = "Video Studio Player",
            items = listOf(
                "Tap / Spacebar" to "Play / Pause video stream",
                "-10s / +10s" to "Seek backward / forward by 10 seconds",
                "Set A / Set B" to "Engage A–B segment loop points",
                "L / Loop" to "Toggle infinite video loop",
                "1 – 5" to "Rate current video inline"
            )
        ),
        ShortcutGroup(
            title = "Comics & Image Sets Reader",
            items = listOf(
                "← / → Buttons" to "Flip to previous or next page (respects LTR / RTL)",
                "W / Fit Width" to "Switch display mode to Fit to Width",
                "H / Fit Height" to "Switch display mode to Fit to Height",
                "D / Double-Page" to "Switch display mode to Double-Page Spread",
                "F / Fullscreen" to "Toggle fullscreen reader mode",
                "Back / Esc" to "Exit fullscreen or return to collection overview"
            )
        )
    )

    fun pathForKey(key: String): String = when (key) {
        "imagesPath" -> dirDrafts.imagesPath
        "videosPath" -> dirDrafts.videosPath
        "comicsPath" -> dirDrafts.comicsPath
        else -> dirDrafts.imageSetsPath
    }

    fun updatePathForKey(key: String, newValue: String): DirectoryConfig = when (key) {
        "imagesPath" -> dirDrafts.copy(imagesPath = newValue)
        "videosPath" -> dirDrafts.copy(videosPath = newValue)
        "comicsPath" -> dirDrafts.copy(comicsPath = newValue)
        else -> dirDrafts.copy(imageSetsPath = newValue)
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 20.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        // Header & Section Switcher
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(
                text = "Settings",
                style = MaterialTheme.typography.displayMedium,
                color = Purple50
            )
            Text(
                text = "Configure local media storage directories, appearance & grid density, reader defaults, keyboard shortcuts, and database backups.",
                style = MaterialTheme.typography.bodyMedium,
                color = Purple300.copy(alpha = 0.78f)
            )

            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .background(DominaCard)
                    .border(1.dp, Purple900.copy(alpha = 0.6f), RoundedCornerShape(12.dp))
                    .padding(4.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                listOf(
                    "general" to "Settings & Appearance",
                    "shortcuts" to "Shortcuts Reference",
                    "architecture" to "Architecture Blueprint"
                ).forEach { (id, label) ->
                    val active = activeTab == id
                    Text(
                        text = label,
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Medium,
                        color = if (active) Color.White else Purple300,
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (active) palette.accent else Color.Transparent)
                            .testTag("settings_tab_$id")
                            .clickable { activeTab = id }
                            .padding(horizontal = 10.dp, vertical = 7.dp)
                    )
                }
            }
            HorizontalDivider(color = Purple900.copy(alpha = 0.6f))
        }

        when (activeTab) {
            "general" -> {
                // 1. Local Storage Directories
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(DominaCard)
                        .border(1.dp, Purple900.copy(alpha = 0.6f), RoundedCornerShape(16.dp))
                        .padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
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
                                imageVector = Icons.Filled.Storage,
                                contentDescription = null,
                                tint = Purple400,
                                modifier = Modifier.size(18.dp)
                            )
                            Text(
                                text = "Local Storage Directories",
                                style = MaterialTheme.typography.headlineMedium,
                                color = Purple50
                            )
                        }
                        Text(
                            text = if (directories.lastIndexedAt.isNotBlank()) {
                                "Indexed: ${directories.lastIndexedAt.take(19).replace('T', ' ')}"
                            } else "Not yet indexed",
                            style = MaterialTheme.typography.labelSmall,
                            color = Purple300.copy(alpha = 0.75f)
                        )
                    }

                    Text(
                        text = "Point Domina to any local directory paths on the device using Browse Disk, or tap Mount Folder to import local files/subfolders directly into the vault directory and index them.",
                        style = MaterialTheme.typography.bodySmall,
                        color = Purple300.copy(alpha = 0.75f)
                    )

                    directoryFields.forEach { entry ->
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text(
                                text = entry.label,
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.Medium,
                                color = Purple200
                            )
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(DominaCardInner)
                                    .border(1.dp, Purple900.copy(alpha = 0.6f), RoundedCornerShape(8.dp))
                                    .padding(horizontal = 10.dp, vertical = 8.dp)
                            ) {
                                BasicTextField(
                                    value = pathForKey(entry.key),
                                    onValueChange = { nextVal ->
                                        val updated = updatePathForKey(entry.key, nextVal)
                                        dirDrafts = updated
                                        onUpdateDirectories(updated)
                                    },
                                    singleLine = true,
                                    textStyle = MaterialTheme.typography.labelSmall.copy(color = Purple50),
                                    cursorBrush = SolidColor(palette.accentBright),
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }

                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Row(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(DominaElevatedSurface)
                                        .border(1.dp, Purple800.copy(alpha = 0.6f), RoundedCornerShape(8.dp))
                                        .clickable { pickerTarget = entry.key to entry.label }
                                        .padding(horizontal = 10.dp, vertical = 7.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(5.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.FolderOpen,
                                        contentDescription = null,
                                        tint = Purple400,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Text("Browse Disk", style = MaterialTheme.typography.bodySmall, color = Purple100)
                                }

                                Row(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(DominaCardInner)
                                        .border(1.dp, Purple900.copy(alpha = 0.6f), RoundedCornerShape(8.dp))
                                        .clickable { onMountFolder(entry.type) }
                                        .padding(horizontal = 10.dp, vertical = 7.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(5.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.CreateNewFolder,
                                        contentDescription = null,
                                        tint = palette.accentBright,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Text("Mount Folder", style = MaterialTheme.typography.bodySmall, color = Purple200)
                                }
                            }
                        }
                    }

                    HorizontalDivider(color = Purple900.copy(alpha = 0.5f))

                    // Auto-Scan on Startup Toggle
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Automatically Scan Directories on Startup",
                                style = MaterialTheme.typography.titleSmall,
                                color = Purple100
                            )
                            Text(
                                text = "Index new files in configured vault paths whenever Domina starts",
                                style = MaterialTheme.typography.bodySmall,
                                color = Purple300.copy(alpha = 0.7f)
                            )
                        }
                        Checkbox(
                            checked = dirDrafts.autoScanOnStartup,
                            onCheckedChange = { checked ->
                                val next = dirDrafts.copy(autoScanOnStartup = checked)
                                dirDrafts = next
                                onUpdateDirectories(next)
                            },
                            colors = CheckboxDefaults.colors(checkedColor = palette.accent)
                        )
                    }

                    HorizontalDivider(color = Purple900.copy(alpha = 0.4f))

                    // Rescan / Re-Index Library Button
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Filesystem Indexer & Subfolder Scanner",
                                style = MaterialTheme.typography.titleSmall,
                                color = Purple100
                            )
                            Text(
                                text = "Scan configured directories on disk, parse image headers, and group Comic/Image Set subfolders.",
                                style = MaterialTheme.typography.bodySmall,
                                color = Purple300.copy(alpha = 0.7f)
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Row(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(palette.accent)
                                .testTag("rescan_library_btn")
                                .clickable(enabled = !isScanning) {
                                    isScanning = true
                                    scanProgress = 50
                                    scanStatusText = "Scanning configured filesystem directories on disk..."
                                    onTriggerServerScan(dirDrafts) { totalFound ->
                                        isScanning = false
                                        scanProgress = 100
                                        scanStatusText = "Filesystem scan complete — $totalFound media items indexed from disk."
                                    }
                                }
                                .padding(horizontal = 12.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Refresh,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(14.dp)
                            )
                            Text(
                                text = if (isScanning) "Scanning..." else "Rescan Library",
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.SemiBold,
                                color = Color.White
                            )
                        }
                    }

                    if (scanStatusText != null) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(DominaCardInner)
                                .border(1.dp, Purple800.copy(alpha = 0.6f), RoundedCornerShape(8.dp))
                                .padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    if (scanProgress == 100) {
                                        Icon(
                                            imageVector = Icons.Filled.CheckCircle,
                                            contentDescription = null,
                                            tint = Emerald400,
                                            modifier = Modifier.size(14.dp)
                                        )
                                    }
                                    Text(
                                        text = scanStatusText!!,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = Purple100
                                    )
                                }
                                Text(
                                    text = "$scanProgress%",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = Purple400
                                )
                            }
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(6.dp)
                                    .clip(RoundedCornerShape(3.dp))
                                    .background(Purple950)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth(scanProgress / 100f)
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

                // 2. Appearance & Grid Density (Live Sync)
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
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Palette,
                                contentDescription = null,
                                tint = Purple400,
                                modifier = Modifier.size(18.dp)
                            )
                            Text(
                                text = "Appearance & Grid Density",
                                style = MaterialTheme.typography.headlineMedium,
                                color = Purple50
                            )
                        }
                        Text("LIVE SYNC", style = MaterialTheme.typography.labelSmall, color = Purple400)
                    }

                    // Purple & Accent Palette Preset
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            text = "Purple & Accent Palette Preset",
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Medium,
                            color = Purple200
                        )
                        val themes = listOf(
                            Triple("violet", "Royal Violet", RoyalVioletAccent),
                            Triple("amethyst", "Amethyst Orchid", AmethystAccent),
                            Triple("indigo", "Midnight Indigo", IndigoAccent),
                            Triple("crimson", "Violet & Crimson", CrimsonAccent)
                        )
                        themes.chunked(2).forEach { pair ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                pair.forEach { (id, label, swatch) ->
                                    val active = appearance.accentColor == id
                                    Row(
                                        modifier = Modifier
                                            .weight(1f)
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(if (active) palette.accent else DominaCardInner)
                                            .border(
                                                1.dp,
                                                if (active) Purple400 else Purple900.copy(alpha = 0.6f),
                                                RoundedCornerShape(8.dp)
                                            )
                                            .testTag("theme_preset_$id")
                                            .clickable {
                                                onUpdateAppearance(appearance.copy(accentColor = id))
                                            }
                                            .padding(horizontal = 10.dp, vertical = 9.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(12.dp)
                                                .clip(CircleShape)
                                                .background(swatch)
                                        )
                                        Text(
                                            text = label,
                                            style = MaterialTheme.typography.bodySmall,
                                            fontWeight = FontWeight.Medium,
                                            color = if (active) Color.White else Purple200
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // Accent Intensity Slider
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "Purple Ambient & Border Glow Intensity",
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.Medium,
                                color = Purple200
                            )
                            Text(
                                text = "${appearance.accentIntensity}%",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.SemiBold,
                                color = Purple400
                            )
                        }
                        Slider(
                            value = appearance.accentIntensity.toFloat(),
                            onValueChange = {
                                onUpdateAppearance(appearance.copy(accentIntensity = it.roundToInt()))
                            },
                            valueRange = 20f..100f,
                            colors = SliderDefaults.colors(
                                thumbColor = palette.accentBright,
                                activeTrackColor = palette.accent,
                                inactiveTrackColor = Purple900.copy(alpha = 0.5f)
                            ),
                            modifier = Modifier.height(24.dp)
                        )
                    }

                    // Gallery Grid Density Preset
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            text = "Gallery Grid Density (Thumbnail Size)",
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Medium,
                            color = Purple200
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            listOf(
                                "ultra-compact" to "Ultra",
                                "compact" to "Compact",
                                "standard" to "Standard",
                                "spacious" to "Spacious"
                            ).forEach { (id, label) ->
                                val active = appearance.gridDensity == id && appearance.gridColumns == 0
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(if (active) palette.accent else DominaCardInner)
                                        .border(
                                            1.dp,
                                            if (active) Purple400 else Purple900.copy(alpha = 0.6f),
                                            RoundedCornerShape(8.dp)
                                        )
                                        .clickable {
                                            onUpdateAppearance(
                                                appearance.copy(gridDensity = id, gridColumns = 0)
                                            )
                                        }
                                        .padding(vertical = 8.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = label,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = if (active) Color.White else Purple200
                                    )
                                }
                            }
                        }
                    }

                    // Custom Columns Per Row Override
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "Custom Columns Per Row",
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.Medium,
                                color = Purple200
                            )
                            Text(
                                text = if (appearance.gridColumns > 0) {
                                    "${appearance.gridColumns} Columns"
                                } else "Auto (${appearance.gridDensity})",
                                style = MaterialTheme.typography.labelSmall,
                                color = Purple400
                            )
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            listOf(0, 1, 2, 3, 4).forEach { cols ->
                                val active = appearance.gridColumns == cols
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(if (active) palette.accent else DominaCardInner)
                                        .border(
                                            1.dp,
                                            if (active) Purple400 else Purple900.copy(alpha = 0.6f),
                                            RoundedCornerShape(8.dp)
                                        )
                                        .clickable {
                                            onUpdateAppearance(appearance.copy(gridColumns = cols))
                                        }
                                        .padding(vertical = 7.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = if (cols == 0) "Auto" else "${cols}x",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = if (active) Color.White else Purple200
                                    )
                                }
                            }
                        }
                    }

                    // Gallery Thumbnail Aspect Ratio
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            text = "Gallery Thumbnail Aspect Ratio",
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Medium,
                            color = Purple200
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            listOf("portrait", "landscape", "square").forEach { aspect ->
                                val active = appearance.thumbnailAspect == aspect
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(if (active) palette.accent else DominaCardInner)
                                        .border(
                                            1.dp,
                                            if (active) Purple400 else Purple900.copy(alpha = 0.6f),
                                            RoundedCornerShape(8.dp)
                                        )
                                        .clickable {
                                            onUpdateAppearance(appearance.copy(thumbnailAspect = aspect))
                                        }
                                        .padding(vertical = 8.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = aspect.replaceFirstChar { it.uppercase() },
                                        style = MaterialTheme.typography.bodySmall,
                                        color = if (active) Color.White else Purple200
                                    )
                                }
                            }
                        }
                    }

                    // Cropping, Spacing, Corner Style, Background Tone, Border Style
                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        SettingsSelectChip(
                            title = "Cropping",
                            currentLabel = if (appearance.thumbnailFit == "contain") "Contain (Full)" else "Cover (Fill)",
                            options = listOf("Cover (Fill)" to "cover", "Contain (Full)" to "contain"),
                            onSelect = { onUpdateAppearance(appearance.copy(thumbnailFit = it)) }
                        )
                        SettingsSelectChip(
                            title = "Grid Spacing",
                            currentLabel = appearance.cardGap.replaceFirstChar { it.uppercase() },
                            options = listOf("Tight" to "tight", "Standard" to "standard", "Relaxed" to "relaxed"),
                            onSelect = { onUpdateAppearance(appearance.copy(cardGap = it)) }
                        )
                        SettingsSelectChip(
                            title = "Corner Style",
                            currentLabel = appearance.cardRadius.replaceFirstChar { it.uppercase() },
                            options = listOf("Sharp" to "sharp", "Rounded" to "rounded", "Luxury" to "lux"),
                            onSelect = { onUpdateAppearance(appearance.copy(cardRadius = it)) }
                        )
                        SettingsSelectChip(
                            title = "Background Tone",
                            currentLabel = appearance.bgDarkness.replaceFirstChar { it.uppercase() },
                            options = listOf(
                                "Pitch Black" to "pitch-black",
                                "Obsidian Purple" to "obsidian",
                                "Charcoal Violet" to "charcoal"
                            ),
                            onSelect = { onUpdateAppearance(appearance.copy(bgDarkness = it)) }
                        )
                        SettingsSelectChip(
                            title = "Border Style",
                            currentLabel = appearance.borderStyle.replaceFirstChar { it.uppercase() },
                            options = listOf(
                                "Neon Glow" to "glow",
                                "Minimal" to "minimal",
                                "Frosted Glass" to "glass"
                            ),
                            onSelect = { onUpdateAppearance(appearance.copy(borderStyle = it)) }
                        )
                    }

                    HorizontalDivider(color = Purple900.copy(alpha = 0.4f))

                    // Gallery Card Elements Toggles
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Tune,
                                contentDescription = null,
                                tint = Purple400,
                                modifier = Modifier.size(14.dp)
                            )
                            Text("Gallery Card Elements", style = MaterialTheme.typography.titleSmall, color = Purple200)
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            ToggleBoxItem(
                                label = "5-Star Rating",
                                checked = appearance.showCardRating,
                                onCheckedChange = { onUpdateAppearance(appearance.copy(showCardRating = it)) },
                                modifier = Modifier.weight(1f)
                            )
                            ToggleBoxItem(
                                label = "File Size",
                                checked = appearance.showCardSize,
                                onCheckedChange = { onUpdateAppearance(appearance.copy(showCardSize = it)) },
                                modifier = Modifier.weight(1f)
                            )
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            ToggleBoxItem(
                                label = "Tag Chips",
                                checked = appearance.showCardTags,
                                onCheckedChange = { onUpdateAppearance(appearance.copy(showCardTags = it)) },
                                modifier = Modifier.weight(1f)
                            )
                            ToggleBoxItem(
                                label = "Dimensions",
                                checked = appearance.showCardDimensions,
                                onCheckedChange = { onUpdateAppearance(appearance.copy(showCardDimensions = it)) },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }

                // 3. Reader, Slideshow & Video Defaults
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(DominaCard)
                        .border(1.dp, Purple900.copy(alpha = 0.6f), RoundedCornerShape(16.dp))
                        .padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.MenuBook,
                            contentDescription = null,
                            tint = Purple400,
                            modifier = Modifier.size(18.dp)
                        )
                        Text(
                            text = "Reader, Slideshow & Video Defaults",
                            style = MaterialTheme.typography.headlineMedium,
                            color = Purple50
                        )
                    }

                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        SettingsSelectChip(
                            title = "Default Reader Fit",
                            currentLabel = appearance.defaultReaderFit.label,
                            options = ReaderFitMode.entries.map { it.label to it },
                            onSelect = { onUpdateAppearance(appearance.copy(defaultReaderFit = it)) }
                        )
                        SettingsSelectChip(
                            title = "Reading Direction",
                            currentLabel = appearance.defaultReaderDirection.label,
                            options = ReaderDirection.entries.map { it.label to it },
                            onSelect = { onUpdateAppearance(appearance.copy(defaultReaderDirection = it)) }
                        )
                        SettingsSelectChip(
                            title = "Slideshow Interval",
                            currentLabel = "Every ${appearance.slideshowIntervalSeconds}s",
                            options = listOf(2, 3, 4, 6, 10).map { "Every ${it}s" to it },
                            onSelect = { onUpdateAppearance(appearance.copy(slideshowIntervalSeconds = it)) }
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        ToggleBoxItem(
                            label = "Loop Videos by Default",
                            checked = appearance.defaultVideoLoop,
                            onCheckedChange = { onUpdateAppearance(appearance.copy(defaultVideoLoop = it)) },
                            modifier = Modifier.weight(1f)
                        )
                        ToggleBoxItem(
                            label = "Auto-Play Next Video",
                            checked = appearance.defaultAutoPlayNext,
                            onCheckedChange = { onUpdateAppearance(appearance.copy(defaultAutoPlayNext = it)) },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                // 4. Panic Close & Thumbnail Concealment
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(DominaCard)
                        .border(1.dp, Purple900.copy(alpha = 0.6f), RoundedCornerShape(16.dp))
                        .padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
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
                                imageVector = Icons.Filled.Security,
                                contentDescription = null,
                                tint = Rose400,
                                modifier = Modifier.size(18.dp)
                            )
                            Text(
                                text = "Panic Close & Thumbnail Concealment",
                                style = MaterialTheme.typography.headlineMedium,
                                color = Purple50
                            )
                        }
                        Text("Hotkey: Ctrl + D", style = MaterialTheme.typography.labelSmall, color = Purple400)
                    }

                    Text(
                        text = "Tap Panic Close to immediately conceal this vault screen. You can also toggle thumbnail blur below across all galleries and viewports.",
                        style = MaterialTheme.typography.bodySmall,
                        color = Purple300.copy(alpha = 0.75f)
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(8.dp))
                                .background(Rose900)
                                .border(1.dp, Rose700, RoundedCornerShape(8.dp))
                                .clickable { onPanicClose() }
                                .padding(vertical = 10.dp),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Filled.PowerSettingsNew,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Panic Close Page",
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.SemiBold,
                                color = Color.White
                            )
                        }

                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (appearance.panicModeBlur) palette.accent else DominaCardInner)
                                .border(
                                    1.dp,
                                    if (appearance.panicModeBlur) Purple400 else Purple900.copy(alpha = 0.6f),
                                    RoundedCornerShape(8.dp)
                                )
                                .testTag("toggle_panic_blur_btn")
                                .clickable {
                                    onUpdateAppearance(appearance.copy(panicModeBlur = !appearance.panicModeBlur))
                                }
                                .padding(vertical = 10.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = if (appearance.panicModeBlur) "Disable Thumbnail Blur" else "Enable Thumbnail Blur",
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.SemiBold,
                                color = if (appearance.panicModeBlur) Color.White else Purple200
                            )
                        }
                    }
                }

                // 5. Database Backup, Export & Clear
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(DominaCard)
                        .border(1.dp, Purple900.copy(alpha = 0.6f), RoundedCornerShape(16.dp))
                        .padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "Database Backup, Export & Clear",
                        style = MaterialTheme.typography.headlineMedium,
                        color = Purple50
                    )
                    Text(
                        text = "Export all 5-star ratings, boolean tags, comic reading progress, favorites, and directory mappings as a portable JSON snapshot, restore from backup, or clear the index.",
                        style = MaterialTheme.typography.bodySmall,
                        color = Purple300.copy(alpha = 0.75f)
                    )

                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(DominaElevatedSurface)
                                .border(1.dp, Purple800.copy(alpha = 0.6f), RoundedCornerShape(8.dp))
                                .clickable {
                                    onExportDatabaseJson { json ->
                                        val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
                                        cm?.setPrimaryClip(ClipData.newPlainText("domina_backup", json))
                                        dbFeedback = "Database JSON (${mediaItems.size} items) copied to clipboard!"
                                    }
                                }
                                .padding(horizontal = 12.dp, vertical = 9.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Download,
                                contentDescription = null,
                                tint = Purple400,
                                modifier = Modifier.size(14.dp)
                            )
                            Text(
                                text = "Export Database JSON (${mediaItems.size} items)",
                                style = MaterialTheme.typography.bodySmall,
                                color = Purple100
                            )
                        }

                        Row(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(DominaElevatedSurface)
                                .border(1.dp, Purple800.copy(alpha = 0.6f), RoundedCornerShape(8.dp))
                                .clickable { showImportJsonBox = !showImportJsonBox }
                                .padding(horizontal = 12.dp, vertical = 9.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Upload,
                                contentDescription = null,
                                tint = palette.accentBright,
                                modifier = Modifier.size(14.dp)
                            )
                            Text(
                                text = "Import Database JSON",
                                style = MaterialTheme.typography.bodySmall,
                                color = Purple100
                            )
                        }

                        Row(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(DominaCardInner)
                                .border(1.dp, Rose800.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
                                .testTag("clear_indexed_vault_btn")
                                .clickable {
                                    onClearLibrary {
                                        dbFeedback = "Vault index cleared (0 items)."
                                    }
                                }
                                .padding(horizontal = 12.dp, vertical = 9.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Delete,
                                contentDescription = null,
                                tint = Rose300,
                                modifier = Modifier.size(14.dp)
                            )
                            Text(
                                text = "Clear Indexed Vault",
                                style = MaterialTheme.typography.bodySmall,
                                color = Rose300
                            )
                        }
                    }

                    if (showImportJsonBox) {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(DominaCardInner)
                                    .border(1.dp, Purple800, RoundedCornerShape(8.dp))
                                    .padding(10.dp)
                            ) {
                                if (importJsonDraft.isEmpty()) {
                                    Text(
                                        text = "Paste exported Domina database JSON here...",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = Purple400.copy(alpha = 0.5f)
                                    )
                                }
                                BasicTextField(
                                    value = importJsonDraft,
                                    onValueChange = { importJsonDraft = it },
                                    textStyle = MaterialTheme.typography.labelSmall.copy(color = Purple100),
                                    cursorBrush = SolidColor(palette.accentBright),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(80.dp)
                                )
                            }
                            Text(
                                text = "Restore JSON Backup",
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.SemiBold,
                                color = Color.White,
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(palette.accent)
                                    .clickable {
                                        onImportDatabaseJson(importJsonDraft) { ok ->
                                            dbFeedback = if (ok) {
                                                showImportJsonBox = false
                                                importJsonDraft = ""
                                                "Database backup imported and persisted."
                                            } else {
                                                "Invalid JSON schema — could not import backup."
                                            }
                                        }
                                    }
                                    .padding(horizontal = 14.dp, vertical = 8.dp)
                            )
                        }
                    }

                    if (dbFeedback != null) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(DominaCardInner)
                                .border(1.dp, Purple800.copy(alpha = 0.6f), RoundedCornerShape(8.dp))
                                .padding(10.dp)
                        ) {
                            Text(
                                text = dbFeedback!!,
                                style = MaterialTheme.typography.labelSmall,
                                color = Emerald400
                            )
                        }
                    }
                }
            }

            "shortcuts" -> {
                shortcutGroups.forEach { group ->
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .background(DominaCard)
                            .border(1.dp, Purple900.copy(alpha = 0.6f), RoundedCornerShape(16.dp))
                            .padding(18.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Keyboard,
                                contentDescription = null,
                                tint = Purple400,
                                modifier = Modifier.size(17.dp)
                            )
                            Text(
                                text = group.title,
                                style = MaterialTheme.typography.headlineMedium,
                                color = Purple50
                            )
                        }
                        HorizontalDivider(color = Purple900.copy(alpha = 0.5f))

                        group.items.forEach { (keys, action) ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = action,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Purple200,
                                    modifier = Modifier.weight(1f)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = keys,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Purple400,
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(DominaCardInner)
                                        .border(1.dp, Purple800.copy(alpha = 0.7f), RoundedCornerShape(6.dp))
                                        .padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                            HorizontalDivider(color = Purple900.copy(alpha = 0.35f))
                        }
                    }
                }
            }

            "architecture" -> {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(DominaCard)
                        .border(1.dp, Purple900.copy(alpha = 0.6f), RoundedCornerShape(16.dp))
                        .padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "1. Local-First Android Vault Architecture",
                        style = MaterialTheme.typography.headlineMedium,
                        color = Purple50
                    )
                    Text(
                        text = "Room SQLite + Filesystem Engine: Persistent Room database paired with recursive directory walkers, interactive disk directory browser, binary image header parser, and subfolder collection scanner.",
                        style = MaterialTheme.typography.bodySmall,
                        color = Purple200.copy(alpha = 0.85f)
                    )
                    Text(
                        text = "Local Indexing & Subfolder Parser: Recursive directory scanner for single Images and Videos, paired with a Subfolder Collection Scanner for Comics and Image Sets where each subdirectory becomes a Comic/Set entity and its natural-sorted image files populate ordered pages.",
                        style = MaterialTheme.typography.bodySmall,
                        color = Purple200.copy(alpha = 0.85f)
                    )
                    Text(
                        text = "Boolean Tag & Multi-Star Query Engine: Deterministic parser supporting +include, -exclude, and tagA OR tagB boolean clauses alongside concurrent multi-rating arrays (rating IN (4, 5)).",
                        style = MaterialTheme.typography.bodySmall,
                        color = Purple200.copy(alpha = 0.85f)
                    )
                }

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(DominaCard)
                        .border(1.dp, Purple900.copy(alpha = 0.6f), RoundedCornerShape(16.dp))
                        .padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "2. SQLite Relational Schema (DDL)",
                        style = MaterialTheme.typography.headlineMedium,
                        color = Purple50
                    )
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(DominaCardInner)
                            .border(1.dp, Purple900.copy(alpha = 0.6f), RoundedCornerShape(8.dp))
                            .padding(12.dp)
                    ) {
                        Text(
                            text = """
CREATE TABLE media_items (
  id TEXT PRIMARY KEY,
  type TEXT NOT NULL CHECK(type IN ('image','video','comic','imageset')),
  title TEXT NOT NULL,
  filename TEXT NOT NULL,
  local_path TEXT UNIQUE NOT NULL,
  folder_path TEXT NOT NULL,
  rating INTEGER DEFAULT 0 CHECK(rating BETWEEN 0 AND 5),
  is_favorite INTEGER DEFAULT 0,
  size_bytes INTEGER NOT NULL,
  format TEXT NOT NULL,
  width INTEGER,
  height INTEGER,
  duration_seconds REAL,
  page_count INTEGER DEFAULT 1,
  last_read_page INTEGER DEFAULT 1,
  read_pages_json TEXT DEFAULT '[]',
  date_added TEXT NOT NULL
);
                            """.trimIndent(),
                            style = MaterialTheme.typography.labelSmall,
                            color = Purple200
                        )
                    }
                }
            }
        }
    }

    // Interactive Filesystem Directory Browser Modal
    DirectoryPickerModal(
        isOpen = pickerTarget != null,
        title = pickerTarget?.let { "Select ${it.second}" } ?: "Select Directory",
        initialPath = pickerTarget?.let { pathForKey(it.first) } ?: dirDrafts.imagesPath,
        onClose = { pickerTarget = null },
        onSelectPath = { selectedPath ->
            val target = pickerTarget ?: return@DirectoryPickerModal
            val updated = updatePathForKey(target.first, selectedPath)
            dirDrafts = updated
            onUpdateDirectories(updated)
            onTriggerServerScan(updated) {}
        }
    )
}

@Composable
private fun <T> SettingsSelectChip(
    title: String,
    currentLabel: String,
    options: List<Pair<String, T>>,
    onSelect: (T) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    Box {
        Column(
            modifier = Modifier
                .clip(RoundedCornerShape(8.dp))
                .background(DominaCardInner)
                .border(1.dp, Purple900.copy(alpha = 0.6f), RoundedCornerShape(8.dp))
                .clickable { expanded = true }
                .padding(horizontal = 10.dp, vertical = 6.dp)
        ) {
            Text(title, style = MaterialTheme.typography.labelSmall, color = Purple300.copy(alpha = 0.7f))
            Text(currentLabel, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Medium, color = Purple100)
        }

        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            modifier = Modifier.background(Color(0xFF140A26))
        ) {
            options.forEach { (label, value) ->
                DropdownMenuItem(
                    text = { Text(label, style = MaterialTheme.typography.bodySmall, color = Purple100) },
                    onClick = {
                        onSelect(value)
                        expanded = false
                    }
                )
            }
        }
    }
}

@Composable
private fun ToggleBoxItem(
    label: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    val palette = LocalDominaPalette.current
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(DominaCardInner)
            .border(1.dp, Purple900.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
            .clickable { onCheckedChange(!checked) }
            .padding(horizontal = 10.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Checkbox(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = CheckboxDefaults.colors(checkedColor = palette.accent),
            modifier = Modifier.size(18.dp)
        )
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = Purple100
        )
    }
}
