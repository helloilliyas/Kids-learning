package com.kidslearning.app.ui.parent

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.kidslearning.app.ui.theme.Workbook
import kotlin.random.Random

/**
 * Adult verification gate for parent mode: a quick multiplication question, the
 * standard kids-app pattern -- no setup or stored PIN needed for V1. A parent PIN
 * (AppSettingsEntity.parentPinHash) can replace this later without changing the
 * navigation.
 */
@Composable
fun ParentGate(onUnlock: () -> Unit, onCancel: () -> Unit) {
    val a = remember { Random.nextInt(6, 10) }
    val b = remember { Random.nextInt(6, 10) }
    var answer by remember { mutableStateOf("") }
    var wrong by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier.fillMaxSize().background(Workbook.PageBackground).padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Card(
            colors = CardDefaults.cardColors(containerColor = Workbook.CardWhite),
            shape = MaterialTheme.shapes.large,
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text("👨‍👩‍👧 Grown-ups only", style = MaterialTheme.typography.titleLarge)
                Text(
                    "To enter parent mode, solve:",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Workbook.TextMuted,
                )
                Text("$a × $b = ?", style = MaterialTheme.typography.headlineMedium)
                OutlinedTextField(
                    value = answer,
                    onValueChange = { answer = it.filter { c -> c.isDigit() } },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    label = { Text("Answer") },
                )
                if (wrong) {
                    Text(
                        "Not quite — try again.",
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Button(
                        onClick = onCancel,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Workbook.Coral, contentColor = Color.White),
                        modifier = Modifier.sizeIn(minHeight = 44.dp),
                    ) { Text("Cancel") }
                    Button(
                        enabled = answer.isNotBlank(),
                        onClick = {
                            if (answer.toIntOrNull() == a * b) onUnlock() else wrong = true
                        },
                        modifier = Modifier.sizeIn(minHeight = 44.dp),
                    ) { Text("Unlock") }
                }
            }
        }
        TextButton(onClick = onCancel, modifier = Modifier.padding(top = 8.dp)) {
            Text("Back to lessons")
        }
    }
}
