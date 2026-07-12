package com.kidslearning.app.ui.renderers

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kidslearning.app.domain.model.AnswerResult
import com.kidslearning.app.domain.model.SortIntoCategoriesSection
import com.kidslearning.app.ui.LocalSounds
import com.kidslearning.app.ui.Sounds
import com.kidslearning.app.ui.theme.OptionPalette
import com.kidslearning.app.ui.theme.Workbook

/**
 * Sort items into labelled buckets by dragging them in with a finger (or the tap
 * fallback: tap an item, then tap a box). Placed items move under their bucket;
 * tapping a placed item returns it to the tray.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SortIntoCategoriesActivity(
    section: SortIntoCategoriesSection,
    number: Int?,
    readAloud: Boolean,
    speak: (String) -> Unit,
    onAnswered: (AnswerResult) -> Unit,
) {
    // itemId -> categoryId (absent = still in the tray)
    val placement = remember(section.id) { mutableStateMapOf<String, String>() }
    var picked by remember(section.id) { mutableStateOf<String?>(null) }
    val dragState = rememberDragDropState(section.id)
    val sounds = LocalSounds.current

    ActivityScaffold(
        activity = section,
        number = number,
        prompt = section.instruction,
        readAloud = readAloud,
        speak = speak,
        onAnswered = onAnswered,
        checkEnabled = placement.size == section.items.size,
        score = {
            section.items.all { placement[it.id] == it.categoryId }
        },
        onIncorrectAttempt = {
            // Keep the correctly-placed items; return the wrong ones to the tray.
            val correct = section.items.associate { it.id to it.categoryId }
            placement.entries.retainAll { it.value == correct[it.key] }
        },
    ) {
        val unplaced = section.items.filter { it.id !in placement }

        // The tray of items still to sort.
        if (unplaced.isNotEmpty()) {
            Text(
                if (picked == null) "🖐 Drag each item into its box (or tap it, then tap a box)"
                else "👇 Now tap a box",
                style = MaterialTheme.typography.labelLarge,
                modifier = Modifier.padding(bottom = 4.dp),
            )
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.padding(bottom = 12.dp),
            ) {
                unplaced.forEachIndexed { index, item ->
                    val isPicked = picked == item.id
                    val scale by animateFloatAsState(if (isPicked) 1.08f else 1f, label = "pick")
                    Card(
                        onClick = { picked = if (isPicked) null else item.id },
                        colors = CardDefaults.cardColors(
                            containerColor = if (isPicked) MaterialTheme.colorScheme.primaryContainer
                            else OptionPalette[index % OptionPalette.size].first,
                        ),
                        border = if (isPicked)
                            BorderStroke(2.dp, MaterialTheme.colorScheme.primary) else null,
                        modifier = Modifier.sizeIn(minHeight = 44.dp)
                            .graphicsLayer { scaleX = scale; scaleY = scale }
                            .dragSource(
                                state = dragState,
                                id = item.id,
                                onPickUp = { sounds(Sounds.Effect.POP) },
                                onDrop = { target ->
                                    if (target != null) {
                                        placement[item.id] = target
                                        picked = null
                                        sounds(Sounds.Effect.POP)
                                    }
                                },
                            ),
                    ) {
                        Text(
                            "${item.emoji ?: ""} ${item.text}".trim(),
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                            style = MaterialTheme.typography.bodyLarge,
                        )
                    }
                }
            }
        }

        // The category buckets.
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
            section.categories.forEachIndexed { index, category ->
                val (container, accent) = OptionPalette[(index + 2) % OptionPalette.size]
                val placedHere = section.items.filter { placement[it.id] == category.id }
                val isDropHover = dragState.draggingId != null
                Card(
                    onClick = {
                        val p = picked
                        if (p != null) {
                            placement[p] = category.id
                            picked = null
                            sounds(Sounds.Effect.POP)
                        }
                    },
                    colors = CardDefaults.cardColors(containerColor = container),
                    border = BorderStroke(2.dp, accent.copy(alpha = if (isDropHover) 0.9f else 0.4f)),
                    modifier = Modifier.weight(1f).sizeIn(minHeight = 110.dp)
                        .dropTarget(dragState, category.id),
                ) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        Text(
                            "${category.emoji ?: "📦"} ${category.label}",
                            style = MaterialTheme.typography.titleSmall,
                        )
                        placedHere.forEach { item ->
                            Card(
                                onClick = { placement.remove(item.id) },
                                colors = CardDefaults.cardColors(containerColor = Workbook.CardWhite),
                                modifier = Modifier.fillMaxWidth().padding(top = 6.dp),
                            ) {
                                Text(
                                    "${item.emoji ?: ""} ${item.text}".trim(),
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                    style = MaterialTheme.typography.bodyMedium,
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
