package com.kidslearning.app.ui.renderers

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kidslearning.app.domain.model.ChartSection
import com.kidslearning.app.ui.theme.OptionPalette
import com.kidslearning.app.ui.theme.Workbook

/**
 * Natively-rendered data visuals -- the AI generates the data, the app draws the
 * graphic, crisp like an HTML chart. Two kinds: colourful bar charts and emoji
 * pictographs with a key ("each 🧁 = 2 muffins"), both in the workbook style.
 */
@Composable
fun ChartView(section: ChartSection, readAloud: Boolean, speak: (String) -> Unit) {
    Card(
        colors = CardDefaults.cardColors(containerColor = Workbook.CardWhite),
        border = BorderStroke(1.5.dp, Workbook.BlueLight),
        shape = MaterialTheme.shapes.medium,
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                section.emoji?.let { Text(it, fontSize = 20.sp, modifier = Modifier.padding(end = 8.dp)) }
                SpeakableText(
                    text = section.title,
                    style = MaterialTheme.typography.titleMedium,
                    readAloud = readAloud,
                    speak = speak,
                )
            }

            if (section.chartType == "pictograph") Pictograph(section) else BarChart(section)
        }
    }
}

@Composable
private fun Pictograph(section: ChartSection) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        section.items.forEachIndexed { index, item ->
            val (container, _) = OptionPalette[index % OptionPalette.size]
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .background(container, MaterialTheme.shapes.small)
                    .padding(horizontal = 10.dp, vertical = 8.dp),
            ) {
                Text(
                    item.label,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(end = 10.dp).weight(0.35f),
                )
                Text(
                    (item.emoji ?: "⬤").repeat(
                        (item.value / section.symbolValue.coerceAtLeast(1)).coerceIn(0, 20)
                    ),
                    fontSize = 20.sp,
                    modifier = Modifier.weight(0.65f),
                )
            }
        }
        HorizontalDivider(color = Workbook.PageBackground)
        Text(
            "🔑 Key: each ${section.items.firstOrNull()?.emoji ?: "symbol"} = " +
                "${section.symbolValue} ${section.unit ?: ""}".trim(),
            style = MaterialTheme.typography.labelLarge,
            color = Workbook.TextMuted,
        )
    }
}

@Composable
private fun BarChart(section: ChartSection) {
    val maxValue = section.items.maxOf { it.value }.coerceAtLeast(1)
    val chartHeight = 150.dp

    Row(
        verticalAlignment = Alignment.Bottom,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        modifier = Modifier.fillMaxWidth().height(chartHeight + 60.dp),
    ) {
        section.items.forEachIndexed { index, item ->
            val (_, accent) = OptionPalette[index % OptionPalette.size]
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.weight(1f),
            ) {
                Spacer(Modifier.weight(1f))
                Text(item.value.toString(), style = MaterialTheme.typography.titleSmall)
                Box(
                    modifier = Modifier
                        .fillMaxWidth(0.7f)
                        .height(chartHeight * (item.value.toFloat() / maxValue))
                        .background(
                            accent,
                            RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp),
                        ),
                )
                HorizontalDivider(thickness = 2.dp, color = Workbook.TextDark)
                Text(
                    "${item.emoji ?: ""} ${item.label}".trim(),
                    style = MaterialTheme.typography.labelMedium,
                    textAlign = TextAlign.Center,
                    maxLines = 2,
                )
            }
        }
    }
    section.unit?.let {
        Text(
            "Counting: $it",
            style = MaterialTheme.typography.labelMedium,
            color = Workbook.TextMuted,
        )
    }
}
