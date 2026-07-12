package com.kidslearning.app.ui.parent

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kidslearning.app.data.local.Prefs
import com.kidslearning.app.data.local.SourceImages
import com.kidslearning.app.data.remote.BackendClient
import com.kidslearning.app.data.remote.DirectGenerator
import com.kidslearning.app.domain.model.Lesson
import com.kidslearning.app.ui.theme.Workbook
import kotlinx.coroutines.launch

/**
 * Parent mode: create a lesson from a topic, optional pasted text, and optional
 * source images -- camera snaps, photos from the phone, or PDF pages (rendered to
 * images on-device). Images travel to the backend where the AI reads them as the
 * source material; sections that teach from a specific image reference it so the
 * child sees the real picture in the lesson.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ParentScreen(
    prefs: Prefs,
    onPreview: (Lesson, List<ByteArray>) -> Unit,
    onExit: () -> Unit,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var topic by remember { mutableStateOf("") }
    var subject by remember { mutableStateOf("") }
    var age by remember { mutableStateOf("8") }
    var sourceText by remember { mutableStateOf("") }
    var objective by remember { mutableStateOf("") }
    val images = remember { mutableStateListOf<ByteArray>() }

    var backendUrl by remember { mutableStateOf(prefs.backendUrl) }
    var appToken by remember { mutableStateOf(prefs.appToken) }
    var apiKey by remember { mutableStateOf(prefs.anthropicKey) }

    var busy by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }

    val takePhoto = rememberLauncherForActivityResult(
        ActivityResultContracts.TakePicturePreview()
    ) { bitmap ->
        bitmap?.let { images.add(SourceImages.compress(it)) }
    }
    val pickImages = rememberLauncherForActivityResult(
        ActivityResultContracts.PickMultipleVisualMedia(SourceImages.MAX_IMAGES)
    ) { uris ->
        uris.take(SourceImages.MAX_IMAGES - images.size).forEach { uri ->
            SourceImages.fromUri(context, uri)?.let { images.add(it) }
        }
    }
    val pickPdf = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        uri?.let { images.addAll(SourceImages.fromPdf(context, it).take(SourceImages.MAX_IMAGES - images.size)) }
    }

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
                        minLines = 3, maxLines = 8, modifier = Modifier.fillMaxWidth(),
                    )

                    Text(
                        "Add material from the camera, photos, or a PDF — the AI reads " +
                            "it (including diagrams and handwriting) and teaches from it.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Workbook.TextMuted,
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        val room = images.size < SourceImages.MAX_IMAGES
                        OutlinedButton(onClick = { takePhoto.launch(null) }, enabled = room) {
                            Text("📷 Camera")
                        }
                        OutlinedButton(
                            onClick = {
                                pickImages.launch(
                                    PickVisualMediaRequest(
                                        ActivityResultContracts.PickVisualMedia.ImageOnly
                                    )
                                )
                            },
                            enabled = room,
                        ) { Text("🖼 Photos") }
                        OutlinedButton(
                            onClick = { pickPdf.launch(arrayOf("application/pdf")) },
                            enabled = room,
                        ) { Text("📄 PDF") }
                    }

                    if (images.isNotEmpty()) {
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            images.forEachIndexed { index, bytes ->
                                val bitmap = remember(bytes) { SourceImages.decode(bytes) }
                                if (bitmap != null) {
                                    Card(
                                        onClick = { images.removeAt(index) },
                                        shape = MaterialTheme.shapes.small,
                                    ) {
                                        Box {
                                            Image(
                                                bitmap = bitmap.asImageBitmap(),
                                                contentDescription = "Attached page ${index + 1}",
                                                contentScale = ContentScale.Crop,
                                                modifier = Modifier.size(72.dp),
                                            )
                                            Text(
                                                "✕",
                                                color = Color.White,
                                                modifier = Modifier
                                                    .align(Alignment.TopEnd)
                                                    .background(Color(0x99000000))
                                                    .padding(horizontal = 6.dp, vertical = 1.dp),
                                            )
                                        }
                                    }
                                }
                            }
                        }
                        Text(
                            "${images.size} page(s) attached · tap a picture to remove it",
                            style = MaterialTheme.typography.labelMedium,
                            color = Workbook.TextMuted,
                        )
                    }

                    error?.let {
                        Text(it, color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodyMedium)
                    }

                    Button(
                        enabled = !busy && topic.isNotBlank() && subject.isNotBlank()
                            && (age.toIntOrNull() ?: 0) in 3..18,
                        onClick = {
                            error = null
                            if (apiKey.isBlank() && backendUrl.isBlank()) {
                                error = "Add your API key (or a backend URL) in the settings below first."
                                return@Button
                            }
                            busy = true
                            scope.launch {
                                try {
                                    if (apiKey.isNotBlank()) {
                                        // Personal-use mode: generate on this phone.
                                        val result = DirectGenerator(apiKey, context).generate(
                                            topic = topic.trim(),
                                            subject = subject.trim(),
                                            age = age.toInt(),
                                            objective = objective.trim(),
                                            sourceText = sourceText,
                                            uploadedImages = images.toList(),
                                        )
                                        val lesson = result.lesson
                                        when {
                                            lesson != null && result.errors.isEmpty() ->
                                                onPreview(lesson, result.images)
                                            lesson != null ->
                                                error = "The lesson failed checks: " +
                                                    result.errors.take(3).joinToString("; ")
                                            else -> error = result.errors.firstOrNull()
                                                ?: "Generation returned nothing."
                                        }
                                    } else {
                                        val client = BackendClient(backendUrl, appToken)
                                        val response = client.generateLesson(
                                            BackendClient.GenerateRequest(
                                                sourceText = sourceText,
                                                topic = topic.trim(),
                                                age = age.toInt(),
                                                subject = subject.trim(),
                                                objective = objective.trim(),
                                                sourceImages = images.map(SourceImages::toBase64),
                                            )
                                        )
                                        val lesson = response.lesson
                                        when {
                                            response.ok && lesson != null -> onPreview(lesson, images.toList())
                                            lesson != null ->
                                                error = "The lesson failed validation: " +
                                                    (response.structuralErrors + response.semanticErrors)
                                                        .take(3).joinToString("; ")
                                            else -> error = "The backend returned no lesson."
                                        }
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
                    Text("AI connection", style = MaterialTheme.typography.titleMedium)
                    Text(
                        "Personal-use mode: paste your Anthropic API key and lessons are " +
                            "generated straight from this phone. The key is stored only in " +
                            "this app's private storage on this device. Tip: set a monthly " +
                            "spend limit at console.anthropic.com.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Workbook.TextMuted,
                    )
                    OutlinedTextField(
                        value = apiKey, onValueChange = { apiKey = it },
                        label = { Text("Anthropic API key (sk-ant-…)") },
                        singleLine = true,
                        visualTransformation = PasswordVisualTransformation(),
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Button(
                        onClick = {
                            prefs.anthropicKey = apiKey
                            error = if (apiKey.isBlank()) "API key cleared."
                            else "✓ Key saved on this device."
                        },
                        modifier = Modifier.sizeIn(minHeight = 44.dp),
                    ) { Text("Save key") }

                    Text(
                        "Alternative: a private backend (leave the key empty to use it).",
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
