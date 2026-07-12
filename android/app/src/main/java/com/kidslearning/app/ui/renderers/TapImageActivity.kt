package com.kidslearning.app.ui.renderers

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kidslearning.app.domain.model.AnswerResult
import com.kidslearning.app.domain.model.TapImageSection

/**
 * Tap the correct picture. Each option shows a real fetched photo when available
 * (via its image_ref), otherwise a big emoji — so it works with or without a
 * network fetch. Two-per-row card grid with a clear selected outline.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun TapImageActivity(
    section: TapImageSection,
    number: Int?,
    readAloud: Boolean,
    speak: (String) -> Unit,
    onAnswered: (AnswerResult) -> Unit,
) {
    var selected by remember(section.id) { mutableStateOf<String?>(null) }
    val resolver = LocalLessonImageResolver.current

    ActivityScaffold(
        activity = section,
        number = number,
        prompt = section.question,
        readAloud = readAloud,
        speak = speak,
        onAnswered = onAnswered,
        checkEnabled = selected != null,
        score = { selected == section.correctOptionId },
    ) {
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            section.options.forEach { option ->
                val isSelected = selected == option.id
                val bitmap = option.imageRef?.let { resolver(it) }
                Card(
                    onClick = { selected = option.id },
                    colors = CardDefaults.cardColors(
                        containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer
                        else MaterialTheme.colorScheme.surfaceVariant,
                    ),
                    border = if (isSelected)
                        BorderStroke(3.dp, MaterialTheme.colorScheme.primary) else null,
                    elevation = CardDefaults.cardElevation(defaultElevation = if (isSelected) 6.dp else 2.dp),
                    modifier = Modifier.size(width = 150.dp, height = 150.dp),
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center,
                        modifier = Modifier.fillMaxWidth().padding(8.dp),
                    ) {
                        if (bitmap != null) {
                            Image(
                                bitmap = bitmap,
                                contentDescription = option.label ?: "picture",
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxWidth().height(100.dp),
                            )
                        } else {
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier.fillMaxWidth().height(100.dp),
                            ) {
                                Text(option.emoji ?: "❓", fontSize = 56.sp)
                            }
                        }
                        option.label?.let {
                            Text(
                                it,
                                style = MaterialTheme.typography.labelMedium,
                                textAlign = TextAlign.Center,
                                maxLines = 1,
                            )
                        }
                    }
                }
            }
        }
    }
}
