package com.example.model

enum class MediaType(val key: String) {
    IMAGE("image"),
    VIDEO("video"),
    COMIC("comic"),
    IMAGESET("imageset");

    companion object {
        fun fromKey(key: String): MediaType =
            entries.find { it.key.equals(key, ignoreCase = true) } ?: IMAGE
    }
}

enum class PageView(val key: String) {
    DASHBOARD("dashboard"),
    IMAGES("images"),
    VIDEOS("videos"),
    COMICS("comics"),
    IMAGESETS("imagesets"),
    ADVANCED_SEARCH("advanced-search"),
    IMAGE_DETAILS("image-details"),
    VIDEO_DETAILS("video-details"),
    COMIC_PAGES("comic-pages"),
    IMAGESET_IMAGES("imageset-images"),
    READER("reader"),
    SETTINGS("settings")
}

enum class SortField(val key: String, val label: String) {
    DATE_ADDED("dateAdded", "Date Added"),
    RATING("rating", "Star Rating"),
    TITLE("title", "Title (A–Z)"),
    SIZE("size", "File Size"),
    RESOLUTION("resolution", "Resolution (Px)"),
    PAGE_COUNT("pageCount", "Pages / Length")
}

enum class SortOrder(val key: String) {
    ASC("asc"),
    DESC("desc")
}

enum class ReaderFitMode(val key: String, val label: String) {
    WIDTH("width", "Fit Width"),
    HEIGHT("height", "Fit Height"),
    ORIGINAL("original", "Original Size"),
    DOUBLE("double", "Double-Page")
}

enum class ReaderDirection(val key: String, val label: String) {
    LTR("ltr", "LTR →"),
    RTL("rtl", "← RTL"),
    VERTICAL("vertical", "Scroll")
}

data class MediaPageItem(
    val id: String,
    val pageNumber: Int,
    val title: String,
    val filename: String,
    val localPath: String,
    val url: String,
    val width: Int = 1600,
    val height: Int = 2400,
    val sizeBytes: Long = 0L,
    val format: String = "PNG",
    val rating: Int = 0,
    val isFavorite: Boolean = false,
    val tags: List<String> = emptyList(),
    val notes: String = ""
)

data class MediaItem(
    val id: String,
    val type: MediaType,
    val title: String,
    val filename: String,
    val localPath: String,
    val folderPath: String,
    val thumbnailUrl: String = "",
    val mediaUrl: String = "",
    val rating: Int = 0,
    val isFavorite: Boolean = false,
    val notes: String = "",
    val tags: List<String> = emptyList(),
    val dateAdded: String = "2026-10-22T09:39:19.023Z",
    val sizeBytes: Long = 0L,
    val format: String = "PNG",
    val width: Int = 1920,
    val height: Int = 1080,
    val durationSeconds: Int = 0,
    val fps: Int = 60,
    val codec: String = "MP4",
    val pages: List<MediaPageItem> = emptyList(),
    val pageCount: Int = 1,
    val lastReadPage: Int = 1,
    val readPages: List<Int> = emptyList(),
    val lastReadAt: String = "",
    val artist: String = "",
    val series: String = ""
)

data class DirectoryConfig(
    val imagesPath: String = "/storage/emulated/0/Domina/vault/images",
    val videosPath: String = "/storage/emulated/0/Domina/vault/videos",
    val comicsPath: String = "/storage/emulated/0/Domina/vault/comics",
    val imageSetsPath: String = "/storage/emulated/0/Domina/vault/imagesets",
    val lastIndexedAt: String = "2026-10-22T09:39:19.023Z",
    val autoScanOnStartup: Boolean = true
)

data class AppearanceConfig(
    val accentColor: String = "amethyst", // violet, amethyst, indigo, crimson, emerald, amber, fuchsia
    val customAccentHex: String = "#9D4EDD",
    val accentIntensity: Int = 90, // 20..100
    val bgDarkness: String = "obsidian", // pitch-black, obsidian, charcoal
    val borderStyle: String = "glow", // glow, minimal, glass
    val gridDensity: String = "standard", // ultra-compact, compact, standard, spacious
    val gridColumns: Int = 0, // 0 = auto, or 2..6
    val cardGap: String = "standard", // tight, standard, relaxed
    val cardRadius: String = "rounded", // sharp, rounded, lux
    val thumbnailAspect: String = "portrait", // portrait, landscape, square
    val thumbnailFit: String = "cover", // cover, contain
    val showCardSize: Boolean = true,
    val showCardTags: Boolean = true,
    val showCardRating: Boolean = true,
    val showCardDimensions: Boolean = true,
    val hoverZoom: Boolean = true,
    val defaultReaderFit: ReaderFitMode = ReaderFitMode.HEIGHT,
    val defaultReaderDirection: ReaderDirection = ReaderDirection.LTR,
    val defaultVideoLoop: Boolean = false,
    val defaultAutoPlayNext: Boolean = true,
    val slideshowIntervalSeconds: Int = 3,
    val slideshowLoop: Boolean = true,
    val panicModeBlur: Boolean = false
)

enum class OrientationFilter(val key: String, val label: String) {
    ALL("all", "All Orientations"),
    PORTRAIT("portrait", "Portrait (Vertical)"),
    LANDSCAPE("landscape", "Landscape (Widescreen)"),
    SQUARE("square", "Square (1:1)")
}

enum class ResolutionFilter(val key: String, val label: String) {
    ALL("all", "Any Resolution"),
    HD("hd", "HD (720p+)"),
    FHD("fhd", "Full HD (1080p+)"),
    QHD("qhd", "QHD / 2K (1440p+)"),
    UHD("uhd", "4K UHD (2160p+)")
}

enum class DurationFilter(val key: String, val label: String) {
    ALL("all", "Any Duration"),
    SHORT("short", "Short (< 1 min)"),
    MEDIUM("medium", "Medium (1 – 10 min)"),
    LONG("long", "Long (> 10 min)")
}

enum class ComicProgressFilter(val key: String, val label: String) {
    ALL("all", "All Reading States"),
    UNREAD("unread", "Unread (0%)"),
    READING("reading", "In Progress (1% – 99%)"),
    COMPLETED("completed", "Completed (100%)")
}

data class FilterState(
    val searchQuery: String = "",
    val tagExpression: String = "",
    val includedTags: List<String> = emptyList(),
    val excludedTags: List<String> = emptyList(),
    val orTags: List<String> = emptyList(),
    val tagMatchMode: String = "all", // "all" (AND) or "any" (OR)
    val selectedRatings: List<Int> = emptyList(),
    val selectedTypes: List<MediaType> = emptyList(),
    val favoritesOnly: Boolean = false,
    val orientation: OrientationFilter = OrientationFilter.ALL,
    val minResolution: ResolutionFilter = ResolutionFilter.ALL,
    val durationRange: DurationFilter = DurationFilter.ALL,
    val comicProgress: ComicProgressFilter = ComicProgressFilter.ALL,
    val sortBy: SortField = SortField.DATE_ADDED,
    val sortOrder: SortOrder = SortOrder.DESC
)

data class SavedSearchPreset(
    val id: String,
    val name: String,
    val filter: FilterState,
    val createdAt: String
)

data class BookmarkProgress(
    val percent: Int,
    val readCount: Int,
    val isStarted: Boolean,
    val isCompleted: Boolean
)
