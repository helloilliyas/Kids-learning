package com.kidslearning.app.ui.renderers

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.kidslearning.app.domain.model.ExplanationSection

@Composable
fun ExplanationCard(
    section: ExplanationSection,
    readAloud: Boolean,
    speak: (String) -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        SpeakableText(
            text = section.title,
            style = MaterialTheme.typography.headlineSmall,
            readAloud = readAloud,
            speak = speak,
        )
        SpeakableText(
            text = section.content,
            style = MaterialTheme.typography.bodyLarge,
            readAloud = readAloud,
            speak = speak,
        )
        section.imageDescription?.let {
            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
                Text(
                    "🖼 $it",
                    modifier = Modifier.padding(14.dp),
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
        }
    }
}
