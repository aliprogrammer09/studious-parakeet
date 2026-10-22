package com.example.utils

import com.example.model.BookmarkProgress
import com.example.model.ComicProgressFilter
import com.example.model.DurationFilter
import com.example.model.FilterState
import com.example.model.MediaItem
import com.example.model.MediaType
import com.example.model.OrientationFilter
import com.example.model.ResolutionFilter
import com.example.model.SortField
import com.example.model.SortOrder
import java.util.Locale
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

data class ParsedTagQuery(
    val include: List<String>,
    val exclude: List<String>,
    val orGroups: List<List<String>>
)

object MediaLogic {

    /**
     * Parses a boolean tag query string such as:
     * "+gothic -daylight crimson OR velvet"
     * into required inclusions, exclusions, and OR groups.
     */
    fun parseTagExpression(expression: String): ParsedTagQuery {
        val include = mutableListOf<String>()
        val exclude = mutableListOf<String>()
        val orGroups = mutableListOf<List<String>>()

        val trimmed = expression.trim()
        if (trimmed.isEmpty()) return ParsedTagQuery(include, exclude, orGroups)

        val rawTokens = trimmed.split(Regex("\\s+")).filter { it.isNotEmpty() }
        var i = 0
        while (i < rawTokens.size) {
            if (i + 2 < rawTokens.size && rawTokens[i + 1].equals("OR", ignoreCase = true)) {
                val orGroup = mutableListOf(
                    rawTokens[i].replace(Regex("^[+-]"), "").lowercase(Locale.ROOT)
                )
                while (i + 2 < rawTokens.size && rawTokens[i + 1].equals("OR", ignoreCase = true)) {
                    orGroup.add(
                        rawTokens[i + 2].replace(Regex("^[+-]"), "").lowercase(Locale.ROOT)
                    )
                    i += 2
                }
                val validGroup = orGroup.filter { it.isNotEmpty() }
                if (validGroup.isNotEmpty()) {
                    orGroups.add(validGroup)
                }
                i += 1
                continue
            }

            val token = rawTokens[i]
            if (token.equals("OR", ignoreCase = true)) {
                i += 1
                continue
            }
            if (token.startsWith("-") && token.length > 1) {
                exclude.add(token.substring(1).lowercase(Locale.ROOT))
            } else if (token.startsWith("+") && token.length > 1) {
                include.add(token.substring(1).lowercase(Locale.ROOT))
            } else {
                include.add(token.lowercase(Locale.ROOT))
            }
            i += 1
        }

        return ParsedTagQuery(include, exclude, orGroups)
    }

    /**
     * Evaluates boolean AND, NOT, and OR logic across structured chips and tagExpression.
     */
    fun matchesTagFilter(itemTags: List<String>, filter: FilterState): Boolean {
        val normalizedItemTags = itemTags.map { it.lowercase(Locale.ROOT) }.toSet()
        val parsed = parseTagExpression(filter.tagExpression)

        val allExclude = filter.excludedTags.map { it.lowercase(Locale.ROOT) } + parsed.exclude
        for (exc in allExclude) {
            if (normalizedItemTags.contains(exc)) return false
        }

        if (filter.orTags.isNotEmpty()) {
            val unionOrTags = filter.includedTags.map { it.lowercase(Locale.ROOT) } +
                    filter.orTags.map { it.lowercase(Locale.ROOT) }
            val matchesUnionOr = unionOrTags.any { normalizedItemTags.contains(it) }
            if (!matchesUnionOr) return false

            if (parsed.include.isNotEmpty()) {
                if (filter.tagMatchMode == "any") {
                    if (!parsed.include.any { normalizedItemTags.contains(it) }) return false
                } else {
                    if (!parsed.include.all { normalizedItemTags.contains(it) }) return false
                }
            }
        } else {
            val allInclude = filter.includedTags.map { it.lowercase(Locale.ROOT) } + parsed.include
            if (allInclude.isNotEmpty()) {
                if (filter.tagMatchMode == "any") {
                    if (!allInclude.any { normalizedItemTags.contains(it) }) return false
                } else {
                    if (!allInclude.all { normalizedItemTags.contains(it) }) return false
                }
            }
        }

        for (group in parsed.orGroups) {
            if (group.isNotEmpty()) {
                val groupMatch = group.any { normalizedItemTags.contains(it) }
                if (!groupMatch) return false
            }
        }

        return true
    }

