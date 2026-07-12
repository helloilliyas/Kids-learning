package com.kidslearning.app.ui.renderers

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kidslearning.app.ui.theme.OptionPalette

/**
 * The shared answer card used by every activity type: each option gets its own
 * pastel colour from the rotating palette, an optional big emoji "picture", an
 * optional badge (order position, match number), and a springy select animation.
 * This one component is what makes all the activities feel colourful and alive
 * instead of a grey list.
 */
@Composable
fun OptionCard(
    text: String,
    index: Int,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    emoji: String? = null,
    badge: String? = null,
    minHeight: Int = 60,
) {
    val (container, accent) = OptionPalette[index % OptionPalette.size]
    val scale by animateFloatAsState(
        targetValue = if (selected) 1.03f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
        label = "cardScale",
    )
    val cardColor by animateColorAsState(
        targetValue = if (selected) MaterialTheme.colorScheme.primaryContainer else container,
        label = "cardColor",
    )

    Card(
        onClick = onClick,
        colors = CardDefaults.cardColors(containerColor = cardColor),
        border = if (selected) BorderStroke(3.dp, MaterialTheme.colorScheme.primary)
        else BorderStroke(2.dp, accent.copy(alpha = 0.25f)),
        elevation = CardDefaults.cardElevation(defaultElevation = if (selected) 6.dp else 2.dp),
        modifier = modifier
            .fillMaxWidth()
            .sizeIn(minHeight = minHeight.dp)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            },
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
        ) {
            if (emoji != null) {
                Text(emoji, fontSize = 30.sp, modifier = Modifier.padding(end = 12.dp))
            }
            Text(
                text,
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.weight(1f),
            )
            if (badge != null) {
                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(32.dp).padding(start = 0.dp),
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(
                            badge,
                            color = MaterialTheme.colorScheme.onPrimary,
                            style = MaterialTheme.typography.titleMedium,
                        )
                    }
                }
            }
        }
    }
}
