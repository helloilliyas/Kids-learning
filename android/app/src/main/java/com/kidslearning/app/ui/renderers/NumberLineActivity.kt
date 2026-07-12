package com.kidslearning.app.ui.renderers

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kidslearning.app.domain.model.AnswerResult
import com.kidslearning.app.domain.model.NumberLineSection
import com.kidslearning.app.ui.theme.Workbook
import kotlin.math.roundToInt

/**
 * Slide a marker along a number line to the answer. The current value shows big
 * above the slider; the ends and midpoint are labelled so the child can orient.
 */
@Composable
fun NumberLineActivity(
    section: NumberLineSection,
    number: Int?,
    readAloud: Boolean,
    speak: (String) -> Unit,
    onAnswered: (AnswerResult) -> Unit,
) {
    val step = section.step.coerceAtLeast(1)
    val steps = ((section.maxValue - section.minValue) / step - 1).coerceAtLeast(0)
    var value by remember(section.id) { mutableFloatStateOf(section.minValue.toFloat()) }
    val snapped = (section.minValue +
        ((value - section.minValue) / step).roundToInt() * step)
        .coerceIn(section.minValue, section.maxValue)

    ActivityScaffold(
        activity = section,
        number = number,
        prompt = section.question,
        readAloud = readAloud,
        speak = speak,
        onAnswered = onAnswered,
        checkEnabled = true,
        score = { snapped == section.correctValue },
    ) {
        Column {
            Text(
                "$snapped${section.unit?.let { " $it" } ?: ""}",
                fontSize = 30.sp,
                fontWeight = FontWeight.ExtraBold,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.fillMaxWidth().padding(bottom = 4.dp),
            )
            Slider(
                value = value,
                onValueChange = { value = it },
                valueRange = section.minValue.toFloat()..section.maxValue.toFloat(),
                steps = steps,
                modifier = Modifier.fillMaxWidth(),
            )
            Row(
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("${section.minValue}", style = MaterialTheme.typography.labelMedium, color = Workbook.TextMuted)
                Text(
                    "${(section.minValue + section.maxValue) / 2}",
                    style = MaterialTheme.typography.labelMedium, color = Workbook.TextMuted,
                )
                Text("${section.maxValue}", style = MaterialTheme.typography.labelMedium, color = Workbook.TextMuted)
            }
        }
    }
}