    fun matchesRatingFilter(itemRating: Int, selectedRatings: List<Int>): Boolean {
        if (selectedRatings.isEmpty()) return true
        return selectedRatings.contains(itemRating)
    }

    fun matchesSearchQuery(item: MediaItem, query: String): Boolean {
        val q = query.trim().lowercase(Locale.ROOT)
        if (q.isEmpty()) return true

        val haystack = buildString {
            append(item.title).append(' ')
            append(item.filename).append(' ')
            append(item.localPath).append(' ')
            append(item.artist).append(' ')
            append(item.series).append(' ')
            append(item.notes).append(' ')
            item.tags.forEach { append(it).append(' ') }
        }.lowercase(Locale.ROOT)

        val terms = q.split(Regex("\\s+")).filter { it.isNotEmpty() }
        return terms.all { haystack.contains(it) }
    }

    fun computeBookmarkProgress(
        lastReadPage: Int?,
        pageCount: Int?,
        readPages: List<Int>?
    ): BookmarkProgress {
        val total = pageCount ?: 0
        if (total <= 0) {
            return BookmarkProgress(0, 0, isStarted = false, isCompleted = false)
        }
        if (readPages != null) {
            val validSet = readPages.filter { it in 1..total }.toSet()
            val readCount = validSet.size
            if (readCount == 0) {
                return BookmarkProgress(0, 0, isStarted = false, isCompleted = false)
            }
            val percent = min(100, ((readCount.toDouble() / total.toDouble()) * 100.0).roundToInt())
            return BookmarkProgress(
                percent = percent,
                readCount = readCount,
                isStarted = readCount > 0,
                isCompleted = readCount >= total
            )
        }
        val last = lastReadPage ?: 1
        if (last <= 1) {
            return BookmarkProgress(0, 0, isStarted = false, isCompleted = false)
        }
        val clamped = last.coerceIn(1, total)
        val percent = ((clamped.toDouble() / total.toDouble()) * 100.0).roundToInt()
        return BookmarkProgress(
            percent = percent,
            readCount = clamped,
            isStarted = clamped > 1,
            isCompleted = clamped >= total
        )
    }

    fun filterAndSortMedia(items: List<MediaItem>, filter: FilterState): List<MediaItem> {
        val filtered = items.filter { item ->
            if (filter.selectedTypes.isNotEmpty() && !filter.selectedTypes.contains(item.type)) {
                return@filter false
            }
            if (filter.favoritesOnly && !item.isFavorite) {
                return@filter false
            }
            if (!matchesSearchQuery(item, filter.searchQuery)) return@filter false
            if (!matchesRatingFilter(item.rating, filter.selectedRatings)) return@filter false
            if (!matchesTagFilter(item.tags, filter)) return@filter false

            if (filter.orientation != OrientationFilter.ALL) {
                val ratio = item.width.toDouble() / max(1, item.height).toDouble()
                if (filter.orientation == OrientationFilter.PORTRAIT && ratio >= 0.95) return@filter false
                if (filter.orientation == OrientationFilter.LANDSCAPE && ratio <= 1.05) return@filter false
                if (filter.orientation == OrientationFilter.SQUARE && (ratio < 0.95 || ratio > 1.05)) return@filter false
            }

            if (filter.minResolution != ResolutionFilter.ALL) {
                val minDim = min(item.width, item.height)
                val maxDim = max(item.width, item.height)
                when (filter.minResolution) {
                    ResolutionFilter.HD -> if (maxDim < 1280 && minDim < 720) return@filter false
                    ResolutionFilter.FHD -> if (maxDim < 1920 && minDim < 1080) return@filter false
                    ResolutionFilter.QHD -> if (maxDim < 2560 && minDim < 1440) return@filter false
                    ResolutionFilter.UHD -> if (maxDim < 3840 && minDim < 2160) return@filter false
                    ResolutionFilter.ALL -> Unit
                }
            }

            if (filter.durationRange != DurationFilter.ALL) {
                if (item.type != MediaType.VIDEO) return@filter false
                val secs = item.durationSeconds
                when (filter.durationRange) {
                    DurationFilter.SHORT -> if (secs >= 60) return@filter false
                    DurationFilter.MEDIUM -> if (secs < 60 || secs > 600) return@filter false
                    DurationFilter.LONG -> if (secs <= 600) return@filter false
                    DurationFilter.ALL -> Unit
                }
            }

            if (filter.comicProgress != ComicProgressFilter.ALL) {
                if (item.type != MediaType.COMIC) return@filter false
                val prog = computeBookmarkProgress(item.lastReadPage, item.pageCount, item.readPages)
                when (filter.comicProgress) {
                    ComicProgressFilter.UNREAD -> if (prog.isStarted) return@filter false
                    ComicProgressFilter.READING -> if (!prog.isStarted || prog.isCompleted) return@filter false
                    ComicProgressFilter.COMPLETED -> if (!prog.isCompleted) return@filter false
                    ComicProgressFilter.ALL -> Unit
                }
            }

            true
        }

        val dir = if (filter.sortOrder == SortOrder.ASC) 1 else -1
        return filtered.sortedWith { a, b ->
            val cmp = when (filter.sortBy) {
                SortField.TITLE -> naturalCompare(a.title, b.title)
                SortField.RATING -> {
                    if (a.rating != b.rating) a.rating.compareTo(b.rating)
                    else naturalCompare(a.title, b.title)
                }
                SortField.SIZE -> a.sizeBytes.compareTo(b.sizeBytes)
                SortField.RESOLUTION -> (a.width.toLong() * a.height.toLong())
                    .compareTo(b.width.toLong() * b.height.toLong())
                SortField.PAGE_COUNT -> {
                    val aVal = if (a.type == MediaType.VIDEO) a.durationSeconds else a.pageCount
                    val bVal = if (b.type == MediaType.VIDEO) b.durationSeconds else b.pageCount
                    aVal.compareTo(bVal)
                }
                SortField.DATE_ADDED -> a.dateAdded.compareTo(b.dateAdded)
            }
            cmp * dir
        }
    }

