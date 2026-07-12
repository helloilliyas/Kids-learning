package com.kidslearning.app.ui.parent

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kidslearning.app.data.local.Prefs
import com.kidslearning.app.data.remote.BackendClient
import com.kidslearning.app.domain.model.Lesson
import com.kidslearning.app.ui.theme.Workbook
import kotlinx.coroutines.launch

/**
 * Parent mode: create a lesson from a topic (plus optional pasted source text) by
 * calling the generation backend, and configure the backend connection.
 *
 * The generated lesson is NOT shown to the child from here -- it goes to the
 * preview screen for parent review and approval first.
 */
@Composable
fun ParentScreen(
    prefs: Prefs,
    onPreview: (Lesson) -> Unit,
    onExit: () -> Unit,
) {
    val scope = rememberCoroutineScope()

    var topic by remember { mutableStateOf("") }
    var subject by remember { mutableStateOf("") }
    var age by remember { mutableStateOf("8") }
    var sourceText by remember { mutableStateOf("") }
    var objective by remember { mutableStateOf("") }

    var backendUrl by remember { mutableStateOf(prefs.backendUrl) }
    var appToken by remember { mutableStateOf(prefs.appToken) }

    var busy by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }

    Column(modifier = Modifier.fillMaxSize().background(Workbook.PageBackground)) {
        Surface(
            color = Workbook.Blue,
            shape = RoundedCornerShape(bottomStart = 18.dp, bottomEnd = 18.dp),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
            ) {
                Surface(
                    color = Workbook.Coral,
                    shape = MaterialTheme.shapes.small,
                    modifier = Modifier.size(34.dp),
                ) {
                    Box(contentAlignment = Alignment.Center) { Text("👨‍👩‍👧", fontSize = 16.sp) }
                }
                Text(
                    "Parent mode",
                    style = MaterialTheme.typography.titleMedium,
                    color = Color.White,
                    modifier = Modifier.weight(1f).padding(horizontal = 12.dp),
                )
            }
        }

        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Card(
                colors = CardDefaults.cardColors(containerColor = Workbook.CardWhite),
                shape = MaterialTheme.shapes.large,
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    Text("Create a lesson", style = MaterialTheme.typography.titleLarge)
                    OutlinedTextField(
                        value = topic, onValueChange = { topic = it },
                        label = { Text("Topic (e.g. The Solar System)") },
                        singleLine = true, modifier = Modifier.fillMaxWidth(),
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        OutlinedTextField(
                            value = subject, onValueChange = { subject = it },
                            label = { Text("Subject") },
                            singleLine = true, modifier = Modifier.weight(1f),
                        )
                        OutlinedTextField(
                            value = age,
                            onValueChange = { age = it.filter { c -> c.isDigit() }.take(2) },
                            label = { Text("Age") },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.weight(0.5f),
                        )
                    }
                    OutlinedTextField(
                        value = objective, onValueChange = { objective = it },
                        label = { Text("Learning objective (optional)") },
                        singleLine = true, modifier = Modifier.fillMaxWidth(),
                    )
                    OutlinedTextField(
                        value = sourceText, onValueChange = { sourceText = it },
                        label = { Text("Paste source material (optional)") },
                        minLines = 4, maxLines = 10, modifier = Modifier.fillMaxWidth(),
                    )

                    error?.let {
                        Text(it, color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodyMedium)
                    }

                    Button(
                        enabled = !busy && topic.isNotBlank() && subject.isNotBlank()
                            && (age.toIntOrNull() ?: 0) in 3..18,
                        onClick = {
                            error = null
                            if (backendUrl.isBlank()) {
                                error = "Set the backend URL below first (see documentation/backend-setup.md)."
                                return@Button
                            }
                            busy = true
                            scope.launch {
                                try {
                                    val client = BackendClient(backendUrl, appToken)
                                    val response = client.generateLesson(
                                        BackendClient.GenerateRequest(
                                            sourceText = sourceText,
                                            topic = topic.trim(),
                                            age = age.toInt(),
                                            subject = subject.trim(),
                                            objective = objective.trim(),
                                        )
                                    )
                                    val lesson = response.lesson
                                    when {
                                        response.ok && lesson != null -> onPreview(lesson)
                                        lesson != null ->
                                            error = "The lesson failed validation: " +
                                                (response.structuralErrors + response.semanticErrors)
                                                    .take(3).joinToString("; ")
                                        else -> error = "The backend returned no lesson."
                                    }
                                } catch (e: Exception) {
                                    error = "Could not generate: ${e.message?.take(200)}"
                                } finally {
                                    busy = false
                                }
                            }
                        },
                        modifier = Modifier.fillMaxWidth().sizeIn(minHeight = 48.dp),
                    ) {
                        if (busy) {
                            CircularProgressIndicator(
                                color = Color.White,
                                strokeWidth = 2.5.dp,
                                modifier = Modifier.size(22.dp),
                            )
                            Text("  Generating…")
                        } else {
                            Text("✨ Generate lesson")
                        }
                    }
                }
            }

            Card(
                colors = CardDefaults.cardColors(containerColor = Workbook.CardWhite),
                shape = MaterialTheme.shapes.large,
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    Text("Backend connection", style = MaterialTheme.typography.titleMedium)
                    Text(
                        "Where lessons are generated. Your AI key stays on the backend; " +
                            "it is never stored in this app.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Workbook.TextMuted,
                    )
                    OutlinedTextField(
                        value = backendUrl, onValueChange = { backendUrl = it },
                        label = { Text("Backend URL (https://…)") },
                        singleLine = true, modifier = Modifier.fillMaxWidth(),
                    )
                    OutlinedTextField(
                        value = appToken, onValueChange = { appToken = it },
                        label = { Text("App token (optional)") },
                        singleLine = true, modifier = Modifier.fillMaxWidth(),
                    )
                    Button(
                        onClick = {
                            prefs.backendUrl = backendUrl
                            prefs.appToken = appToken
                            error = null
                            scope.launch {
                                val ok = BackendClient(backendUrl, appToken).health()
                                error = if (ok) "✓ Connected to the backend."
                                else "Could not reach the backend at that URL."
                            }
                        },
                        enabled = backendUrl.isNotBlank(),
                        modifier = Modifier.sizeIn(minHeight = 44.dp),
                    ) { Text("Save & test connection") }
                }
            }
        }

        Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 10.dp)) {
            Button(
                onClick = onExit,
                colors = ButtonDefaults.buttonColors(
                    containerColor = Workbook.Coral, contentColor = Color.White),
                modifier = Modifier.sizeIn(minHeight = 48.dp),
            ) { Text("← Back to lessons") }
        }
    }
}
