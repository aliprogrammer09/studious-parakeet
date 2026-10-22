package com.example.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.StarBorder
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.ui.theme.LocalDominaPalette
import com.example.ui.theme.Purple900

enum class StarRatingSize(val dp: Dp) {
    SM(15.dp),
    MD(18.dp),
    LG(22.dp)
}

@Composable
fun StarRating(
    rating: Int,
    onChange: ((Int) -> Unit)? = null,
    size: StarRatingSize = StarRatingSize.SM,
    itemTitle: String = "media item",
    readOnly: Boolean = false,
    modifier: Modifier = Modifier
) {
    val palette = LocalDominaPalette.current

    Row(
        modifier = modifier.semantics {
            contentDescription = "Rating for $itemTitle: $rating out of 5 stars"
        },
        horizontalArrangement = Arrangement.spacedBy(2.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        for (star in 1..5) {
            val isFilled = star <= rating
            val starModifier = if (!readOnly && onChange != null) {
                Modifier
                    .testTag("star_${itemTitle}_$star")
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) {
                        onChange(if (rating == star) 0 else star)
                    }
                    .padding(2.dp)
            } else {
                Modifier.padding(1.dp)
            }

            Icon(
                imageVector = if (isFilled) Icons.Filled.Star else Icons.Outlined.StarBorder,
                contentDescription = "Rate $itemTitle $star out of 5 stars",
                tint = if (isFilled) palette.starFill else Purple900.copy(alpha = 0.75f),
                modifier = starModifier.size(size.dp)
            )
        }
    }
}
