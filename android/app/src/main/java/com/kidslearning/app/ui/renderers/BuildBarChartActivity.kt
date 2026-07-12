package com.kidslearning.app.ui.renderers

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kidslearning.app.domain.model.AnswerResult
import com.kidslearning.app.domain.model.BuildBarChartSection
import com.kidslearning.app.ui.theme.OptionPalette
import com.kidslearning.app.ui.theme.Workbook

/**
 * Interactive graphing, the flagship activity from the reference workbook app:
 * the child drags each bar up and down (or taps the +/− chips) until the chart
 * matches the story, then checks. Deterministically scored on-device.
 */
@Composable
fun BuildBarChartActivity(
    section: BuildBarChartSection,
    number: Int?,
    readAloud: Boolean,
    speak: (String) -> Unit,
    onAnswered: (AnswerResult) -> Unit,
) {
    val values = remember(section.id) {
        mutableStateListOf<Int>().apply { repeat(section.items.size) { add(0) } }
    }

    ActivityScaffold(
        activity = section,
        number = number,
        prompt = section.instruction,
        readAloud = readAloud,
        speak = speak,
        onAnswered = onAnswered,
        checkEnabled = true,
        score = { values.toList() == section.items.map { it.target } },
        onIncorrectAttempt = {},
    ) {
        val chartHeight = 170.dp
        val density = LocalDensity.current
        val pxPerUnit = with(density) { chartHeight.toPx() } / section.maxValue

        Column {
            Text(
                "👆 Drag each bar up or down — or tap + and −",
                style = MaterialTheme.typography.labelLarge,
                modifier = Modifier.padding(bottom = 6.dp),
            )
            Row(
                verticalAlignment = Alignment.Bottom,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxWidth().height(chartHeight + 30.dp),
            ) {
                section.items.forEachIndexed { index, item ->
                    val (_, accent) = OptionPalette[index % OptionPalette.size]
                    val barHeight by animateDpAsState(
                        targetValue = chartHeight * (values[index].toFloat() / section.maxValue),
                        label = "barHeight",
                    )
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier
                            .weight(1f)
                            .height(chartHeight + 30.dp)
                            .pointerInput(section.id, index) {
                                // Accumulate raw pixels so slow drags still add up,
                                // then snap to whole units.
                                var rawPx = values[index] * pxPerUnit
                                detectVerticalDragGestures(
                                    onDragStart = { rawPx = values[index] * pxPerUnit },
                                ) { change, dragAmount ->
                                    change.consume()
                                    rawPx = (rawPx - dragAmount)
                                        .coerceIn(0f, pxPerUnit * section.maxValue)
                                    values[index] = (rawPx / pxPerUnit + 0.5f).toInt()
                                        .coerceIn(0, section.maxValue)
                                }
                            },
                    ) {
                        Spacer(Modifier.weight(1f))
                        Text(values[index].toString(), style = MaterialTheme.typography.titleSmall)
                        Box(
                            modifier = Modifier
                                .fillMaxWidth(0.65f)
                                .height(barHeight)
                                .background(
                                    accent,
                                    RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp),
                                ),
                        )
                    }
                }
            }
            HorizontalDivider(thickness = 2.dp, color = Workbook.TextDark)
            Row(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxWidth(),
            ) {
                section.items.forEachIndexed { index, item ->
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.weight(1f),
                    ) {
                        Text(
                            "${item.emoji ?: ""} ${item.label}".trim(),
                            style = MaterialTheme.typography.labelMedium,
                            textAlign = TextAlign.Center,
                            maxLines = 2,
                        )
                        Row {
                            TextButton(
                                onClick = {
                                    values[index] = (values[index] - section.step)
                                        .coerceAtLeast(0)
                                },
                            ) { Text("−", fontSize = 20.sp) }
                            TextButton(
                                onClick = {
                                    values[index] = (values[index] + section.step)
                                        .coerceAtMost(section.maxValue)
                                },
                            ) { Text("+", fontSize = 20.sp) }
                        }
                    }
                }
            }
        }
    }
}
