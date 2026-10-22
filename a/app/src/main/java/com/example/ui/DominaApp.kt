package com.example.ui

import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CreateNewFolder
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.PowerSettingsNew
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.model.MediaItem
import com.example.model.MediaType
import com.example.model.PageView
import com.example.ui.components.FilterToolbar
import com.example.ui.components.QuickSearchModal
import com.example.ui.screens.AdvancedSearchView
import com.example.ui.screens.CollectionOverviewView
import com.example.ui.screens.DashboardView
import com.example.ui.screens.ImageDetailsView
import com.example.ui.screens.MediaGridFlow
import com.example.ui.screens.ReaderView
import com.example.ui.screens.SettingsView
import com.example.ui.screens.VideoDetailsView
import com.example.ui.theme.DominaCard
import com.example.ui.theme.DominaHeaderBg
import com.example.ui.theme.DominaTheme
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
import com.example.viewmodel.VaultViewModel

@Composable
fun DominaApp(
    vaultViewModel: VaultViewModel = viewModel()
) {
    val state by vaultViewModel.uiState.collectAsStateWithLifecycle()

    // Zero-permission Android Photo Picker & Multiple Document Picker for importing files/folders
    var pendingImportType by remember { mutableStateOf(MediaType.IMAGE) }
    var pendingCollectionName by remember { mutableStateOf<String?>(null) }

    val visualMediaLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickMultipleVisualMedia(20)
    ) { uris: List<Uri> ->
        if (uris.isNotEmpty()) {
            vaultViewModel.importLocalUris(uris, pendingImportType, pendingCollectionName)
        }
    }

    val openMultipleDocumentsLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenMultipleDocuments()
    ) { uris: List<Uri> ->
        if (uris.isNotEmpty()) {
            vaultViewModel.importLocalUris(uris, pendingImportType, pendingCollectionName)
        }
    }

    fun triggerImportPicker(type: MediaType, asFolderCollection: Boolean) {
        pendingImportType = type
        pendingCollectionName = if (asFolderCollection && (type == MediaType.COMIC || type == MediaType.IMAGESET)) {
            "${type.key.replaceFirstChar { it.uppercase() }}_Imported_${System.currentTimeMillis() % 1000}"
        } else null
        try {
            val mediaTypeFilter = if (type == MediaType.VIDEO) {
                ActivityResultContracts.PickVisualMedia.VideoOnly
            } else {
                ActivityResultContracts.PickVisualMedia.ImageOnly
            }
            visualMediaLauncher.launch(PickVisualMediaRequest(mediaTypeFilter))
        } catch (_: Exception) {
            val mime = if (type == MediaType.VIDEO) arrayOf("video/*") else arrayOf("image/*")
            openMultipleDocumentsLauncher.launch(mime)
        }
    }

    // System Back Button handling across all views
    BackHandler(enabled = vaultViewModel.canNavigateBack() || state.privacyScreenLocked || state.panicClosed) {
        when {
            state.panicClosed -> vaultViewModel.restoreFromPanicClose()
            state.privacyScreenLocked -> vaultViewModel.setPrivacyScreenLocked(false)
            else -> vaultViewModel.navigateBack()
        }
    }

    DominaTheme(appearance = state.appearance) {
        val palette = LocalDominaPalette.current

        // 1. Panic Closed Screen (Pure black with discrete double-tap restore)
        if (state.panicClosed) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black)
                    .testTag("panic_closed_screen")
                    .clickable { vaultViewModel.restoreFromPanicClose() },
                contentAlignment = Alignment.BottomCenter
            ) {
                Text(
                    text = "Tap anywhere to restore session",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.White.copy(alpha = 0.18f),
                    modifier = Modifier.padding(24.dp)
                )
            }
            return@DominaTheme
        }

        // 2. Fullscreen Privacy Lock Screen
        if (state.privacyScreenLocked) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0xFF06030C))
                    .padding(24.dp)
                    .testTag("privacy_lock_screen"),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(DominaCard)
                        .border(1.dp, Purple800.copy(alpha = 0.7f), RoundedCornerShape(16.dp))
                        .padding(28.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(18.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF1D0F36))
                            .border(1.dp, Purple700.copy(alpha = 0.6f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Lock,
                            contentDescription = null,
                            tint = Purple400,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = "Vault Privacy Lock Active",
                            style = MaterialTheme.typography.displaySmall,
                            color = Purple50,
                            textAlign = TextAlign.Center
                        )
                        Text(
                            text = "All local media viewports and thumbnails are concealed. Tap below to unlock your vault.",
                            style = MaterialTheme.typography.bodySmall,
                            color = Purple300.copy(alpha = 0.75f),
                            textAlign = TextAlign.Center
                        )
                    }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(palette.accent)
                            .testTag("unlock_vault_btn")
                            .clickable { vaultViewModel.setPrivacyScreenLocked(false) }
                            .padding(vertical = 11.dp),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Filled.LockOpen,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(15.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Unlock Vault",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = Color.White
                        )
                    }
                }
            }
            return@DominaTheme
        }

        // Compute dynamic columns count from AppearanceConfig
        val columnsCount = remember(state.appearance.gridColumns, state.appearance.gridDensity) {
            if (state.appearance.gridColumns >= 1) {
                state.appearance.gridColumns
            } else {
                when (state.appearance.gridDensity) {
                    "ultra-compact" -> 3
                    "compact" -> 3
                    "spacious" -> 1
                    else -> 2
                }
            }
        }

        // All unique tags sorted by frequency
        val allAvailableTags = remember(state.mediaItems) {
            val counts = mutableMapOf<String, Int>()
            state.mediaItems.forEach { item ->
                item.tags.forEach { t ->
                    counts[t] = (counts[t] ?: 0) + 1
                }
            }
            counts.entries.sortedByDescending { it.value }.map { it.key }
        }

        val selectedItem = remember(state.mediaItems, state.selectedItemId) {
            state.mediaItems.find { it.id == state.selectedItemId }
        }

        val activeCollectionPageItem: MediaItem? = remember(
            selectedItem,
            state.selectedCollectionPageNumber
        ) {
            val col = selectedItem ?: return@remember null
            val pageNum = state.selectedCollectionPageNumber ?: return@remember null
            if (col.type != MediaType.COMIC && col.type != MediaType.IMAGESET) return@remember null
            val p = col.pages.find { it.pageNumber == pageNum } ?: return@remember null
            MediaItem(
                id = "${col.id}:p${p.pageNumber}",
                type = MediaType.IMAGE,
                title = "${col.title} — Page ${p.pageNumber}",
                filename = p.filename,
                localPath = p.localPath,
                folderPath = col.folderPath,
                thumbnailUrl = p.url,
                mediaUrl = p.url,
                rating = p.rating,
                isFavorite = p.isFavorite,
                tags = p.tags.ifEmpty { col.tags },
                notes = p.notes,
                dateAdded = col.dateAdded,
                sizeBytes = p.sizeBytes,
                format = p.format,
                width = p.width,
                height = p.height
            )
        }

        val activeMediaType = when (state.currentPage) {
            PageView.VIDEOS -> MediaType.VIDEO
            PageView.COMICS -> MediaType.COMIC
            PageView.IMAGESETS -> MediaType.IMAGESET
            else -> MediaType.IMAGE
        }

        val categoryItems = remember(state.mediaItems, activeMediaType) {
            state.mediaItems.filter { it.type == activeMediaType }
        }

        val filteredCategoryItems = remember(categoryItems, state.gallerySearchQuery) {
            categoryItems.filter { MediaLogic.matchesSearchQuery(it, state.gallerySearchQuery) }
        }

        Scaffold(
            contentWindowInsets = WindowInsets.safeDrawing,
            containerColor = palette.bg,
            topBar = {
                DominaTopHeader(
                    currentPage = state.currentPage,
                    selectedItemType = selectedItem?.type,
                    onNavigateSection = { page -> vaultViewModel.navigateTo(page) }
                )
            }
        ) { innerPadding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.radialGradient(
                            colors = listOf(palette.accentSoft, palette.bg),
                            radius = 1200f
                        )
                    )
                    .padding(innerPadding)
            ) {
                when {
                    state.isLoadingVault -> {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "Connecting to local Domina filesystem engine...",
                                style = MaterialTheme.typography.labelMedium,
                                color = Purple300.copy(alpha = 0.7f)
                            )
                        }
                    }

                    state.currentPage == PageView.DASHBOARD -> {
                        DashboardView(
                            items = state.mediaItems,
                            directories = state.directories,
                            appearance = state.appearance,
                            columnsCount = columnsCount,
                            panicBlur = state.appearance.panicModeBlur,
                            uploadStatusText = state.uploadStatusText,
                            onNavigate = { vaultViewModel.navigateTo(it) },
                            onSelectItem = { vaultViewModel.selectItem(it) },
                            onResumeReader = { vaultViewModel.resumeReader(it) },
                            onRate = { id, r -> vaultViewModel.rateItem(id, r) },
                            onToggleFavorite = { id, fav -> vaultViewModel.toggleFavorite(id, fav) },
                            onQuickTagJump = { tag -> vaultViewModel.quickTagJump(tag) },
                            onOpenSearch = { vaultViewModel.setQuickSearchOpen(true) },
                            onPanicClose = { vaultViewModel.triggerPanicClose() },
                            onMountFolder = { type -> triggerImportPicker(type, asFolderCollection = true) },
                            onAddFiles = { type -> triggerImportPicker(type, asFolderCollection = false) }
                        )
                    }

                    state.currentPage == PageView.IMAGES ||
                            state.currentPage == PageView.VIDEOS ||
                            state.currentPage == PageView.COMICS ||
                            state.currentPage == PageView.IMAGESETS -> {
                        GalleryCategoryScreen(
                            currentPage = state.currentPage,
                            activeMediaType = activeMediaType,
                            directoryPath = when (state.currentPage) {
                                PageView.IMAGES -> state.directories.imagesPath
                                PageView.VIDEOS -> state.directories.videosPath
                                PageView.COMICS -> state.directories.comicsPath
                                else -> state.directories.imageSetsPath
                            },
                            categoryItems = categoryItems,
                            filteredCategoryItems = filteredCategoryItems,
                            searchQuery = state.gallerySearchQuery,
                            isUploading = state.isUploading,
                            uploadStatusText = state.uploadStatusText,
                            columnsCount = columnsCount,
                            appearance = state.appearance,
                            panicBlur = state.appearance.panicModeBlur,
                            onChangeSearch = { vaultViewModel.setGallerySearchQuery(it) },
                            onOpenAdvancedSearch = {
                                vaultViewModel.updateAdvancedFilter { prev ->
                                    prev.copy(selectedTypes = listOf(activeMediaType))
                                }
                                vaultViewModel.navigateTo(PageView.ADVANCED_SEARCH)
                            },
                            onLockScreen = { vaultViewModel.setPrivacyScreenLocked(true) },
                            onPanicClose = { vaultViewModel.triggerPanicClose() },
                            onMountFolder = { triggerImportPicker(activeMediaType, asFolderCollection = true) },
                            onAddFiles = { triggerImportPicker(activeMediaType, asFolderCollection = false) },
                            onSelectItem = { vaultViewModel.selectItem(it) },
                            onRate = { id, r -> vaultViewModel.rateItem(id, r) },
                            onToggleFavorite = { id, fav -> vaultViewModel.toggleFavorite(id, fav) },
                            onResumeReader = { vaultViewModel.resumeReader(it) },
                            onTagClick = { tag -> vaultViewModel.quickTagJump(tag) }
                        )
                    }

                    state.currentPage == PageView.ADVANCED_SEARCH -> {
                        AdvancedSearchView(
                            items = state.mediaItems,
                            filter = state.advancedFilter,
                            allTags = allAvailableTags,
                            savedPresets = state.savedPresets,
                            appearance = state.appearance,
                            columnsCount = columnsCount,
                            panicBlur = state.appearance.panicModeBlur,
                            onChangeFilter = { vaultViewModel.updateAdvancedFilter(it) },
                            onResetFilter = { vaultViewModel.resetAdvancedFilter() },
                            onSavePreset = { vaultViewModel.savePreset(it) },
                            onDeletePreset = { vaultViewModel.deletePreset(it) },
                            onSelectItem = { vaultViewModel.selectItem(it) },
                            onResumeReader = { vaultViewModel.resumeReader(it) },
                            onRate = { id, r -> vaultViewModel.rateItem(id, r) },
                            onToggleFavorite = { id, fav -> vaultViewModel.toggleFavorite(id, fav) },
                            onUpdateTags = { id, tags -> vaultViewModel.updateTags(id, tags) },
                            onDelete = { id -> vaultViewModel.deleteItem(id) }
                        )
                    }

                    state.currentPage == PageView.IMAGE_DETAILS -> {
                        val colPageNum = state.selectedCollectionPageNumber
                        if (colPageNum != null && activeCollectionPageItem != null && selectedItem != null) {
                            val totalColPages = selectedItem.pages.size.coerceAtLeast(1)
                            ImageDetailsView(
                                item = activeCollectionPageItem,
                                isCollectionPage = true,
                                onClose = {
                                    vaultViewModel.navigateTo(
                                        if (selectedItem.type == MediaType.COMIC) PageView.COMIC_PAGES else PageView.IMAGESET_IMAGES,
                                        selectedItem.id
                                    )
                                },
                                hasPrev = colPageNum > 1,
                                hasNext = colPageNum < totalColPages,
                                onPrev = {
                                    val prevP = if (colPageNum > 1) colPageNum - 1 else totalColPages
                                    vaultViewModel.navigateTo(
                                        PageView.IMAGE_DETAILS,
                                        selectedItem.id,
                                        1,
                                        prevP,
                                        pushHistory = false
                                    )
                                },
                                onNext = {
                                    val nextP = if (colPageNum < totalColPages) colPageNum + 1 else 1
                                    vaultViewModel.navigateTo(
                                        PageView.IMAGE_DETAILS,
                                        selectedItem.id,
                                        1,
                                        nextP,
                                        pushHistory = false
                                    )
                                },
                                onOpenReaderAtPage = {
                                    vaultViewModel.navigateTo(PageView.READER, selectedItem.id, colPageNum)
                                },
                                onRate = { _, rating ->
                                    vaultViewModel.rateCollectionPage(selectedItem.id, colPageNum, rating)
                                },
                                onToggleFavorite = { _, fav ->
                                    vaultViewModel.toggleFavoriteCollectionPage(selectedItem.id, colPageNum, fav)
                                },
                                onUpdateTags = { _, nextTags ->
                                    vaultViewModel.updateCollectionPageTags(selectedItem.id, colPageNum, nextTags)
                                },
                                onUpdateNotes = { _, notes ->
                                    vaultViewModel.updateCollectionPageNotes(selectedItem.id, colPageNum, notes)
                                },
                                onDelete = { vaultViewModel.deleteItem(selectedItem.id) },
                                slideshowIntervalSeconds = state.appearance.slideshowIntervalSeconds,
                                panicBlur = state.appearance.panicModeBlur
                            )
                        } else if (selectedItem != null) {
                            val imageList = state.mediaItems.filter { it.type == MediaType.IMAGE }
                            val idx = imageList.indexOfFirst { it.id == selectedItem.id }
                            ImageDetailsView(
                                item = selectedItem,
                                onClose = { vaultViewModel.navigateTo(PageView.IMAGES) },
                                hasPrev = idx > 0,
                                hasNext = idx >= 0 && idx < imageList.size - 1,
                                onPrev = {
                                    if (imageList.isNotEmpty()) {
                                        val prevIdx = if (idx > 0) idx - 1 else imageList.lastIndex
                                        vaultViewModel.navigateTo(
                                            PageView.IMAGE_DETAILS,
                                            imageList[prevIdx].id,
                                            pushHistory = false
                                        )
                                    }
                                },
                                onNext = {
                                    if (imageList.isNotEmpty()) {
                                        val nextIdx = if (idx >= 0 && idx < imageList.lastIndex) idx + 1 else 0
                                        vaultViewModel.navigateTo(
                                            PageView.IMAGE_DETAILS,
                                            imageList[nextIdx].id,
                                            pushHistory = false
                                        )
                                    }
                                },
                                onRate = { id, r -> vaultViewModel.rateItem(id, r) },
                                onToggleFavorite = { id, fav -> vaultViewModel.toggleFavorite(id, fav) },
                                onUpdateTags = { id, tags -> vaultViewModel.updateTags(id, tags) },
                                onUpdateNotes = { id, notes -> vaultViewModel.updateNotes(id, notes) },
                                onDelete = { id -> vaultViewModel.deleteItem(id) },
                                slideshowIntervalSeconds = state.appearance.slideshowIntervalSeconds,
                                panicBlur = state.appearance.panicModeBlur
                            )
                        }
                    }

                    state.currentPage == PageView.VIDEO_DETAILS && selectedItem != null -> {
                        val vids = state.mediaItems.filter { it.type == MediaType.VIDEO }
                        val idx = vids.indexOfFirst { it.id == selectedItem.id }
                        val nextVid = if (idx >= 0 && vids.isNotEmpty()) vids[(idx + 1) % vids.size] else null

                        VideoDetailsView(
                            item = selectedItem,
                            nextVideoItem = nextVid,
                            defaultLoop = state.appearance.defaultVideoLoop,
                            defaultAutoPlayNext = state.appearance.defaultAutoPlayNext,
                            onClose = { vaultViewModel.navigateTo(PageView.VIDEOS) },
                            onSelectVideo = { vid ->
                                vaultViewModel.navigateTo(PageView.VIDEO_DETAILS, vid.id, pushHistory = false)
                            },
                            onRate = { id, r -> vaultViewModel.rateItem(id, r) },
                            onToggleFavorite = { id, fav -> vaultViewModel.toggleFavorite(id, fav) },
                            onUpdateTags = { id, tags -> vaultViewModel.updateTags(id, tags) },
                            onDelete = { id -> vaultViewModel.deleteItem(id) },
                            panicBlur = state.appearance.panicModeBlur
                        )
                    }

                    (state.currentPage == PageView.COMIC_PAGES ||
                            state.currentPage == PageView.IMAGESET_IMAGES) && selectedItem != null -> {
                        CollectionOverviewView(
                            item = selectedItem,
                            appearance = state.appearance,
                            columnsCount = columnsCount,
                            onBack = {
                                vaultViewModel.navigateTo(
                                    if (selectedItem.type == MediaType.COMIC) PageView.COMICS else PageView.IMAGESETS
                                )
                            },
                            onOpenReader = { startPage ->
                                vaultViewModel.navigateTo(PageView.READER, selectedItem.id, startPage)
                            },
                            onSelectPage = { pageNum ->
                                vaultViewModel.navigateTo(
                                    PageView.IMAGE_DETAILS,
                                    selectedItem.id,
                                    1,
                                    pageNum
                                )
                            },
                            onRate = { id, r -> vaultViewModel.rateItem(id, r) },
                            onRatePage = { colId, pNum, r ->
                                vaultViewModel.rateCollectionPage(colId, pNum, r)
                            },
                            onToggleFavorite = { id, fav -> vaultViewModel.toggleFavorite(id, fav) },
                            onToggleFavoritePage = { colId, pNum, fav ->
                                vaultViewModel.toggleFavoriteCollectionPage(colId, pNum, fav)
                            },
                            onUpdateTags = { id, tags -> vaultViewModel.updateTags(id, tags) },
                            onResetComicProgress = { id -> vaultViewModel.resetComicProgress(id) },
                            onMarkAllComicRead = { id, total -> vaultViewModel.markAllComicRead(id, total) },
                            onDelete = { id -> vaultViewModel.deleteItem(id) },
                            panicBlur = state.appearance.panicModeBlur
                        )
                    }

                    state.currentPage == PageView.READER && selectedItem != null -> {
                        ReaderView(
                            item = selectedItem,
                            initialPage = state.readerStartPage,
                            defaultFitMode = state.appearance.defaultReaderFit,
                            defaultDirection = state.appearance.defaultReaderDirection,
                            slideshowIntervalSeconds = state.appearance.slideshowIntervalSeconds,
                            onCloseToOverview = {
                                vaultViewModel.navigateTo(
                                    if (selectedItem.type == MediaType.COMIC) PageView.COMIC_PAGES else PageView.IMAGESET_IMAGES,
                                    selectedItem.id
                                )
                            },
                            onRate = { id, r -> vaultViewModel.rateItem(id, r) },
                            onRatePage = { colId, pNum, r ->
                                vaultViewModel.rateCollectionPage(colId, pNum, r)
                            },
                            onUpdateBookmark = { id, pNum, readList ->
                                vaultViewModel.updateBookmark(id, pNum, readList)
                            },
                            panicBlur = state.appearance.panicModeBlur
                        )
                    }

                    state.currentPage == PageView.SETTINGS -> {
                        SettingsView(
                            directories = state.directories,
                            appearance = state.appearance,
                            mediaItems = state.mediaItems,
                            onUpdateDirectories = { vaultViewModel.updateDirectories(it) },
                            onUpdateAppearance = { vaultViewModel.updateAppearance(it) },
                            onTriggerServerScan = { dirs, cb -> vaultViewModel.triggerServerScan(dirs, cb) },
                            onMountFolder = { type -> triggerImportPicker(type, asFolderCollection = true) },
                            onExportDatabaseJson = { cb -> vaultViewModel.exportDatabaseJson(cb) },
                            onImportDatabaseJson = { json, cb -> vaultViewModel.importDatabaseJson(json, cb) },
                            onClearLibrary = { cb -> vaultViewModel.clearLibrary(cb) },
                            onPanicClose = { vaultViewModel.triggerPanicClose() }
                        )
                    }
                }
            }
        }

        // Global Quick Search Modal
        QuickSearchModal(
            isOpen = state.quickSearchOpen,
            onClose = { vaultViewModel.setQuickSearchOpen(false) },
            items = state.mediaItems,
            onSelectItem = { vaultViewModel.selectItem(it) },
            onRate = { id, r -> vaultViewModel.rateItem(id, r) },
            onOpenAdvancedSearch = { vaultViewModel.navigateTo(PageView.ADVANCED_SEARCH) }
        )
    }
}

