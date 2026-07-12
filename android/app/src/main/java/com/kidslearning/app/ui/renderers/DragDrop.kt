package com.kidslearning.app.ui.renderers

import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.zIndex

/**
 * Minimal drag-and-drop for the activity renderers: drop targets register their
 * bounds; a drag source follows the finger (floating above siblings) and reports
 * which target the pointer was over on release. All coordinates are in root space
 * so it works inside scrolling worksheet pages.
 *
 * Every draggable interaction in the app keeps a tap fallback — drag is the fun
 * path, not the only path.
 */
class DragDropState {
    val targets = mutableStateMapOf<String, Rect>()
    var draggingId by mutableStateOf<String?>(null)
}

@Composable
fun rememberDragDropState(key: Any?): DragDropState = remember(key) { DragDropState() }

fun Modifier.dropTarget(state: DragDropState, id: String): Modifier =
    onGloballyPositioned { state.targets[id] = it.boundsInRoot() }

/**
 * Make this element draggable. [longPress] starts the drag after a hold — use it
 * for large cards that would otherwise steal page scrolling. [onDrop] receives
 * the id of the target under the pointer, or null when released elsewhere.
 */
fun Modifier.dragSource(
    state: DragDropState,
    id: String,
    longPress: Boolean = false,
    onPickUp: () -> Unit = {},
    onDrop: (String?) -> Unit,
): Modifier = composed {
    var origin by remember(id) { mutableStateOf(Offset.Zero) }
    var pointer by remember(id) { mutableStateOf(Offset.Zero) }
    var delta by remember(id) { mutableStateOf(Offset.Zero) }
    val dragging = state.draggingId == id

    this
        .onGloballyPositioned { origin = it.positionInRoot() }
        .zIndex(if (dragging) 8f else 0f)
        .graphicsLayer {
            if (dragging) {
                translationX = delta.x
                translationY = delta.y
                scaleX = 1.06f
                scaleY = 1.06f
                shadowElevation = 18f
            }
        }
        .pointerInput(id, longPress) {
            val onStart: (Offset) -> Unit = { start ->
                state.draggingId = id
                delta = Offset.Zero
                pointer = origin + start
                onPickUp()
            }
            val onMove: (Offset) -> Unit = { amount ->
                delta += amount
                pointer += amount
            }
            val onEnd: () -> Unit = {
                val hit = state.targets.entries
                    .firstOrNull { it.value.contains(pointer) }?.key
                state.draggingId = null
                delta = Offset.Zero
                onDrop(hit)
            }
            val onCancel: () -> Unit = {
                state.draggingId = null
                delta = Offset.Zero
            }
            if (longPress) {
                detectDragGesturesAfterLongPress(
                    onDragStart = onStart,
                    onDrag = { change, amount -> change.consume(); onMove(amount) },
                    onDragEnd = onEnd,
                    onDragCancel = onCancel,
                )
            } else {
                detectDragGestures(
                    onDragStart = onStart,
                    onDrag = { change, amount -> change.consume(); onMove(amount) },
                    onDragEnd = onEnd,
                    onDragCancel = onCancel,
                )
            }
        }
}
