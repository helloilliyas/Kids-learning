package com.kidslearning.app.ui.renderers

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
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
 * Matched pairs show the partner's number badge; tapping a matched left item
 * undoes it. The right column is shuffled deterministically per section id so the
 * answers never line up row-by-row.
 */
@Composable
fun MatchPairsActivity(
    section: MatchPairsSection,
    number: Int?,
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
        number = number,
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
                    OptionCard(
                        text = pair.left.text,
                        emoji = pair.left.emoji,
                        index = index,
                        selected = pendingLeft == leftId,
                        badge = matchedNumber?.toString(),
                        onClick = {
                            if (matches.containsKey(leftId)) matches.remove(leftId)
                            pendingLeft = leftId
                        },
                    )
                }
            }
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                shuffledRight.forEachIndexed { index, right ->
                    val taken = matches.containsValue(right.id)
                    OptionCard(
                        text = right.text,
                        emoji = right.emoji,
                        index = index + section.pairs.size, // different palette slots than the left column
                        selected = taken,
                        badge = (index + 1).toString(),
                        onClick = {
                            val left = pendingLeft
                            if (left != null && !taken) {
                                matches[left] = right.id
                                pendingLeft = null
                            }
                        },
                    )
                }
            }
        }
    }
}
