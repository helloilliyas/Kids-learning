package com.kidslearning.app.ui.renderers

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.kidslearning.app.domain.model.AnswerResult
import com.kidslearning.app.domain.model.DragIntoOrderSection
import com.kidslearning.app.domain.model.LocalScoring
import kotlin.random.Random

/**
 * Sequencing via tap-in-order: items are shown shuffled; the child taps them in
 * the order they believe is correct and each tap gets a position number. Tapping a
 * numbered item removes it (and renumbers). Simpler and more reliable for small
 * fingers than long-press drag; a true drag interaction is a later polish item.
 */
@Composable
fun DragIntoOrderActivity(
    section: DragIntoOrderSection,
    readAloud: Boolean,
    speak: (String) -> Unit,
    onAnswered: (AnswerResult) -> Unit,
) {
    val shuffled = remember(section.id) {
        section.items.shuffled(Random(section.id.hashCode()))
    }
    val tappedOrder = remember(section.id) { mutableStateListOf<String>() }

    ActivityScaffold(
        activity = section,
        prompt = section.instruction,
        readAloud = readAloud,
        speak = speak,
        onAnswered = onAnswered,
        checkEnabled = tappedOrder.size == section.items.size,
        score = { LocalScoring.scoreDragIntoOrder(section, tappedOrder.toList()) },
        onIncorrectAttempt = { tappedOrder.clear() },
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text("Tap the steps in order.", style = MaterialTheme.typography.labelLarge)
            shuffled.forEach { item ->
                val position = tappedOrder.indexOf(item.id)
                Card(
                    onClick = {
                        if (position >= 0) tappedOrder.remove(item.id) else tappedOrder.add(item.id)
                    },
                    colors = CardDefaults.cardColors(
                        containerColor = if (position >= 0) MaterialTheme.colorScheme.primaryContainer
                        else MaterialTheme.colorScheme.surfaceVariant,
                    ),
                    modifier = Modifier.fillMaxWidth().sizeIn(minHeight = 56.dp),
                ) {
                    Text(
                        if (position >= 0) "${position + 1}. ${item.text}" else item.text,
                        modifier = Modifier.padding(14.dp),
                        style = MaterialTheme.typography.bodyLarge,
                    )
                }
            }
        }
    }
}
