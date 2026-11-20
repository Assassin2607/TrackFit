package com.example.ui.workout

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.domain.calc.FitnessCalculations
import com.example.domain.model.CardioActivityType
import com.example.domain.model.UnitSystem
import com.example.ui.theme.FitTrackThemeExt
import com.example.ui.theme.MonoMetricMedium

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun CardioLogDialog(
    weightKg: Double,
    unitSystem: UnitSystem,
    onDismiss: () -> Unit,
    onSave: (
        activity: CardioActivityType,
        durationMin: Int,
        distanceKm: Double,
        speedKmh: Double,
        inclinePercent: Double,
        resistanceLevel: Int,
        notes: String
    ) -> Unit
) {
    var activity by remember { mutableStateOf(CardioActivityType.WALKING) }
    var durationText by remember { mutableStateOf("20") }
    var distanceText by remember { mutableStateOf("") }
    var speedText by remember { mutableStateOf("") }
    var inclineText by remember { mutableStateOf("") }
    var levelText by remember { mutableStateOf("") }
    var notesText by remember { mutableStateOf("") }

    val duration = durationText.toIntOrNull()?.coerceIn(1, 600) ?: 0
    val distanceDisplay = distanceText.toDoubleOrNull() ?: 0.0
    val distanceKm = FitnessCalculations.displayToKm(distanceDisplay, unitSystem)
    val speedDisplay = speedText.toDoubleOrNull() ?: 0.0
    val speedKmh = if (unitSystem == UnitSystem.IMPERIAL) speedDisplay / FitnessCalculations.KM_TO_MI else speedDisplay
    val derivedSpeedKmh = if (speedKmh > 0) speedKmh else if (distanceKm > 0 && duration > 0) distanceKm / (duration / 60.0) else 0.0
    val incline = inclineText.toDoubleOrNull()?.coerceIn(0.0, 45.0) ?: 0.0
    val resistance = levelText.toIntOrNull()?.coerceIn(0, 50) ?: 0

    val estimatedCalories = remember(activity, duration, weightKg, derivedSpeedKmh, incline, resistance) {
        FitnessCalculations.estimateCardioCalories(
            activityType = activity,
            durationMinutes = duration,
            weightKg = weightKg,
            speedKmh = derivedSpeedKmh,
            inclinePercent = incline,
            resistanceLevel = resistance
        )
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Log Cardio Session") },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text("Activity", style = MaterialTheme.typography.labelLarge)
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    CardioActivityType.entries.forEach { act ->
                        FilterChip(
                            selected = activity == act,
                            onClick = { activity = act },
                            label = { Text(act.label, style = MaterialTheme.typography.labelSmall) }
                        )
                    }
                }

                OutlinedTextField(
                    value = durationText,
                    onValueChange = { durationText = it.filter(Char::isDigit).take(3) },
                    label = { Text("Duration (minutes)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("cardio_duration_input")
                )

                if (activity.hasDistance || activity.hasSpeed) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        if (activity.hasDistance) {
                            OutlinedTextField(
                                value = distanceText,
                                onValueChange = { distanceText = it.filter { c -> c.isDigit() || c == '.' }.take(6) },
                                label = { Text("Distance (${unitSystem.distanceUnit})") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                singleLine = true,
                                modifier = Modifier.weight(1f)
                            )
                        }
                        if (activity.hasSpeed) {
                            OutlinedTextField(
                                value = speedText,
                                onValueChange = { speedText = it.filter { c -> c.isDigit() || c == '.' }.take(5) },
                                label = { Text("Speed (${unitSystem.distanceUnit}/h)") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                singleLine = true,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }

                if (activity.hasIncline || activity.hasResistance) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        if (activity.hasIncline) {
                            OutlinedTextField(
                                value = inclineText,
                                onValueChange = { inclineText = it.filter { c -> c.isDigit() || c == '.' }.take(4) },
                                label = { Text("Incline (%)") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                singleLine = true,
                                modifier = Modifier.weight(1f)
                            )
                        }
                        if (activity.hasResistance) {
                            OutlinedTextField(
                                value = levelText,
                                onValueChange = { levelText = it.filter(Char::isDigit).take(2) },
                                label = { Text("Level / Resistance") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                singleLine = true,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }

                OutlinedTextField(
                    value = notesText,
                    onValueChange = { notesText = it.take(160) },
                    label = { Text("Notes (optional)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Surface(
                    shape = MaterialTheme.shapes.medium,
                    color = MaterialTheme.colorScheme.surfaceVariant
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(
                            text = "Estimated Expenditure (MET Formula)",
                            style = MaterialTheme.typography.labelSmall,
                            color = FitTrackThemeExt.colors.mutedText
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "~$estimatedCalories kcal (estimate)",
                            style = MonoMetricMedium,
                            color = FitTrackThemeExt.colors.cardioAccent
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (duration > 0) {
                        onSave(activity, duration, distanceKm, derivedSpeedKmh, incline, resistance, notesText)
                        onDismiss()
                    }
                },
                enabled = duration > 0,
                modifier = Modifier.testTag("save_cardio_button")
            ) { Text("Log Cardio") }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}
