package com.kidslearning.app.ui.renderers

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextStyle

/**
 * Text with an optional read-aloud control. When [readAloud] is on (default for
 * younger age profiles), a speaker button sits next to the text and pipes it to
 * the on-device TTS engine -- free and offline, per the v2 plan.
 */
@Composable
fun SpeakableText(
    text: String,
    style: TextStyle = MaterialTheme.typography.bodyLarge,
    readAloud: Boolean,
    speak: (String) -> Unit,
) {
    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(text, style = style, modifier = Modifier.weight(1f))
        if (readAloud) {
            TextButton(onClick = { speak(text) }) { Text("🔊") }
        }
    }
}
