package com.example.ui.workout

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.EmojiEvents
import androidx.compose.material.icons.rounded.Timer
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.domain.calc.FitnessCalculations
import com.example.domain.model.UnitSystem
import com.example.ui.components.FitTrackCard
import com.example.ui.theme.FitTrackThemeExt
import com.example.ui.theme.MonoMetricMedium
import com.example.ui.theme.MonoMetricSmall
import com.example.ui.theme.WorkoutCreamText
import com.example.viewmodel.ActiveSetDraft
import com.example.viewmodel.ActiveWorkoutState
import com.example.viewmodel.RestTimerState

@Composable
fun ActiveWorkoutScreen(
    session: ActiveWorkoutState,
    restTimer: RestTimerState,
    unitSystem: UnitSystem,
    onUpdateSet: (exerciseIdx: Int, setIdx: Int, weightKg: Double?, reps: Int?, rir: Int?, warmup: Boolean?, completed: Boolean?) -> Unit,
    onAddSet: (exerciseIdx: Int) -> Unit,
    onRemoveSet: (exerciseIdx: Int, setIdx: Int) -> Unit,
    onQuickProgression: (exerciseIdx: Int, deltaWeightKg: Double, deltaReps: Int) -> Unit,
    onUpdateNotes: (String) -> Unit,
    onStartRestTimer: (Int) -> Unit,
    onAdjustRestTimer: (Int) -> Unit,
    onStopRestTimer: () -> Unit,
    onFinishWorkout: () -> Unit,
    onCancelWorkout: () -> Unit
) {
    var showDiscardDialog by remember { mutableStateOf(false) }

    BackHandler { showDiscardDialog = true }

    val completedSetsCount = session.exercises.sumOf { ex -> ex.sets.count { it.completed } }
    val liveVolumeKg = session.exercises.sumOf { ex ->
        ex.sets.filter { it.completed && !it.warmup }
            .sumOf { FitnessCalculations.calculateSetVolume(it.weightKg, it.reps) }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .statusBarsPadding()
            .navigationBarsPadding()
            .testTag("active_workout_screen")
    ) {
        // Top Bar: Close + Workout Title (Cream) + Live Stats (no Finish button at top)
        Surface(
            color = MaterialTheme.colorScheme.surface,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 10.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = { showDiscardDialog = true }) {
                    Icon(Icons.Rounded.Close, contentDescription = "Cancel workout")
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = session.templateName,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = WorkoutCreamText
                    )
                    Text(
                        text = "$completedSetsCount sets • Vol ${FitnessCalculations.formatVolume(liveVolumeKg, unitSystem)}",
                        style = MonoMetricSmall,
                        color = FitTrackThemeExt.colors.success
                    )
                }
            }
        }

        // Rest Timer Dock pinned exclusively at the top
        RestTimerDock(
            timer = restTimer,
            onStartPreset = onStartRestTimer,
            onAdjust = onAdjustRestTimer,
            onStop = onStopRestTimer
        )

        LazyColumn(
            modifier = Modifier.weight(1f),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            itemsIndexed(session.exercises, key = { idx, ex -> "${ex.exerciseId}_$idx" }) { exIndex, exercise ->
                FitTrackCard {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(exercise.exerciseName, style = MaterialTheme.typography.titleMedium)
                            Text(
                                text = "Rest ${exercise.restSeconds}s • ${exercise.muscleGroup}",
                                style = MaterialTheme.typography.labelSmall,
                                color = FitTrackThemeExt.colors.mutedText
                            )
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            val stepKg = if (unitSystem == UnitSystem.IMPERIAL) 2.26796 else 2.5
                            val stepLabel = if (unitSystem == UnitSystem.IMPERIAL) "+5 lb" else "+2.5 kg"
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant,
                                modifier = Modifier.clickable { onQuickProgression(exIndex, stepKg, 0) }
                            ) {
                                Text(
                                    text = stepLabel,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp)
                                )
                            }
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant,
                                modifier = Modifier.clickable { onQuickProgression(exIndex, 0.0, 1) }
                            ) {
                                Text(
                                    text = "+1 rep",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = FitTrackThemeExt.colors.success,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp)
                                )
                            }
                        }
                    }

                    // Previous performance callout
                    if (exercise.previousSetsSummary.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(6.dp))
                        Surface(
                            shape = MaterialTheme.shapes.small,
                            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f)
                        ) {
                            Text(
                                text = "Previous: ${exercise.previousSetsSummary.joinToString("  •  ")}",
                                style = MonoMetricSmall,
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 10.dp, vertical = 6.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Column headers
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Set", style = MaterialTheme.typography.labelSmall, color = FitTrackThemeExt.colors.mutedText, modifier = Modifier.width(34.dp))
                        Text(unitSystem.weightUnit.uppercase(), style = MaterialTheme.typography.labelSmall, color = FitTrackThemeExt.colors.mutedText, modifier = Modifier.weight(1f))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("REPS", style = MaterialTheme.typography.labelSmall, color = FitTrackThemeExt.colors.mutedText, modifier = Modifier.width(66.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("RIR", style = MaterialTheme.typography.labelSmall, color = FitTrackThemeExt.colors.mutedText, modifier = Modifier.width(48.dp))
                        Text("Done", style = MaterialTheme.typography.labelSmall, color = FitTrackThemeExt.colors.mutedText, modifier = Modifier.width(48.dp))
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    exercise.sets.forEachIndexed { setIndex, setDraft ->
                        ActiveSetRow(
                            setDraft = setDraft,
                            unitSystem = unitSystem,
                            onWeightChange = { kg -> onUpdateSet(exIndex, setIndex, kg, null, null, null, null) },
                            onRepsChange = { reps -> onUpdateSet(exIndex, setIndex, null, reps, null, null, null) },
                            onRirChange = { rir -> onUpdateSet(exIndex, setIndex, null, null, rir, null, null) },
                            onToggleWarmup = { onUpdateSet(exIndex, setIndex, null, null, null, !setDraft.warmup, null) },
                            onToggleComplete = { onUpdateSet(exIndex, setIndex, null, null, null, null, !setDraft.completed) },
                            onRemove = if (exercise.sets.size > 1) ({ onRemoveSet(exIndex, setIndex) }) else null
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                    }

                    TextButton(
                        onClick = { onAddSet(exIndex) },
                        modifier = Modifier.align(Alignment.CenterHorizontally)
                    ) {
                        Icon(Icons.Rounded.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Add Set")
                    }
                }
            }

            item {
                OutlinedTextField(
                    value = session.notes,
                    onValueChange = onUpdateNotes,
                    label = { Text("Workout Notes (energy, cues, soreness)") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
            item { Spacer(modifier = Modifier.height(8.dp)) }
        }

        // Bottom Docked Finish Action Bar
        Surface(
            color = MaterialTheme.colorScheme.surface,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Button(
                    onClick = onFinishWorkout,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = FitTrackThemeExt.colors.success,
                        contentColor = FitTrackThemeExt.colors.onSuccess
                    ),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                        .testTag("finish_workout_button")
                ) {
                    Icon(Icons.Rounded.Check, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (completedSetsCount > 0) "Finish Workout ($completedSetsCount sets)" else "Finish Workout",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }

    if (showDiscardDialog) {
        AlertDialog(
            onDismissRequest = { showDiscardDialog = false },
            title = { Text("Discard Active Workout?") },
            text = { Text("Your current unsubmitted sets will be discarded.") },
            confirmButton = {
                Button(
                    onClick = {
                        showDiscardDialog = false
                        onCancelWorkout()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) { Text("Discard") }
            },
            dismissButton = {
                OutlinedButton(onClick = { showDiscardDialog = false }) { Text("Keep Training") }
            }
        )
    }
}

@Composable
private fun ActiveSetRow(
    setDraft: ActiveSetDraft,
    unitSystem: UnitSystem,
    onWeightChange: (Double) -> Unit,
    onRepsChange: (Int) -> Unit,
    onRirChange: (Int) -> Unit,
    onToggleWarmup: () -> Unit,
    onToggleComplete: () -> Unit,
    onRemove: (() -> Unit)?
) {
    val displayWeight = FitnessCalculations.kgToDisplay(setDraft.weightKg, unitSystem)
    var weightText by remember(setDraft.id, setDraft.weightKg, unitSystem) {
        mutableStateOf(if (displayWeight > 0) String.format("%.1f", displayWeight) else "")
    }
    var repsText by remember(setDraft.id, setDraft.reps) {
        mutableStateOf(if (setDraft.reps > 0) setDraft.reps.toString() else "")
    }
    var rirText by remember(setDraft.id, setDraft.rir) {
        mutableStateOf(setDraft.rir.toString())
    }

    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Set number / Warmup toggle chip
            Surface(
                modifier = Modifier
                    .size(34.dp)
                    .clickable(onClick = onToggleWarmup),
                shape = RoundedCornerShape(8.dp),
                color = if (setDraft.warmup) FitTrackThemeExt.colors.warningContainer else MaterialTheme.colorScheme.surfaceVariant
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        text = if (setDraft.warmup) "W" else "${setDraft.setNumber}",
                        style = MonoMetricSmall,
                        color = if (setDraft.warmup) FitTrackThemeExt.colors.warning else MaterialTheme.colorScheme.onSurface
                    )
                }
            }
            Spacer(modifier = Modifier.width(6.dp))

            OutlinedTextField(
                value = weightText,
                onValueChange = { raw ->
                    val cleaned = raw.filter { it.isDigit() || it == '.' }.take(6)
                    weightText = cleaned
                    if (cleaned.isBlank()) {
                        onWeightChange(0.0)
                    } else {
                        cleaned.toDoubleOrNull()?.let { v ->
                            onWeightChange(FitnessCalculations.displayToKg(v, unitSystem))
                        }
                    }
                },
                placeholder = {
                    Text(
                        text = "BW",
                        style = MonoMetricSmall,
                        color = FitTrackThemeExt.colors.mutedText.copy(alpha = 0.7f)
                    )
                },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                singleLine = true,
                textStyle = MonoMetricSmall,
                modifier = Modifier.weight(1f)
            )
            Spacer(modifier = Modifier.width(6.dp))

            OutlinedTextField(
                value = repsText,
                onValueChange = { raw ->
                    val cleaned = raw.filter(Char::isDigit).take(3)
                    repsText = cleaned
                    cleaned.toIntOrNull()?.let(onRepsChange)
                },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true,
                textStyle = MonoMetricSmall,
                modifier = Modifier.width(66.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))

            OutlinedTextField(
                value = rirText,
                onValueChange = { raw ->
                    val cleaned = raw.filter(Char::isDigit).take(1)
                    rirText = cleaned
                    cleaned.toIntOrNull()?.let(onRirChange)
                },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true,
                textStyle = MonoMetricSmall,
                modifier = Modifier.width(48.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))

            // 48dp accessible completion checkbox button
            Surface(
                modifier = Modifier
                    .size(48.dp)
                    .clickable(onClick = onToggleComplete)
                    .testTag("complete_set_${setDraft.setNumber}"),
                shape = RoundedCornerShape(10.dp),
                color = if (setDraft.completed) FitTrackThemeExt.colors.success else MaterialTheme.colorScheme.surfaceVariant,
                border = BorderStroke(
                    1.dp,
                    if (setDraft.completed) FitTrackThemeExt.colors.success else MaterialTheme.colorScheme.outline
                )
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Rounded.Check,
                        contentDescription = "Mark set complete",
                        tint = if (setDraft.completed) FitTrackThemeExt.colors.onSuccess else FitTrackThemeExt.colors.mutedText
                    )
                }
            }
        }

        if (setDraft.isPr || onRemove != null) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (setDraft.isPr) {
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = FitTrackThemeExt.colors.successContainer
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Rounded.EmojiEvents,
                                contentDescription = null,
                                tint = FitTrackThemeExt.colors.success,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "NEW PR • ${setDraft.prLabel}",
                                style = MaterialTheme.typography.labelSmall,
                                color = FitTrackThemeExt.colors.success,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                } else {
                    Spacer(modifier = Modifier.width(1.dp))
                }
                if (onRemove != null && !setDraft.completed) {
                    IconButton(onClick = onRemove, modifier = Modifier.size(28.dp)) {
                        Icon(
                            Icons.Rounded.Delete,
                            contentDescription = "Remove set",
                            tint = FitTrackThemeExt.colors.mutedText,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun RestTimerDock(
    timer: RestTimerState,
    onStartPreset: (Int) -> Unit,
    onAdjust: (Int) -> Unit,
    onStop: () -> Unit
) {
    val presets = listOf(30, 60, 90, 120, 180)
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surfaceVariant,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
    ) {
        Column(modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Rounded.Timer,
                        contentDescription = null,
                        tint = if (timer.isRunning) FitTrackThemeExt.colors.cardioAccent else FitTrackThemeExt.colors.mutedText
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    val mins = timer.remainingSeconds / 60
                    val secs = timer.remainingSeconds % 60
                    Text(
                        text = if (timer.isRunning) String.format("Rest %02d:%02d", mins, secs) else "Rest Timer Ready",
                        style = MonoMetricMedium,
                        color = if (timer.isRunning) FitTrackThemeExt.colors.cardioAccent else MaterialTheme.colorScheme.onSurface
                    )
                }
                if (timer.isRunning) {
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        FilledTonalButton(
                            onClick = { onAdjust(-15) },
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                        ) { Text("-15s", style = MaterialTheme.typography.labelSmall) }
                        FilledTonalButton(
                            onClick = { onAdjust(15) },
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                        ) { Text("+15s", style = MaterialTheme.typography.labelSmall) }
                        OutlinedButton(
                            onClick = onStop,
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                        ) { Text("Stop", style = MaterialTheme.typography.labelSmall) }
                    }
                }
            }

            if (timer.isRunning && timer.totalSeconds > 0) {
                Spacer(modifier = Modifier.height(6.dp))
                LinearProgressIndicator(
                    progress = { (timer.remainingSeconds.toFloat() / timer.totalSeconds.toFloat()).coerceIn(0f, 1f) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(5.dp),
                    color = FitTrackThemeExt.colors.cardioAccent,
                    trackColor = MaterialTheme.colorScheme.outline
                )
            } else {
                Spacer(modifier = Modifier.height(6.dp))
                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    items(presets) { sec ->
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.surface,
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                            modifier = Modifier.clickable { onStartPreset(sec) }
                        ) {
                            Text(
                                text = "${sec}s",
                                style = MonoMetricSmall,
                                color = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}
