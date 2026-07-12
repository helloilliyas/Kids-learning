package com.kidslearning.app.ui.renderers

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kidslearning.app.domain.model.ExplanationSection
import com.kidslearning.app.ui.theme.OptionPalette

/**
 * Teaching card: a big emoji "hero" visual on a soft gradient, the title, the
 * explanation text, and the illustration description as a styled picture card.
 */
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
        section.emoji?.let { emoji ->
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        Brush.verticalGradient(
                            listOf(
                                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f),
                                MaterialTheme.colorScheme.background,
                            )
                        ),
                        shape = MaterialTheme.shapes.extraLarge,
                    )
                    .padding(vertical = 24.dp),
            ) {
                Text(emoji, fontSize = 72.sp, textAlign = TextAlign.Center)
            }
        }

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
        section.imageDescription?.let { description ->
            val (container, accent) = OptionPalette[section.id.hashCode().mod(OptionPalette.size)]
            Card(
                colors = CardDefaults.cardColors(containerColor = container),
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("🖼 Picture this…", style = MaterialTheme.typography.titleMedium)
                    Text(
                        description,
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(top = 6.dp),
                    )
                }
            }
        }
    }
}
