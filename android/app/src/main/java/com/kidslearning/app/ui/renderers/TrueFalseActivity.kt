package com.kidslearning.app.ui.renderers

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.kidslearning.app.domain.model.AnswerResult
import com.kidslearning.app.domain.model.LocalScoring
import com.kidslearning.app.domain.model.TrueFalseSection

@Composable
fun TrueFalseActivity(
    section: TrueFalseSection,
    readAloud: Boolean,
    speak: (String) -> Unit,
    onAnswered: (AnswerResult) -> Unit,
) {
    var choice by remember(section.id) { mutableStateOf<Boolean?>(null) }

    ActivityScaffold(
        activity = section,
        prompt = section.statement,
        readAloud = readAloud,
        speak = speak,
        onAnswered = onAnswered,
        checkEnabled = choice != null,
        score = { choice != null && LocalScoring.scoreTrueFalse(section, choice!!) },
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            listOf(true to "✔ True", false to "✘ False").forEach { (value, label) ->
                val isSelected = choice == value
                Card(
                    onClick = { choice = value },
                    colors = CardDefaults.cardColors(
                        containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer
                        else MaterialTheme.colorScheme.surfaceVariant,
                    ),
                    border = if (isSelected) BorderStroke(2.dp, MaterialTheme.colorScheme.primary) else null,
                    modifier = Modifier.weight(1f).sizeIn(minHeight = 72.dp),
                ) {
                    Text(
                        label,
                        modifier = Modifier.padding(20.dp).align(Alignment.CenterHorizontally),
                        style = MaterialTheme.typography.titleMedium,
                    )
                }
            }
        }
    }
}
