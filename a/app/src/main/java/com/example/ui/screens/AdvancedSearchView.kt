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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.CheckBox
import androidx.compose.material.icons.filled.CheckBoxOutlineBlank
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Tag
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import com.example.model.AppearanceConfig
import com.example.model.ComicProgressFilter
import com.example.model.DurationFilter
import com.example.model.FilterState
import com.example.model.MediaItem
import com.example.model.MediaType
import com.example.model.OrientationFilter
import com.example.model.ResolutionFilter
import com.example.model.SavedSearchPreset
import com.example.model.SortField
import com.example.model.SortOrder
import com.example.ui.components.StarRating
import com.example.ui.components.StarRatingSize
import com.example.ui.theme.DominaCard
import com.example.ui.theme.DominaCardInput
import com.example.ui.theme.Emerald300
import com.example.ui.theme.Emerald700
import com.example.ui.theme.Emerald950
import com.example.ui.theme.LocalDominaPalette
import com.example.ui.theme.Purple100
import com.example.ui.theme.Purple200
import com.example.ui.theme.Purple300
import com.example.ui.theme.Purple400
import com.example.ui.theme.Purple50
import com.example.ui.theme.Purple500
import com.example.ui.theme.Purple700
import com.example.ui.theme.Purple800
import com.example.ui.theme.Purple900
import com.example.ui.theme.Rose300
import com.example.ui.theme.Rose700
import com.example.ui.theme.Rose950
import com.example.utils.MediaLogic

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AdvancedSearchView(
    items: List<MediaItem>,
    filter: FilterState,
    allTags: List<String>,
    savedPresets: List<SavedSearchPreset>,
    appearance: AppearanceConfig,
    columnsCount: Int,
    panicBlur: Boolean,
    onChangeFilter: ((FilterState) -> FilterState) -> Unit,
    onResetFilter: () -> Unit,
    onSavePreset: (String) -> Unit,
    onDeletePreset: (String) -> Unit,
    onSelectItem: (MediaItem) -> Unit,
    onResumeReader: (MediaItem) -> Unit,
    onRate: (String, Int) -> Unit,
    onToggleFavorite: (String, Boolean) -> Unit,
    onUpdateTags: (String, List<String>) -> Unit,
    onDelete: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val palette = LocalDominaPalette.current

    var showSuggestions by remember { mutableStateOf(false) }
    var presetName by remember { mutableStateOf("") }
    var batchMode by remember { mutableStateOf(false) }
    val selectedIds = remember { mutableStateListOf<String>() }
    var batchTagInput by remember { mutableStateOf("") }

    val suggestions = remember(allTags, filter) {
        MediaLogic.getTagSuggestions(
            allAvailableTags = allTags,
            rawInput = filter.tagExpression,
            excludeAlreadyChosen = filter.includedTags + filter.excludedTags + filter.orTags
        )
    }

    val filteredItems = remember(items, filter) {
        MediaLogic.filterAndSortMedia(items, filter)
    }

    fun applySuggestion(tag: String, mode: String) {
        when (mode) {
            "include" -> {
                onChangeFilter { prev ->
                    prev.copy(
                        includedTags = if (prev.includedTags.contains(tag)) prev.includedTags else prev.includedTags + tag,
                        excludedTags = prev.excludedTags.filter { it != tag }
                    )
                }
            }
            "exclude" -> {
                onChangeFilter { prev ->
                    prev.copy(
                        excludedTags = if (prev.excludedTags.contains(tag)) prev.excludedTags else prev.excludedTags + tag,
                        includedTags = prev.includedTags.filter { it != tag }
                    )
                }
            }
            "or" -> {
                onChangeFilter { prev ->
                    val mergedOr = (prev.orTags + prev.includedTags + tag).distinct()
                    prev.copy(
                        includedTags = emptyList(),
                        orTags = mergedOr,
                        excludedTags = prev.excludedTags.filter { it != tag }
                    )
                }
            }
            else -> {
                onChangeFilter { prev ->
                    val tokens = prev.tagExpression.trim().split(Regex("\\s+")).filter { it.isNotEmpty() }.toMutableList()
                    if (tokens.isEmpty()) {
                        prev.copy(tagExpression = "+$tag ")
                    } else {
                        val last = tokens.last()
                        val prefix = when {
                            last.startsWith("-") -> "-"
                            last.startsWith("+") -> "+"
                            else -> ""
                        }
                        tokens[tokens.lastIndex] = "$prefix$tag"
                        prev.copy(tagExpression = tokens.joinToString(" ") + " ")
                    }
                }
            }
        }
        showSuggestions = false
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 20.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp)
    ) {
        // Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Bottom
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "MULTI-DIMENSIONAL QUERY ENGINE",
                    style = MaterialTheme.typography.labelMedium,
                    color = Purple400
                )
                Text(
                    text = "Advanced Search & Tag Matrix",
                    style = MaterialTheme.typography.displayMedium,
                    color = Purple50
                )
            }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (batchMode) palette.accent else DominaCard)
                        .border(
                            1.dp,
                            if (batchMode) palette.accentBright else Purple900.copy(alpha = 0.6f),
                            RoundedCornerShape(8.dp)
                        )
                        .clickable {
                            batchMode = !batchMode
                            selectedIds.clear()
                        }
                        .padding(horizontal = 10.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(5.dp)
                ) {
                    Icon(
                        imageVector = if (batchMode) Icons.Filled.CheckBox else Icons.Filled.CheckBoxOutlineBlank,
                        contentDescription = null,
                        tint = if (batchMode) Color.White else Purple200,
                        modifier = Modifier.size(14.dp)
                    )
                    Text(
                        text = if (batchMode) "Exit Batch" else "Batch Select",
                        style = MaterialTheme.typography.bodySmall,
                        color = if (batchMode) Color.White else Purple200
                    )
                }

                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(DominaCard)
                        .border(1.dp, Purple900.copy(alpha = 0.6f), RoundedCornerShape(8.dp))
                        .testTag("reset_all_criteria_btn")
                        .clickable { onResetFilter() }
                        .padding(horizontal = 10.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(5.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.Refresh,
                        contentDescription = null,
                        tint = Purple400,
                        modifier = Modifier.size(14.dp)
                    )
                    Text(
                        text = "Reset",
                        style = MaterialTheme.typography.bodySmall,
                        color = Purple400
                    )
                }
            }
        }

        // Main Advanced Search Control Panel
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(DominaCard)
                .border(1.dp, Purple900.copy(alpha = 0.6f), RoundedCornerShape(16.dp))
                .padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Keyword Search
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    text = "Title, Filename, Path, Artist, or Notes",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                    color = Purple200
                )
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(DominaCardInput)
                        .border(1.dp, Purple900.copy(alpha = 0.6f), RoundedCornerShape(8.dp))
                        .padding(horizontal = 12.dp, vertical = 10.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Search,
                            contentDescription = null,
                            tint = palette.accentBright,
                            modifier = Modifier.size(16.dp)
                        )
                        Box(modifier = Modifier.weight(1f)) {
                            if (filter.searchQuery.isEmpty()) {
                                Text(
                                    text = "Search across all titles, filenames, artists, or local paths...",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = Purple400.copy(alpha = 0.5f)
                                )
                            }
                            BasicTextField(
                                value = filter.searchQuery,
                                onValueChange = { q -> onChangeFilter { it.copy(searchQuery = q) } },
                                singleLine = true,
                                textStyle = MaterialTheme.typography.bodyMedium.copy(color = Purple50),
                                cursorBrush = SolidColor(palette.accentBright),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("adv_keyword_search")
                            )
                        }
                        if (filter.searchQuery.isNotEmpty()) {
                            Icon(
                                imageVector = Icons.Filled.Close,
                                contentDescription = "Clear keyword search",
                                tint = Purple400,
                                modifier = Modifier
                                    .size(15.dp)
                                    .clickable { onChangeFilter { it.copy(searchQuery = "") } }
                            )
                        }
                    }
                }
            }

            // Boolean Tag Query with Click-to-Reopen Auto-Complete Dropdown
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Boolean Tag Query (+include, -exclude, tagA OR tagB)",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium,
                        color = Purple200,
                        modifier = Modifier.weight(1f)
                    )
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(DominaCardInput)
                            .border(1.dp, Purple900.copy(alpha = 0.6f), RoundedCornerShape(6.dp))
                            .padding(2.dp)
                    ) {
                        val isAll = filter.tagMatchMode != "any"
                        Text(
                            text = "ALL (AND)",
                            style = MaterialTheme.typography.labelSmall,
                            color = if (isAll) Color.White else Purple300.copy(alpha = 0.7f),
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(if (isAll) palette.accent else Color.Transparent)
                                .clickable { onChangeFilter { it.copy(tagMatchMode = "all") } }
                                .padding(horizontal = 8.dp, vertical = 3.dp)
                        )
                        Text(
                            text = "ANY (OR)",
                            style = MaterialTheme.typography.labelSmall,
                            color = if (!isAll) Color.White else Purple300.copy(alpha = 0.7f),
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(if (!isAll) palette.accent else Color.Transparent)
                                .clickable { onChangeFilter { it.copy(tagMatchMode = "any") } }
                                .padding(horizontal = 8.dp, vertical = 3.dp)
                        )
                    }
                }

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(DominaCardInput)
                        .border(1.dp, Purple900.copy(alpha = 0.6f), RoundedCornerShape(8.dp))
                        .clickable { showSuggestions = true }
                        .padding(horizontal = 12.dp, vertical = 10.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Tag,
                            contentDescription = null,
                            tint = Purple400,
                            modifier = Modifier.size(16.dp)
                        )
                        Box(modifier = Modifier.weight(1f)) {
                            if (filter.tagExpression.isEmpty()) {
                                Text(
                                    text = "Tap to browse tags or type: +violet -daylight gothic OR noir",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = Purple400.copy(alpha = 0.5f)
                                )
                            }
                            BasicTextField(
                                value = filter.tagExpression,
                                onValueChange = { expr ->
                                    onChangeFilter { it.copy(tagExpression = expr) }
                                    showSuggestions = true
                                },
                                singleLine = true,
                                textStyle = MaterialTheme.typography.labelMedium.copy(color = Purple50),
                                cursorBrush = SolidColor(Purple400),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("adv_tag_expression")
                            )
                        }
                        Text(
                            text = if (showSuggestions) "Hide Tags" else "Browse Tags",
                            style = MaterialTheme.typography.labelSmall,
                            color = Purple400,
                            modifier = Modifier.clickable { showSuggestions = !showSuggestions }
                        )
                        if (filter.tagExpression.isNotEmpty()) {
                            Icon(
                                imageVector = Icons.Filled.Close,
                                contentDescription = "Clear tag expression",
                                tint = Purple400,
                                modifier = Modifier
                                    .size(15.dp)
                                    .clickable {
                                        onChangeFilter { it.copy(tagExpression = "") }
                                        showSuggestions = true
                                    }
                            )
                        }
                    }
                }

                // Tag Auto-Complete Suggestions Panel
                if (showSuggestions && suggestions.isNotEmpty()) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color(0xFF130A24))
                            .border(1.dp, Purple700.copy(alpha = 0.7f), RoundedCornerShape(12.dp))
                            .padding(10.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Tag Suggestions (${suggestions.size})",
                                style = MaterialTheme.typography.labelSmall,
                                color = Purple300.copy(alpha = 0.85f)
                            )
                            Text(
                                text = "Close",
                                style = MaterialTheme.typography.labelSmall,
                                color = Purple400,
                                modifier = Modifier.clickable { showSuggestions = false }
                            )
                        }
                        HorizontalDivider(color = Purple900.copy(alpha = 0.5f))

                        suggestions.take(8).forEach { tag ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "#$tag",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = Purple100,
                                    modifier = Modifier
                                        .weight(1f)
                                        .clickable { applySuggestion(tag, "expression") }
                                )
                                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Text(
                                        text = "+Include",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = Emerald300,
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(4.dp))
                                            .background(Emerald950.copy(alpha = 0.8f))
                                            .border(1.dp, Emerald700.copy(alpha = 0.5f), RoundedCornerShape(4.dp))
                                            .clickable { applySuggestion(tag, "include") }
                                            .padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                    Text(
                                        text = "OR",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = Purple200,
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(4.dp))
                                            .background(Purple900.copy(alpha = 0.8f))
                                            .border(1.dp, Purple700.copy(alpha = 0.6f), RoundedCornerShape(4.dp))
                                            .clickable { applySuggestion(tag, "or") }
                                            .padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                    Text(
                                        text = "-Exclude",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = Rose300,
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(4.dp))
                                            .background(Rose950.copy(alpha = 0.8f))
                                            .border(1.dp, Rose700.copy(alpha = 0.5f), RoundedCornerShape(4.dp))
                                            .clickable { applySuggestion(tag, "exclude") }
                                            .padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            HorizontalDivider(color = Purple900.copy(alpha = 0.4f))

            // Media Categories & Concurrent Multi-Star Ratings & Favorites
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = "Media Categories",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                    color = Purple200
                )
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf(
                        MediaType.IMAGE to "Images",
                        MediaType.VIDEO to "Videos",
                        MediaType.COMIC to "Comics",
                        MediaType.IMAGESET to "Image Sets"
                    ).forEach { (type, label) ->
                        val active = filter.selectedTypes.contains(type)
                        Text(
                            text = label,
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Medium,
                            color = if (active) Color.White else Purple300.copy(alpha = 0.8f),
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (active) palette.accent else DominaCardInput)
                                .border(
                                    1.dp,
                                    if (active) Purple400 else Purple900.copy(alpha = 0.6f),
                                    RoundedCornerShape(8.dp)
                                )
                                .clickable {
                                    onChangeFilter { prev ->
                                        val next = if (active) prev.selectedTypes - type else prev.selectedTypes + type
                                        prev.copy(selectedTypes = next)
                                    }
                                }
                                .padding(horizontal = 12.dp, vertical = 7.dp)
                        )
                    }

                    // Favorites Only Toggle
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (filter.favoritesOnly) Color(0xFF9333EA) else DominaCardInput)
                            .border(
                                1.dp,
                                if (filter.favoritesOnly) Purple400 else Purple900.copy(alpha = 0.6f),
                                RoundedCornerShape(8.dp)
                            )
                            .clickable {
                                onChangeFilter { it.copy(favoritesOnly = !it.favoritesOnly) }
                            }
                            .padding(horizontal = 12.dp, vertical = 7.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(5.dp)
                    ) {
                        Icon(
                            imageVector = if (filter.favoritesOnly) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
                            contentDescription = null,
                            tint = if (filter.favoritesOnly) Color.White else Purple300,
                            modifier = Modifier.size(13.dp)
                        )
                        Text(
                            text = "Favorites Only",
                            style = MaterialTheme.typography.bodySmall,
                            color = if (filter.favoritesOnly) Color.White else Purple300
                        )
                    }
                }

                Text(
                    text = "Concurrent Star Ratings (Multi-Select)",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                    color = Purple200
                )
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    (0..5).forEach { star ->
                        val active = filter.selectedRatings.contains(star)
                        Row(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (active) palette.accent else DominaCardInput)
                                .border(
                                    1.dp,
                                    if (active) Purple400 else Purple900.copy(alpha = 0.6f),
                                    RoundedCornerShape(8.dp)
                                )
                                .testTag("filter_rating_$star")
                                .clickable {
                                    onChangeFilter { prev ->
                                        val next = if (active) {
                                            prev.selectedRatings - star
                                        } else {
                                            (prev.selectedRatings + star).sorted()
                                        }
                                        prev.copy(selectedRatings = next)
                                    }
                                }
                                .padding(horizontal = 10.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(
                                text = if (star == 0) "Unrated" else "$star",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = if (active) FontWeight.Bold else FontWeight.Normal,
                                color = if (active) Color.White else Purple300
                            )
                            if (star > 0) {
                                Icon(
                                    imageVector = Icons.Filled.Star,
                                    contentDescription = null,
                                    tint = if (active) Color.White else Purple400.copy(alpha = 0.7f),
                                    modifier = Modifier.size(12.dp)
                                )
                            }
                        }
                    }
                }
            }

            HorizontalDivider(color = Purple900.copy(alpha = 0.4f))

            // Orientation, Resolution, Duration, Comic Progress, Sort OrderSelectors
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                FilterDropdownChip(
                    title = "Orientation",
                    currentLabel = filter.orientation.label,
                    options = OrientationFilter.entries.map { it.label to it },
                    onSelect = { sel -> onChangeFilter { it.copy(orientation = sel) } }
                )
                FilterDropdownChip(
                    title = "Min Resolution",
                    currentLabel = filter.minResolution.label,
                    options = ResolutionFilter.entries.map { it.label to it },
                    onSelect = { sel -> onChangeFilter { it.copy(minResolution = sel) } }
                )
                FilterDropdownChip(
                    title = "Video Duration",
                    currentLabel = filter.durationRange.label,
                    options = DurationFilter.entries.map { it.label to it },
                    onSelect = { sel -> onChangeFilter { it.copy(durationRange = sel) } }
                )
                FilterDropdownChip(
                    title = "Comic Status",
                    currentLabel = filter.comicProgress.label,
                    options = ComicProgressFilter.entries.map { it.label to it },
                    onSelect = { sel -> onChangeFilter { it.copy(comicProgress = sel) } }
                )
                FilterDropdownChip(
                    title = "Sort By",
                    currentLabel = filter.sortBy.label,
                    options = SortField.entries.map { it.label to it },
                    onSelect = { sel -> onChangeFilter { it.copy(sortBy = sel) } }
                )

                // Sort Direction Button
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(DominaCardInput)
                        .border(1.dp, Purple900.copy(alpha = 0.6f), RoundedCornerShape(8.dp))
                        .clickable {
                            onChangeFilter { prev ->
                                prev.copy(
                                    sortOrder = if (prev.sortOrder == SortOrder.ASC) SortOrder.DESC else SortOrder.ASC
                                )
                            }
                        }
                        .padding(horizontal = 10.dp, vertical = 9.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        imageVector = if (filter.sortOrder == SortOrder.ASC) Icons.Filled.ArrowUpward else Icons.Filled.ArrowDownward,
                        contentDescription = "Toggle sort direction",
                        tint = Purple200,
                        modifier = Modifier.size(15.dp)
                    )
                    Text(
                        text = filter.sortOrder.key.uppercase(),
                        style = MaterialTheme.typography.labelSmall,
                        color = Purple200
                    )
                }
            }

            HorizontalDivider(color = Purple900.copy(alpha = 0.4f))

            // Interactive Tag Bank (Tap to cycle: +Include -> OR -> -Exclude -> Off)
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Interactive Tag Bank (Tap to cycle: +Include → OR → -Exclude → Off):",
                        style = MaterialTheme.typography.bodySmall,
                        color = Purple300.copy(alpha = 0.85f),
                        modifier = Modifier.weight(1f)
                    )
                    Text(
                        text = "Matching: ${filteredItems.size} of ${items.size}",
                        style = MaterialTheme.typography.labelMedium,
                        color = Purple400
                    )
                }

                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    allTags.forEach { tag ->
                        val isInc = filter.includedTags.contains(tag)
                        val isExc = filter.excludedTags.contains(tag)
                        val isOr = filter.orTags.contains(tag)

                        val bg = when {
                            isInc -> Emerald950.copy(alpha = 0.85f)
                            isOr -> Purple900.copy(alpha = 0.85f)
                            isExc -> Rose950.copy(alpha = 0.85f)
                            else -> DominaCardInput
                        }
                        val border = when {
                            isInc -> Emerald700.copy(alpha = 0.65f)
                            isOr -> Purple500.copy(alpha = 0.7f)
                            isExc -> Rose700.copy(alpha = 0.65f)
                            else -> Purple900.copy(alpha = 0.6f)
                        }
                        val textColor = when {
                            isInc -> Emerald300
                            isOr -> Purple100
                            isExc -> Rose300
                            else -> Purple300.copy(alpha = 0.85f)
                        }
                        val label = when {
                            isInc -> "+$tag"
                            isOr -> "OR:$tag"
                            isExc -> "-$tag"
                            else -> "#$tag"
                        }

                        Text(
                            text = label,
                            style = MaterialTheme.typography.labelSmall.copy(
                                textDecoration = if (isExc) TextDecoration.LineThrough else TextDecoration.None
                            ),
                            color = textColor,
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(bg)
                                .border(1.dp, border, RoundedCornerShape(6.dp))
                                .testTag("tag_chip_$tag")
                                .clickable {
                                    if (!isInc && !isOr && !isExc) {
                                        applySuggestion(tag, "include")
                                    } else if (isInc) {
                                        onChangeFilter { prev ->
                                            prev.copy(
                                                includedTags = prev.includedTags - tag,
                                                orTags = prev.orTags + tag
                                            )
                                        }
                                    } else if (isOr) {
                                        onChangeFilter { prev ->
                                            prev.copy(
                                                orTags = prev.orTags - tag,
                                                excludedTags = prev.excludedTags + tag
                                            )
                                        }
                                    } else {
                                        onChangeFilter { prev ->
                                            prev.copy(excludedTags = prev.excludedTags - tag)
                                        }
                                    }
                                }
                                .padding(horizontal = 9.dp, vertical = 5.dp)
                        )
                    }
                }
            }

            HorizontalDivider(color = Purple900.copy(alpha = 0.4f))

            // Saved Search Presets Row
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.Bookmark,
                        contentDescription = null,
                        tint = Purple400,
                        modifier = Modifier.size(14.dp)
                    )
                    Text(
                        text = "Saved Search Presets:",
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Medium,
                        color = Purple300
                    )
                }

                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    savedPresets.forEach { preset ->
                        Row(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(DominaCardInput)
                                .border(1.dp, Purple800.copy(alpha = 0.7f), RoundedCornerShape(8.dp))
                                .padding(horizontal = 10.dp, vertical = 5.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = preset.name,
                                style = MaterialTheme.typography.bodySmall,
                                color = Purple100,
                                modifier = Modifier.clickable {
                                    onChangeFilter { preset.filter }
                                }
                            )
                            Icon(
                                imageVector = Icons.Filled.Close,
                                contentDescription = "Delete preset",
                                tint = Purple400,
                                modifier = Modifier
                                    .size(12.dp)
                                    .clickable { onDeletePreset(preset.id) }
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
                            .background(DominaCardInput)
                            .border(1.dp, Purple900.copy(alpha = 0.6f), RoundedCornerShape(8.dp))
                            .padding(horizontal = 10.dp, vertical = 8.dp)
                    ) {
                        if (presetName.isEmpty()) {
                            Text(
                                text = "Preset name...",
                                style = MaterialTheme.typography.bodySmall,
                                color = Purple400.copy(alpha = 0.5f)
                            )
                        }
                        BasicTextField(
                            value = presetName,
                            onValueChange = { presetName = it },
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
                                if (presetName.isNotBlank()) {
                                    onSavePreset(presetName)
                                    presetName = ""
                                }
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
                            text = "Save Preset",
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.SemiBold,
                            color = Color.White
                        )
                    }
                }
            }
        }

        // Batch Action Bar (when Batch Mode is active)
        if (batchMode) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0xFF1B0E33))
                    .border(1.dp, palette.accentBright, RoundedCornerShape(12.dp))
                    .padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Selected: ${selectedIds.size} items",
                        style = MaterialTheme.typography.labelMedium,
                        color = Purple100
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text(
                            text = "Select All (${filteredItems.size})",
                            style = MaterialTheme.typography.bodySmall,
                            color = Purple400,
                            modifier = Modifier.clickable {
                                selectedIds.clear()
                                selectedIds.addAll(filteredItems.map { it.id })
                            }
                        )
                        Text(
                            text = "Clear",
                            style = MaterialTheme.typography.bodySmall,
                            color = Purple300,
                            modifier = Modifier.clickable { selectedIds.clear() }
                        )
                    }
                }

                if (selectedIds.isNotEmpty()) {
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text("Batch Rate:", style = MaterialTheme.typography.bodySmall, color = Purple200)
                            StarRating(
                                rating = 0,
                                onChange = { r ->
                                    selectedIds.forEach { id -> onRate(id, r) }
                                },
                                size = StarRatingSize.MD
                            )
                        }

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .width(140.dp)
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(DominaCardInput)
                                    .border(1.dp, Purple800, RoundedCornerShape(6.dp))
                                    .padding(horizontal = 8.dp, vertical = 5.dp)
                            ) {
                                if (batchTagInput.isEmpty()) {
                                    Text("Add tag...", style = MaterialTheme.typography.bodySmall, color = Purple400.copy(alpha = 0.5f))
                                }
                                BasicTextField(
                                    value = batchTagInput,
                                    onValueChange = { batchTagInput = it },
                                    singleLine = true,
                                    textStyle = MaterialTheme.typography.bodySmall.copy(color = Purple100),
                                    cursorBrush = SolidColor(palette.accentBright),
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                            Text(
                                text = "Apply Tag",
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.SemiBold,
                                color = Color.White,
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(palette.accent)
                                    .clickable {
                                        val cleaned = batchTagInput.trim().lowercase().removePrefix("#").replace(Regex("\\s+"), "-")
                                        if (cleaned.isNotEmpty()) {
                                            selectedIds.forEach { id ->
                                                val t = items.find { it.id == id }
                                                if (t != null && !t.tags.contains(cleaned)) {
                                                    onUpdateTags(id, t.tags + cleaned)
                                                }
                                            }
                                            batchTagInput = ""
                                        }
                                    }
                                    .padding(horizontal = 10.dp, vertical = 6.dp)
                            )
                        }

                        Row(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(Rose950.copy(alpha = 0.85f))
                                .border(1.dp, Rose700.copy(alpha = 0.6f), RoundedCornerShape(6.dp))
                                .clickable {
                                    val copy = selectedIds.toList()
                                    selectedIds.clear()
                                    copy.forEach { onDelete(it) }
                                }
                                .padding(horizontal = 10.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Delete,
                                contentDescription = null,
                                tint = Rose300,
                                modifier = Modifier.size(13.dp)
                            )
                            Text("Delete Selected", style = MaterialTheme.typography.bodySmall, color = Rose300)
                        }
                    }
                }
            }
        }

        // Results Grid
        if (filteredItems.isEmpty()) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(DominaCard)
                    .border(1.dp, Purple900.copy(alpha = 0.5f), RoundedCornerShape(16.dp))
                    .padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Icon(
                    imageVector = Icons.Filled.Tune,
                    contentDescription = null,
                    tint = palette.accentBright,
                    modifier = Modifier.size(28.dp)
                )
                Text(
                    text = "No matching media items found",
                    style = MaterialTheme.typography.displaySmall,
                    color = Purple50
                )
                Text(
                    text = "Adjust your boolean tag query, star ratings, or resolution/orientation filters above to broaden the results.",
                    style = MaterialTheme.typography.bodySmall,
                    color = Purple300.copy(alpha = 0.7f)
                )
                Text(
                    text = "Reset All Search Filters",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.White,
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(palette.accent)
                        .clickable { onResetFilter() }
                        .padding(horizontal = 16.dp, vertical = 9.dp)
                )
            }
        } else {
            MediaGridFlow(
                items = filteredItems,
                columnsCount = columnsCount,
                appearance = appearance,
                panicBlur = panicBlur,
                onSelectItem = onSelectItem,
                onRate = onRate,
                onToggleFavorite = onToggleFavorite,
                onResumeReader = onResumeReader,
                onTagClick = { tag -> applySuggestion(tag, "include") },
                selectable = batchMode,
                selectedIds = selectedIds,
                onToggleSelect = { id ->
                    if (selectedIds.contains(id)) selectedIds.remove(id) else selectedIds.add(id)
                }
            )
        }
    }
}

@Composable
private fun <T> FilterDropdownChip(
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
                .background(DominaCardInput)
                .border(1.dp, Purple900.copy(alpha = 0.6f), RoundedCornerShape(8.dp))
                .clickable { expanded = true }
                .padding(horizontal = 10.dp, vertical = 6.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.labelSmall,
                color = Purple300.copy(alpha = 0.7f)
            )
            Text(
                text = currentLabel,
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.Medium,
                color = Purple100
            )
        }

        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            modifier = Modifier.background(Color(0xFF140A26))
        ) {
            options.forEach { (label, value) ->
                DropdownMenuItem(
                    text = {
                        Text(
                            text = label,
                            style = MaterialTheme.typography.bodySmall,
                            color = Purple100
                        )
                    },
                    onClick = {
                        onSelect(value)
                        expanded = false
                    }
                )
            }
        }
    }
}
