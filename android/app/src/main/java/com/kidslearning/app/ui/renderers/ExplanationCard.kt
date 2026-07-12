package com.kidslearning.app.ui.renderers

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kidslearning.app.domain.model.ExplanationSection
import com.kidslearning.app.ui.theme.Workbook

/**
 * Teaching content as a cream "definition panel", exactly like the instruction
 * blocks in the reference workbook design: soft cream background, subtle border,
 * bold lead-in, and the illustration description as a pink picture note.
 */
@Composable
fun ExplanationCard(
    section: ExplanationSection,
    readAloud: Boolean,
    speak: (String) -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Card(
            colors = CardDefaults.cardColors(containerColor = Workbook.Cream),
            border = BorderStroke(1.5.dp, Workbook.CreamBorder),
            shape = MaterialTheme.shapes.medium,
        ) {
            Column(
                modifier = Modifier.fillMaxWidth().padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    section.emoji?.let {
                        Text(it, fontSize = 26.sp, modifier = Modifier.padding(end = 10.dp))
                    }
                    SpeakableText(
                        text = section.title,
                        style = MaterialTheme.typography.titleLarge,
                        readAloud = readAloud,
                        speak = speak,
                    )
                }
                SpeakableText(
                    text = section.content,
                    style = MaterialTheme.typography.bodyLarge,
                    readAloud = readAloud,
                    speak = speak,
                )
            }
        }

        section.imageDescription?.let { description ->
            Card(
                colors = CardDefaults.cardColors(containerColor = Workbook.Pink),
                border = BorderStroke(1.5.dp, Workbook.PinkBorder),
                shape = MaterialTheme.shapes.small,
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text("🖼", fontSize = 18.sp, modifier = Modifier.padding(end = 10.dp))
                    Text(description, style = MaterialTheme.typography.bodyMedium)
                }
            }
        }
    }
}
