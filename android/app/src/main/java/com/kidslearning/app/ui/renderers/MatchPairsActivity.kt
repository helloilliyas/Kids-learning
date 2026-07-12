package com.kidslearning.app.ui.renderers

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.kidslearning.app.domain.model.AnswerResult
import com.kidslearning.app.domain.model.LocalScoring
import com.kidslearning.app.domain.model.MatchPairsSection
import kotlin.random.Random

/**
 * Tap-to-match pairs: tap an item on the left, then its partner on the right.
 * Matched pairs are numbered so the child can see (and undo) their choices by
 * tapping the left item again. The right column is shuffled deterministically per
 * section id so the correct answers never line up row-by-row.
 */
@Composable
fun MatchPairsActivity(
    section: MatchPairsSection,
    readAloud: Boolean,
    speak: (String) -> Unit,
    onAnswered: (AnswerResult) -> Unit,
) {
    val matches = remember(section.id) { mutableStateMapOf<String, String>() } // leftId -> rightId
    var pendingLeft by remember(section.id) { mutableStateOf<String?>(null) }
    val shuffledRight = remember(section.id) {
        section.pairs.map { it.right }.shuffled(Random(section.id.hashCode()))
    }

    ActivityScaffold(
        activity = section,
        prompt = section.instruction,
        readAloud = readAloud,
        speak = speak,
        onAnswered = onAnswered,
        checkEnabled = matches.size == section.pairs.size,
        score = { LocalScoring.scoreMatchPairs(section, matches.toMap()) },
        onIncorrectAttempt = {
            // Keep only the correct matches so the child retries just the wrong ones.
            val correctByLeft = section.pairs.associate { it.left.id to it.right.id }
            matches.entries.retainAll { it.value == correctByLeft[it.key] }
        },
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                section.pairs.forEachIndexed { index, pair ->
                    val leftId = pair.left.id
                    val matchedNumber = matches[leftId]
                        ?.let { rid -> shuffledRight.indexOfFirst { it.id == rid } + 1 }
                    val isPending = pendingLeft == leftId
                    Card(
                        onClick = {
                            if (matches.containsKey(leftId)) matches.remove(leftId)
                            pendingLeft = leftId
                        },
                        colors = CardDefaults.cardColors(
                            containerColor = when {
                                isPending -> MaterialTheme.colorScheme.primaryContainer
                                matchedNumber != null -> MaterialTheme.colorScheme.tertiaryContainer
                                else -> MaterialTheme.colorScheme.surfaceVariant
                            },
                        ),
                        border = if (isPending) BorderStroke(2.dp, MaterialTheme.colorScheme.primary) else null,
                        modifier = Modifier.fillMaxWidth().sizeIn(minHeight = 56.dp),
                    ) {
                        Text(
                            if (matchedNumber != null) "${pair.left.text}  →❨${matchedNumber}❩"
                            else pair.left.text,
                            modifier = Modifier.padding(14.dp),
                            style = MaterialTheme.typography.bodyLarge,
                        )
                    }
                }
            }
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                shuffledRight.forEachIndexed { index, right ->
                    val taken = matches.containsValue(right.id)
                    Card(
                        onClick = {
                            val left = pendingLeft
                            if (left != null && !taken) {
                                matches[left] = right.id
                                pendingLeft = null
                            }
                        },
                        colors = CardDefaults.cardColors(
                            containerColor = if (taken) MaterialTheme.colorScheme.tertiaryContainer
                            else MaterialTheme.colorScheme.surfaceVariant,
                        ),
                        modifier = Modifier.fillMaxWidth().sizeIn(minHeight = 56.dp),
                    ) {
                        Text(
                            "❨${index + 1}❩ ${right.text}",
                            modifier = Modifier.padding(14.dp),
                            style = MaterialTheme.typography.bodyLarge,
                        )
                    }
                }
            }
        }
    }
}
