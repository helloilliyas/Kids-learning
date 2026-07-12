package com.kidslearning.app.ui.renderers

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
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
import androidx.compose.ui.unit.dp
import com.kidslearning.app.domain.model.AnswerResult
import com.kidslearning.app.domain.model.LocalScoring
import com.kidslearning.app.domain.model.MatchPairsSection
import com.kidslearning.app.ui.LocalSounds
import com.kidslearning.app.ui.Sounds
import kotlin.random.Random

/**
 * Match pairs by holding a left card and dragging it onto its partner on the
 * right — or the tap fallback: tap left, then tap right. Matched pairs show the
 * partner's number badge; tapping a matched left item undoes it. The right column
 * is shuffled deterministically per section id so the answers never line up
 * row-by-row.
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
    val dragState = rememberDragDropState(section.id)
    val sounds = LocalSounds.current

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
        Column {
            Text(
                "🖐 Hold a card and drag it onto its match (or tap-tap).",
                style = MaterialTheme.typography.labelLarge,
                modifier = Modifier.padding(bottom = 8.dp),
            )
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                section.pairs.forEachIndexed { index, pair ->
                    val leftId = pair.left.id
                    val matchedNumber = matches[leftId]
                        ?.let { rid -> shuffledRight.indexOfFirst { it.id == rid } + 1 }
                    Box(
                        modifier = Modifier.dragSource(
                            state = dragState,
                            id = leftId,
                            longPress = true,
                            onPickUp = {
                                matches.remove(leftId)
                                sounds(Sounds.Effect.POP)
                            },
                            onDrop = { target ->
                                if (target != null && !matches.containsValue(target)) {
                                    matches[leftId] = target
                                    pendingLeft = null
                                    sounds(Sounds.Effect.POP)
                                }
                            },
                        ),
                    ) {
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
            }
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                shuffledRight.forEachIndexed { index, right ->
                    val taken = matches.containsValue(right.id)
                    Box(modifier = Modifier.dropTarget(dragState, right.id)) {
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
                                    sounds(Sounds.Effect.POP)
                                }
                            },
                        )
                    }
                }
            }
        }
        }
    }
}
