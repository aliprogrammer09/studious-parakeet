package com.example

import com.example.model.BookmarkProgress
import com.example.model.FilterState
import com.example.model.MediaItem
import com.example.model.MediaType
import com.example.model.SortField
import com.example.model.SortOrder
import com.example.utils.ImageHeaderParser
import com.example.utils.MediaLogic
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ExampleUnitTest {

    @Test
    fun parseTagExpression_parsesIncludeExcludeAndOrGroups() {
        val parsed = MediaLogic.parseTagExpression("+gothic -daylight crimson OR velvet OR lace")
        assertEquals(listOf("gothic"), parsed.include)
        assertEquals(listOf("daylight"), parsed.exclude)
        assertEquals(listOf(listOf("crimson", "velvet", "lace")), parsed.orGroups)
    }

    @Test
    fun matchesTagFilter_evaluatesBooleanAndNotAndOrLogic() {
        val baseFilter = FilterState(
            tagExpression = "+obsidian -pastel ruby OR violet",
            includedTags = listOf("noir"),
            excludedTags = listOf("bright"),
            orTags = emptyList()
        )
        assertTrue(MediaLogic.matchesTagFilter(listOf("obsidian", "noir", "violet", "portrait"), baseFilter))
        assertFalse(MediaLogic.matchesTagFilter(listOf("obsidian", "noir", "portrait"), baseFilter))
        assertFalse(MediaLogic.matchesTagFilter(listOf("obsidian", "noir", "ruby", "pastel"), baseFilter))

        val orFilter = FilterState(
            orTags = listOf("amethyst", "crimson")
        )
        assertTrue(MediaLogic.matchesTagFilter(listOf("amethyst"), orFilter))
        assertTrue(MediaLogic.matchesTagFilter(listOf("crimson"), orFilter))
        assertFalse(MediaLogic.matchesTagFilter(listOf("unrelated"), orFilter))
    }

    @Test
    fun computeBookmarkProgress_calculatesActualReadPages() {
        assertEquals(
            BookmarkProgress(percent = 0, readCount = 0, isStarted = false, isCompleted = false),
            MediaLogic.computeBookmarkProgress(1, 10, emptyList())
        )
        assertEquals(
            BookmarkProgress(percent = 10, readCount = 1, isStarted = true, isCompleted = false),
            MediaLogic.computeBookmarkProgress(10, 10, listOf(10))
        )
        assertEquals(
            BookmarkProgress(percent = 50, readCount = 5, isStarted = true, isCompleted = false),
            MediaLogic.computeBookmarkProgress(8, 10, listOf(1, 2, 3, 5, 8))
        )
        assertEquals(
            BookmarkProgress(percent = 100, readCount = 4, isStarted = true, isCompleted = true),
            MediaLogic.computeBookmarkProgress(4, 4, listOf(1, 2, 3, 4))
        )
    }

    @Test
    fun filterAndSortMedia_filtersAndSortsDeterministically() {
        val sampleItems = listOf(
            MediaItem(
                id = "1",
                type = MediaType.IMAGE,
                title = "Crimson Cathedral",
                filename = "cathedral.png",
                localPath = "/vault/images/cathedral.png",
                folderPath = "/vault/images",
                rating = 4,
                tags = listOf("architecture", "crimson", "gothic"),
                dateAdded = "2026-10-10T10:00:00Z",
                sizeBytes = 1000L,
                format = "PNG",
                width = 1920,
                height = 1080
            ),
            MediaItem(
                id = "2",
                type = MediaType.IMAGE,
                title = "Obsidian Throne",
                filename = "throne.png",
                localPath = "/vault/images/throne.png",
                folderPath = "/vault/images",
                rating = 5,
                tags = listOf("architecture", "obsidian", "gothic"),
                dateAdded = "2026-10-15T10:00:00Z",
                sizeBytes = 2000L,
                format = "PNG",
                width = 1920,
                height = 1080
            ),
            MediaItem(
                id = "3",
                type = MediaType.IMAGE,
                title = "Sunlit Meadow",
                filename = "meadow.png",
                localPath = "/vault/images/meadow.png",
                folderPath = "/vault/images",
                rating = 3,
                tags = listOf("nature", "daylight"),
                dateAdded = "2026-10-18T10:00:00Z",
                sizeBytes = 1500L,
                format = "PNG",
                width = 1920,
                height = 1080
            )
        )

        val filter = FilterState(
            tagExpression = "+gothic -daylight",
            selectedRatings = listOf(4, 5),
            sortBy = SortField.RATING,
            sortOrder = SortOrder.DESC
        )
        val results = MediaLogic.filterAndSortMedia(sampleItems, filter)
        assertEquals(2, results.size)
        assertEquals("2", results[0].id)
        assertEquals("1", results[1].id)
    }

    @Test
    fun parseImageDimensionsFromBuffer_extractsPngDimensions() {
        val pngHeader = byteArrayOf(
            0x89.toByte(), 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A,
            0x00, 0x00, 0x00, 0x0D, 0x49, 0x48, 0x44, 0x52,
            0x00, 0x00, 0x0A, 0x00,
            0x00, 0x00, 0x05, 0xA0.toByte()
        )
        val dims = ImageHeaderParser.parseImageDimensionsFromBuffer(pngHeader)
        assertNotNull(dims)
        assertEquals(2560, dims?.width)
        assertEquals(1440, dims?.height)
    }
}