@Composable
private fun DominaTopHeader(
    currentPage: PageView,
    selectedItemType: MediaType?,
    onNavigateSection: (PageView) -> Unit
) {
    val palette = LocalDominaPalette.current

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(DominaHeaderBg)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Zone 1: Brand Wordmark
            Text(
                text = "DOMINA",
                style = MaterialTheme.typography.titleLarge,
                color = Purple50,
                modifier = Modifier
                    .testTag("brand_wordmark")
                    .clickable { onNavigateSection(PageView.DASHBOARD) }
            )

            // Zone 2: Scrollable Navigation Links
            Row(
                modifier = Modifier
                    .weight(1f)
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                val navItems = listOf(
                    PageView.DASHBOARD to "Dashboard",
                    PageView.IMAGES to "Images",
                    PageView.VIDEOS to "Videos",
                    PageView.COMICS to "Comics",
                    PageView.IMAGESETS to "Image Sets",
                    PageView.ADVANCED_SEARCH to "Advanced Search"
                )

                navItems.forEach { (navPage, label) ->
                    val isActive = currentPage == navPage ||
                            (navPage == PageView.IMAGES && currentPage == PageView.IMAGE_DETAILS) ||
                            (navPage == PageView.VIDEOS && currentPage == PageView.VIDEO_DETAILS) ||
                            (navPage == PageView.COMICS && (currentPage == PageView.COMIC_PAGES ||
                                    (currentPage == PageView.READER && selectedItemType == MediaType.COMIC))) ||
                            (navPage == PageView.IMAGESETS && (currentPage == PageView.IMAGESET_IMAGES ||
                                    (currentPage == PageView.READER && selectedItemType == MediaType.IMAGESET)))

                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier
                            .testTag("nav_${navPage.key}")
                            .clickable { onNavigateSection(navPage) }
                            .padding(vertical = 4.dp)
                    ) {
                        Text(
                            text = label,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = if (isActive) FontWeight.SemiBold else FontWeight.Medium,
                            color = if (isActive) Color.White else Purple300.copy(alpha = 0.78f)
                        )
                        Spacer(modifier = Modifier.height(3.dp))
                        Box(
                            modifier = Modifier
                                .height(2.dp)
                                .width(28.dp)
                                .background(if (isActive) palette.accentBright else Color.Transparent)
                        )
                    }
                }
            }

            // Zone 3: Settings CTA
            val isSettings = currentPage == PageView.SETTINGS
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(if (isSettings) Color.White else palette.accent)
                    .testTag("nav_settings")
                    .clickable { onNavigateSection(PageView.SETTINGS) }
                    .padding(horizontal = 12.dp, vertical = 7.dp)
            ) {
                Text(
                    text = "Settings",
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.SemiBold,
                    color = if (isSettings) Purple950 else Color.White
                )
            }
        }
        HorizontalDivider(color = Purple900.copy(alpha = 0.6f))
    }
}