    fun getTagSuggestions(
        allAvailableTags: List<String>,
        rawInput: String,
        excludeAlreadyChosen: List<String> = emptyList()
    ): List<String> {
        val excludedSet = excludeAlreadyChosen.map { it.lowercase(Locale.ROOT) }.toSet()
        val tokens = rawInput.split(Regex("\\s+"))
        val lastToken = (tokens.lastOrNull() ?: "")
            .replace(Regex("^[+-]"), "")
            .lowercase(Locale.ROOT)

        if (lastToken.isEmpty() || lastToken == "or") {
            return allAvailableTags.filter { !excludedSet.contains(it.lowercase(Locale.ROOT)) }.take(12)
        }

        val startsWith = mutableListOf<String>()
        val contains = mutableListOf<String>()
        for (tag in allAvailableTags) {
            val lower = tag.lowercase(Locale.ROOT)
            if (excludedSet.contains(lower)) continue
            if (lower.startsWith(lastToken)) {
                startsWith.add(tag)
            } else if (lower.contains(lastToken)) {
                contains.add(tag)
            }
        }
        return (startsWith + contains).take(12)
    }

    fun formatDuration(seconds: Int): String {
        if (seconds <= 0) return "00:00"
        val hrs = seconds / 3600
        val mins = (seconds % 3600) / 60
        val secs = seconds % 60
        val mm = String.format(Locale.US, "%02d", mins)
        val ss = String.format(Locale.US, "%02d", secs)
        return if (hrs > 0) "$hrs:$mm:$ss" else "$mm:$ss"
    }

    fun formatFileSize(bytes: Long): String {
        val safeBytes = max(0L, bytes)
        if (safeBytes < 1024L) return "$safeBytes B"
        val kb = safeBytes / 1024.0
        if (kb < 1024.0) return String.format(Locale.US, "%.1f KB", kb)
        val mb = kb / 1024.0
        if (mb < 1024.0) return String.format(Locale.US, "%.1f MB", mb)
        val gb = mb / 1024.0
        return String.format(Locale.US, "%.2f GB", gb)
    }

    fun naturalCompare(a: String, b: String): Int {
        val regex = Regex("(\\d+|\\D+)")
        val aParts = regex.findAll(a.lowercase(Locale.ROOT)).map { it.value }.toList()
        val bParts = regex.findAll(b.lowercase(Locale.ROOT)).map { it.value }.toList()
        val len = min(aParts.size, bParts.size)
        for (i in 0 until len) {
            val pA = aParts[i]
            val pB = bParts[i]
            val nA = pA.toBigIntegerOrNull()
            val nB = pB.toBigIntegerOrNull()
            val cmp = if (nA != null && nB != null) {
                nA.compareTo(nB)
            } else {
                pA.compareTo(pB)
            }
            if (cmp != 0) return cmp
        }
        return aParts.size.compareTo(bParts.size)
    }
}
