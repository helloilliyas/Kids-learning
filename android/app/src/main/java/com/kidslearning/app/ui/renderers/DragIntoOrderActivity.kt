package com.kidslearning.app.ui.renderers

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.background
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import com.kidslearning.app.domain.model.AnswerResult
import com.kidslearning.app.domain.model.DragIntoOrderSection
import com.kidslearning.app.domain.model.LocalScoring
import com.kidslearning.app.ui.LocalSounds
import com.kidslearning.app.ui.Sounds
import com.kidslearning.app.ui.theme.OptionPalette
import com.kidslearning.app.ui.theme.Workbook
import kotlin.random.Random

/**
 * True drag-to-reorder sequencing: hold a card and drag it up or down the list;
 * the other cards make room as it passes their midpoint. The current position of
 * every card is its answer, so the child can Check at any time.
 */
@Composable
fun DragIntoOrderActivity(
    section: DragIntoOrderSection,
    number: Int?,
    readAloud: Boolean,
    speak: (String) -> Unit,
    onAnswered: (AnswerResult) -> Unit,
) {
    val byId = remember(section.id) { section.items.associateBy { it.id } }
    val order = remember(section.id) {
        val shuffled = section.items.map { it.id }
            .shuffled(Random(section.id.hashCode()))
            .toMutableList()
        // Never present the already-correct sequence as the starting layout.
        if (shuffled == section.correctOrder) shuffled.reverse()
        mutableStateListOf<String>().also { it.addAll(shuffled) }
    }
    var draggedId by remember(section.id) { mutableStateOf<String?>(null) }
    var dragDelta by remember(section.id) { mutableFloatStateOf(0f) }
    val rowHeights = remember(section.id) { mutableStateMapOf<String, Float>() }
    val sounds = LocalSounds.current

    ActivityScaffold(
        activity = section,
        number = number,
        prompt = section.instruction,
        readAloud = readAloud,
        speak = speak,
        onAnswered = onAnswered,
        checkEnabled = true,
        score = { LocalScoring.scoreDragIntoOrder(section, order.toList()) },
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text("🖐 Hold a card and drag it into place.", style = MaterialTheme.typography.labelLarge)
            order.forEachIndexed { position, itemId ->
                val item = byId[itemId]
                if (item != null) key(itemId) {
                    val isDragged = draggedId == itemId
                    val (container, accent) = OptionPalette[
                        section.items.indexOfFirst { it.id == itemId } % OptionPalette.size]
                    Card(
                        colors = CardDefaults.cardColors(containerColor = container),
                        border = BorderStroke(2.dp, accent.copy(alpha = if (isDragged) 0.9f else 0.4f)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .onGloballyPositioned { rowHeights[itemId] = it.size.height.toFloat() }
                            .zIndex(if (isDragged) 8f else 0f)
                            .graphicsLayer {
                                if (isDragged) {
                                    translationY = dragDelta
                                    scaleX = 1.03f
                                    scaleY = 1.03f
                                    shadowElevation = 18f
                                }
                            }
                            .pointerInput(itemId) {
                                val spacing = 10.dp.toPx()
                                detectDragGesturesAfterLongPress(
                                    onDragStart = {
                                        draggedId = itemId
                                        dragDelta = 0f
                                        sounds(Sounds.Effect.POP)
                                    },
                                    onDrag = { change, amount ->
                                        change.consume()
                                        dragDelta += amount.y
                                        var idx = order.indexOf(itemId)
                                        // Crossing the midpoint of a neighbour swaps places.
                                        while (true) {
                                            val nextId = order.getOrNull(idx + 1) ?: break
                                            val step = (rowHeights[nextId] ?: 0f) + spacing
                                            if (step > 0f && dragDelta > step / 2) {
                                                order.removeAt(idx)
                                                order.add(idx + 1, itemId)
                                                idx += 1
                                                dragDelta -= step
                                            } else break
                                        }
                                        while (true) {
                                            val prevId = order.getOrNull(idx - 1) ?: break
                                            val step = (rowHeights[prevId] ?: 0f) + spacing
                                            if (step > 0f && dragDelta < -step / 2) {
                                                order.removeAt(idx)
                                                order.add(idx - 1, itemId)
                                                idx -= 1
                                                dragDelta += step
                                            } else break
                                        }
                                    },
                                    onDragEnd = {
                                        draggedId = null
                                        dragDelta = 0f
                                        sounds(Sounds.Effect.POP)
                                    },
                                    onDragCancel = {
                                        draggedId = null
                                        dragDelta = 0f
                                    },
                                )
                            },
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 12.dp),
                        ) {
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier
                                    .size(26.dp)
                                    .background(Workbook.Blue, CircleShape),
                            ) {
                                Text(
                                    "${position + 1}",
                                    color = androidx.compose.ui.graphics.Color.White,
                                    style = MaterialTheme.typography.titleSmall,
                                )
                            }
                            item.emoji?.let {
                                Text(it, fontSize = 19.sp, modifier = Modifier.padding(start = 10.dp))
                            }
                            Text(
                                item.text,
                                style = MaterialTheme.typography.bodyLarge,
                                modifier = Modifier.weight(1f).padding(start = 10.dp),
                            )
                            Text("☰", color = Workbook.TextMuted, fontSize = 18.sp)
                        }
                    }
                }
            }
        }
    }
}