@Composable
private fun GalleryCategoryScreen(
    currentPage: PageView,
    activeMediaType: MediaType,
    directoryPath: String,
    categoryItems: List<MediaItem>,
    filteredCategoryItems: List<MediaItem>,
    searchQuery: String,
    isUploading: Boolean,
    uploadStatusText: String?,
    columnsCount: Int,
    appearance: com.example.model.AppearanceConfig,
    panicBlur: Boolean,
    onChangeSearch: (String) -> Unit,
    onOpenAdvancedSearch: () -> Unit,
    onLockScreen: () -> Unit,
    onPanicClose: () -> Unit,
    onMountFolder: () -> Unit,
    onAddFiles: () -> Unit,
    onSelectItem: (MediaItem) -> Unit,
    onRate: (String, Int) -> Unit,
    onToggleFavorite: (String, Boolean) -> Unit,
    onResumeReader: (MediaItem) -> Unit,
    onTagClick: (String) -> Unit
) {
    val palette = LocalDominaPalette.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 20.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp)
    ) {
        // Section Title & Lock / Panic Buttons
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Bottom
        ) {
            Column(modifier = Modifier.weight(1f)) {
                val pathPrefix = if (currentPage == PageView.IMAGES || currentPage == PageView.VIDEOS) {
                    "RECURSIVE PATH"
                } else {
                    "SUBFOLDER ARCHIVE"
                }
                Text(
                    text = "$pathPrefix • $directoryPath",
                    style = MaterialTheme.typography.labelSmall,
                    color = Purple400,
                    maxLines = 1
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = when (currentPage) {
                        PageView.IMAGES -> "Images Gallery"
                        PageView.VIDEOS -> "Videos Studio"
                        PageView.COMICS -> "Comics Library"
                        else -> "Image Sets Library"
                    },
                    style = MaterialTheme.typography.displayMedium,
                    color = Purple50
                )
            }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(DominaCard)
                        .border(1.dp, Purple900.copy(alpha = 0.6f), RoundedCornerShape(8.dp))
                        .testTag("gallery_lock_screen_btn")
                        .clickable { onLockScreen() }
                        .padding(horizontal = 10.dp, vertical = 7.dp)
                ) {
                    Text("Lock Screen", style = MaterialTheme.typography.bodySmall, color = Purple200)
                }

                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(Rose950.copy(alpha = 0.85f))
                        .border(1.dp, Rose700.copy(alpha = 0.6f), RoundedCornerShape(8.dp))
                        .clickable { onPanicClose() }
                        .padding(horizontal = 10.dp, vertical = 7.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.PowerSettingsNew,
                        contentDescription = null,
                        tint = Color(0xFFFFE4E6),
                        modifier = Modifier.size(12.dp)
                    )
                    Text("Panic Close", style = MaterialTheme.typography.bodySmall, color = Color(0xFFFFE4E6))
                }
            }
        }

        // FilterToolbar
        FilterToolbar(
            mediaType = activeMediaType,
            totalCount = categoryItems.size,
            filteredCount = filteredCategoryItems.size,
            searchQuery = searchQuery,
            isUploading = isUploading,
            uploadStatusText = uploadStatusText,
            onChangeSearch = onChangeSearch,
            onOpenAdvancedSearch = onOpenAdvancedSearch,
            onMountFolder = onMountFolder,
            onAddFiles = onAddFiles
        )

        // Media Grid or Empty State
        if (categoryItems.isEmpty()) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(DominaCard)
                    .border(1.dp, Purple800.copy(alpha = 0.6f), RoundedCornerShape(16.dp))
                    .padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Icon(
                    imageVector = Icons.Filled.CreateNewFolder,
                    contentDescription = null,
                    tint = Purple400,
                    modifier = Modifier.size(28.dp)
                )
                Text(
                    text = "No ${currentPage.key} indexed yet",
                    style = MaterialTheme.typography.displaySmall,
                    color = Purple50
                )
                Text(
                    text = "Use Mount Folder or Add Files in the search bar above, or run Rescan Library in Settings.",
                    style = MaterialTheme.typography.bodySmall,
                    color = Purple200.copy(alpha = 0.7f),
                    textAlign = TextAlign.Center
                )
            }
        } else if (filteredCategoryItems.isEmpty()) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(DominaCard)
                    .border(1.dp, Purple900.copy(alpha = 0.6f), RoundedCornerShape(16.dp))
                    .padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = "No matching media items found",
                    style = MaterialTheme.typography.displaySmall,
                    color = Purple50
                )
                Text(
                    text = "No items matched \"$searchQuery\". Clear the search bar or use Advanced Search for multi-star and boolean tag queries.",
                    style = MaterialTheme.typography.bodySmall,
                    color = Purple300.copy(alpha = 0.75f),
                    textAlign = TextAlign.Center
                )
                Text(
                    text = "Clear Search",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.White,
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(palette.accent)
                        .clickable { onChangeSearch("") }
                        .padding(horizontal = 16.dp, vertical = 9.dp)
                )
            }
        } else {
            MediaGridFlow(
                items = filteredCategoryItems,
                columnsCount = columnsCount,
                appearance = appearance,
                panicBlur = panicBlur,
                onSelectItem = onSelectItem,
                onRate = onRate,
                onToggleFavorite = onToggleFavorite,
                onResumeReader = onResumeReader,
                onTagClick = onTagClick
            )
        }
    }
}
