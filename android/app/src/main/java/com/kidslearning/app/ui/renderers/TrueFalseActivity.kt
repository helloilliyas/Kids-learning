package com.kidslearning.app.ui.renderers

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.kidslearning.app.domain.model.AnswerResult
import com.kidslearning.app.domain.model.LocalScoring
import com.kidslearning.app.domain.model.TrueFalseSection

@Composable
fun TrueFalseActivity(
    section: TrueFalseSection,
    number: Int?,
    readAloud: Boolean,
    speak: (String) -> Unit,
    onAnswered: (AnswerResult) -> Unit,
) {
    var choice by remember(section.id) { mutableStateOf<Boolean?>(null) }

    ActivityScaffold(
        activity = section,
        number = number,
        prompt = section.statement,
        readAloud = readAloud,
        speak = speak,
        onAnswered = onAnswered,
        checkEnabled = choice != null,
        score = { choice != null && LocalScoring.scoreTrueFalse(section, choice!!) },
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            OptionCard(
                text = "True",
                emoji = "👍",
                index = 3, // green slot in the palette
                selected = choice == true,
                onClick = { choice = true },
                minHeight = 76,
                modifier = Modifier.weight(1f),
            )
            OptionCard(
                text = "False",
                emoji = "👎",
                index = 4, // red-pink slot in the palette
                selected = choice == false,
                onClick = { choice = false },
                minHeight = 76,
                modifier = Modifier.weight(1f),
            )
        }
    }
}
