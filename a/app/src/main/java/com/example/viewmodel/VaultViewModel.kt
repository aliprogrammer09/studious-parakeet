package com.example.viewmodel

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.VaultRepository
import com.example.model.AppearanceConfig
import com.example.model.DirectoryConfig
import com.example.model.FilterState
import com.example.model.MediaItem
import com.example.model.MediaType
import com.example.model.PageView
import com.example.model.SavedSearchPreset
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class NavStackEntry(
    val page: PageView,
    val selectedItemId: String? = null,
    val readerStartPage: Int = 1,
    val selectedCollectionPageNumber: Int? = null
)

data class VaultUiState(
    val isLoadingVault: Boolean = true,
    val isScanning: Boolean = false,
    val isUploading: Boolean = false,
    val uploadStatusText: String? = null,
    val mediaItems: List<MediaItem> = emptyList(),
    val directories: DirectoryConfig = DirectoryConfig(),
    val appearance: AppearanceConfig = AppearanceConfig(),
    val currentPage: PageView = PageView.DASHBOARD,
    val selectedItemId: String? = null,
    val readerStartPage: Int = 1,
    val selectedCollectionPageNumber: Int? = null,
    val gallerySearchQuery: String = "",
    val advancedFilter: FilterState = FilterState(),
    val savedPresets: List<SavedSearchPreset> = emptyList(),
    val quickSearchOpen: Boolean = false,
    val privacyScreenLocked: Boolean = false,
    val panicClosed: Boolean = false
)

class VaultViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = VaultRepository(application)

    private val _uiState = MutableStateFlow(VaultUiState())
    val uiState: StateFlow<VaultUiState> = _uiState.asStateFlow()

    private val navBackStack = mutableListOf<NavStackEntry>()

    init {
        viewModelScope.launch {
            val (dirs, app) = repository.initializeVaultIfNeeded()
            _uiState.update {
                it.copy(
                    directories = dirs,
                    appearance = app,
                    isLoadingVault = false
                )
            }
        }

        viewModelScope.launch {
            repository.observeMediaItems().collect { items ->
                _uiState.update { it.copy(mediaItems = items, isLoadingVault = false) }
            }
        }

        viewModelScope.launch {
            repository.observeConfig().collect { (dirs, app) ->
                _uiState.update { it.copy(directories = dirs, appearance = app) }
            }
        }
    }

    fun navigateTo(
        page: PageView,
        itemId: String? = null,
        startPage: Int = 1,
        collectionPageNum: Int? = null,
        pushHistory: Boolean = true
    ) {
        val current = _uiState.value
        if (pushHistory && (current.currentPage != page ||
                    current.selectedItemId != itemId ||
                    current.selectedCollectionPageNumber != collectionPageNum)
        ) {
            navBackStack.add(
                NavStackEntry(
                    page = current.currentPage,
                    selectedItemId = current.selectedItemId,
                    readerStartPage = current.readerStartPage,
                    selectedCollectionPageNumber = current.selectedCollectionPageNumber
                )
            )
        }
        _uiState.update {
            it.copy(
                currentPage = page,
                selectedItemId = itemId,
                readerStartPage = startPage,
                selectedCollectionPageNumber = collectionPageNum
            )
        }
    }

    fun canNavigateBack(): Boolean =
        navBackStack.isNotEmpty() || _uiState.value.currentPage != PageView.DASHBOARD

    fun navigateBack(): Boolean {
        if (_uiState.value.quickSearchOpen) {
            setQuickSearchOpen(false)
            return true
        }
        if (navBackStack.isNotEmpty()) {
            val prev = navBackStack.removeAt(navBackStack.lastIndex)
            _uiState.update {
                it.copy(
                    currentPage = prev.page,
                    selectedItemId = prev.selectedItemId,
                    readerStartPage = prev.readerStartPage,
                    selectedCollectionPageNumber = prev.selectedCollectionPageNumber
                )
            }
            return true
        }
        if (_uiState.value.currentPage != PageView.DASHBOARD) {
            _uiState.update {
                it.copy(
                    currentPage = PageView.DASHBOARD,
                    selectedItemId = null,
                    selectedCollectionPageNumber = null
                )
            }
            return true
        }
        return false
    }

    fun selectItem(item: MediaItem) {
        when (item.type) {
            MediaType.IMAGE -> navigateTo(PageView.IMAGE_DETAILS, item.id)
            MediaType.VIDEO -> navigateTo(PageView.VIDEO_DETAILS, item.id)
            MediaType.COMIC -> navigateTo(PageView.COMIC_PAGES, item.id)
            MediaType.IMAGESET -> navigateTo(PageView.IMAGESET_IMAGES, item.id)
        }
    }

    fun resumeReader(item: MediaItem) {
        navigateTo(PageView.READER, item.id, startPage = item.lastReadPage.coerceAtLeast(1))
    }

    fun quickTagJump(tag: String) {
        _uiState.update {
            it.copy(
                advancedFilter = FilterState(includedTags = listOf(tag))
            )
        }
        navigateTo(PageView.ADVANCED_SEARCH)
    }

    fun setGallerySearchQuery(query: String) {
        _uiState.update { it.copy(gallerySearchQuery = query) }
    }

    fun updateAdvancedFilter(transform: (FilterState) -> FilterState) {
        _uiState.update { it.copy(advancedFilter = transform(it.advancedFilter)) }
    }

    fun resetAdvancedFilter() {
        _uiState.update { it.copy(advancedFilter = FilterState()) }
    }

    fun setQuickSearchOpen(open: Boolean) {
        _uiState.update { it.copy(quickSearchOpen = open) }
    }

    fun setPrivacyScreenLocked(locked: Boolean) {
        _uiState.update { it.copy(privacyScreenLocked = locked) }
    }

    fun triggerPanicClose() {
        _uiState.update { it.copy(panicClosed = true) }
    }

    fun restoreFromPanicClose() {
        _uiState.update { it.copy(panicClosed = false) }
    }

    fun rateItem(id: String, rating: Int) {
        val target = _uiState.value.mediaItems.find { it.id == id } ?: return
        val updated = target.copy(rating = rating.coerceIn(0, 5))
        _uiState.update { state ->
            state.copy(mediaItems = state.mediaItems.map { if (it.id == id) updated else it })
        }
        viewModelScope.launch {
            repository.updateMediaItem(updated)
        }
    }

    fun toggleFavorite(id: String, nextFav: Boolean) {
        val target = _uiState.value.mediaItems.find { it.id == id } ?: return
        val updated = target.copy(isFavorite = nextFav)
        _uiState.update { state ->
            state.copy(mediaItems = state.mediaItems.map { if (it.id == id) updated else it })
        }
        viewModelScope.launch {
            repository.updateMediaItem(updated)
        }
    }

    fun updateTags(id: String, nextTags: List<String>) {
        val target = _uiState.value.mediaItems.find { it.id == id } ?: return
        val updated = target.copy(tags = nextTags)
        _uiState.update { state ->
            state.copy(mediaItems = state.mediaItems.map { if (it.id == id) updated else it })
        }
        viewModelScope.launch {
            repository.updateMediaItem(updated)
        }
    }

    fun updateNotes(id: String, notes: String) {
        val target = _uiState.value.mediaItems.find { it.id == id } ?: return
        val updated = target.copy(notes = notes)
        _uiState.update { state ->
            state.copy(mediaItems = state.mediaItems.map { if (it.id == id) updated else it })
        }
        viewModelScope.launch {
            repository.updateMediaItem(updated)
        }
    }

    fun rateCollectionPage(collectionId: String, pageNumber: Int, rating: Int) {
        val target = _uiState.value.mediaItems.find { it.id == collectionId } ?: return
        val updatedPages = target.pages.map { p ->
            if (p.pageNumber == pageNumber) p.copy(rating = rating.coerceIn(0, 5)) else p
        }
        val updated = target.copy(pages = updatedPages)
        _uiState.update { state ->
            state.copy(mediaItems = state.mediaItems.map { if (it.id == collectionId) updated else it })
        }
        viewModelScope.launch {
            repository.updateMediaItem(updated)
        }
    }

    fun toggleFavoriteCollectionPage(collectionId: String, pageNumber: Int, nextFav: Boolean) {
        val target = _uiState.value.mediaItems.find { it.id == collectionId } ?: return
        val updatedPages = target.pages.map { p ->
            if (p.pageNumber == pageNumber) p.copy(isFavorite = nextFav) else p
        }
        val updated = target.copy(pages = updatedPages)
        _uiState.update { state ->
            state.copy(mediaItems = state.mediaItems.map { if (it.id == collectionId) updated else it })
        }
        viewModelScope.launch {
            repository.updateMediaItem(updated)
        }
    }

    fun updateCollectionPageTags(collectionId: String, pageNumber: Int, nextTags: List<String>) {
        val target = _uiState.value.mediaItems.find { it.id == collectionId } ?: return
        val updatedPages = target.pages.map { p ->
            if (p.pageNumber == pageNumber) p.copy(tags = nextTags) else p
        }
        val updated = target.copy(pages = updatedPages)
        _uiState.update { state ->
            state.copy(mediaItems = state.mediaItems.map { if (it.id == collectionId) updated else it })
        }
        viewModelScope.launch {
            repository.updateMediaItem(updated)
        }
    }

    fun updateCollectionPageNotes(collectionId: String, pageNumber: Int, notes: String) {
        val target = _uiState.value.mediaItems.find { it.id == collectionId } ?: return
        val updatedPages = target.pages.map { p ->
            if (p.pageNumber == pageNumber) p.copy(notes = notes) else p
        }
        val updated = target.copy(pages = updatedPages)
        _uiState.update { state ->
            state.copy(mediaItems = state.mediaItems.map { if (it.id == collectionId) updated else it })
        }
        viewModelScope.launch {
            repository.updateMediaItem(updated)
        }
    }

    fun updateBookmark(id: String, pageNumber: Int, newlyReadPages: List<Int>) {
        val target = _uiState.value.mediaItems.find { it.id == id } ?: return
        if (target.type != MediaType.COMIC) return
        val mergedRead = (target.readPages + newlyReadPages).distinct().sorted()
        val updated = target.copy(
            lastReadPage = pageNumber,
            readPages = mergedRead
        )
        _uiState.update { state ->
            state.copy(mediaItems = state.mediaItems.map { if (it.id == id) updated else it })
        }
        viewModelScope.launch {
            repository.updateMediaItem(updated)
        }
    }

    fun resetComicProgress(id: String) {
        val target = _uiState.value.mediaItems.find { it.id == id } ?: return
        val updated = target.copy(lastReadPage = 1, readPages = emptyList())
        _uiState.update { state ->
            state.copy(mediaItems = state.mediaItems.map { if (it.id == id) updated else it })
        }
        viewModelScope.launch {
            repository.updateMediaItem(updated)
        }
    }

    fun markAllComicRead(id: String, totalPages: Int) {
        val target = _uiState.value.mediaItems.find { it.id == id } ?: return
        val allPages = (1..totalPages).toList()
        val updated = target.copy(lastReadPage = totalPages, readPages = allPages)
        _uiState.update { state ->
            state.copy(mediaItems = state.mediaItems.map { if (it.id == id) updated else it })
        }
        viewModelScope.launch {
            repository.updateMediaItem(updated)
        }
    }

    fun deleteItem(id: String) {
        _uiState.update { state ->
            state.copy(
                mediaItems = state.mediaItems.filter { it.id != id },
                currentPage = if (state.selectedItemId == id) PageView.DASHBOARD else state.currentPage,
                selectedItemId = if (state.selectedItemId == id) null else state.selectedItemId
            )
        }
        viewModelScope.launch {
            repository.deleteMediaItem(id, deleteFromDisk = true)
        }
    }

    fun updateDirectories(nextDirs: DirectoryConfig) {
        _uiState.update { it.copy(directories = nextDirs) }
        viewModelScope.launch {
            repository.saveDirectories(nextDirs)
        }
    }

    fun updateAppearance(nextApp: AppearanceConfig) {
        _uiState.update { it.copy(appearance = nextApp) }
        viewModelScope.launch {
            repository.saveAppearance(nextApp)
        }
    }

    fun triggerServerScan(customDirs: DirectoryConfig? = null, onComplete: (Int) -> Unit = {}) {
        viewModelScope.launch {
            _uiState.update { it.copy(isScanning = true) }
            val count = repository.scanFilesystemLibrary(customDirs)
            _uiState.update { it.copy(isScanning = false) }
            onComplete(count)
        }
    }

    fun importLocalUris(uris: List<Uri>, targetType: MediaType, collectionName: String? = null) {
        if (uris.isEmpty()) return
        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isUploading = true,
                    uploadStatusText = "Importing ${uris.size} local file(s) into ${targetType.key} vault..."
                )
            }
            val imported = repository.importUrisToVault(uris, targetType, collectionName)
            _uiState.update {
                it.copy(
                    isUploading = false,
                    uploadStatusText = "Successfully indexed $imported file(s) into ${targetType.key} vault."
                )
            }
        }
    }

    fun clearUploadStatus() {
        _uiState.update { it.copy(uploadStatusText = null) }
    }

    fun savePreset(name: String) {
        val trimmed = name.trim()
        if (trimmed.isEmpty()) return
        val preset = SavedSearchPreset(
            id = "preset-${System.currentTimeMillis()}",
            name = trimmed,
            filter = _uiState.value.advancedFilter,
            createdAt = "2026-10-22"
        )
        _uiState.update { it.copy(savedPresets = listOf(preset) + it.savedPresets) }
    }

    fun deletePreset(id: String) {
        _uiState.update { state ->
            state.copy(savedPresets = state.savedPresets.filter { it.id != id })
        }
    }

    fun clearLibrary(onDone: () -> Unit = {}) {
        viewModelScope.launch {
            repository.clearAllVaultItems()
            _uiState.update {
                it.copy(
                    mediaItems = emptyList(),
                    gallerySearchQuery = "",
                    advancedFilter = FilterState()
                )
            }
            onDone()
        }
    }

    fun exportDatabaseJson(onResult: (String) -> Unit) {
        viewModelScope.launch {
            val json = repository.exportDatabaseJson()
            onResult(json)
        }
    }

    fun importDatabaseJson(json: String, onResult: (Boolean) -> Unit) {
        viewModelScope.launch {
            val ok = repository.importDatabaseJson(json)
            onResult(ok)
        }
    }
}
